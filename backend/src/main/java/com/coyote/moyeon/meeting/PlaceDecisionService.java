package com.coyote.moyeon.meeting;

import com.coyote.moyeon.common.exception.BusinessException;
import com.coyote.moyeon.meeting.dto.ChoosePlaceRequest;
import com.coyote.moyeon.meeting.dto.MeetingResultResponse;
import com.coyote.moyeon.place.GeoMath;
import com.coyote.moyeon.place.Place;
import com.coyote.moyeon.place.PlaceRankingService;
import com.coyote.moyeon.place.PlaceSearchProvider;
import com.coyote.moyeon.place.RankRequest;
import com.coyote.moyeon.place.RankedPlace;
import com.coyote.moyeon.place.consensus.Cluster;
import com.coyote.moyeon.place.consensus.ConsensusCandidateService;
import com.coyote.moyeon.place.consensus.ConsensusOutcome;
import com.coyote.moyeon.place.consensus.PlaceVote;
import com.coyote.moyeon.place.consensus.Proposal;
import com.coyote.moyeon.place.consensus.SpatialConsensus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 마감 이후의 장소 결정.
 * <ul>
 * <li>{@link #completePending}: 건너뛰기 모임에서 제안이 흩어졌을 때 합의 지역의 후보를 만들고 AI로 3곳을 골라
 * 호스트의 선택을 기다린다. 외부 API·AI를 부르므로 DB 잠금 밖에서 하고, 결과만 짧은 트랜잭션으로 저장한다.</li>
 * <li>{@link #chooseByHost}: 호스트가 후보 중 하나를 확정한다.</li>
 * <li>{@link #autoPick}: 호스트가 시한 안에 고르지 않으면 첫 번째 후보로 자동 확정한다.</li>
 * </ul>
 */
@Service
public class PlaceDecisionService {

	private static final Logger log = LoggerFactory.getLogger(PlaceDecisionService.class);
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int PICK = 3;
	private static final int REASON_MAX = 200;

	private final MeetingRepository meetingRepository;
	private final FinalResultRepository finalResultRepository;
	private final ParticipantRepository participantRepository;
	private final ResultInputFactory inputFactory;
	private final ConsensusCandidateService candidateService;
	private final PlaceRankingService rankingService;
	private final PlaceSearchProvider provider;
	private final PlatformTransactionManager transactionManager;
	private final Duration hostWindow;
	private final boolean async;
	private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

	public PlaceDecisionService(MeetingRepository meetingRepository, FinalResultRepository finalResultRepository,
			ParticipantRepository participantRepository, ResultInputFactory inputFactory,
			ConsensusCandidateService candidateService, PlaceRankingService rankingService,
			PlaceSearchProvider provider, PlatformTransactionManager transactionManager,
			@Value("${app.place-decision.host-window-hours:24}") long hostWindowHours,
			@Value("${app.place-decision.async:true}") boolean async) {
		this.meetingRepository = meetingRepository;
		this.finalResultRepository = finalResultRepository;
		this.participantRepository = participantRepository;
		this.inputFactory = inputFactory;
		this.candidateService = candidateService;
		this.rankingService = rankingService;
		this.provider = provider;
		this.transactionManager = transactionManager;
		this.hostWindow = Duration.ofHours(hostWindowHours);
		this.async = async;
	}

	/** 마감 처리 커밋 직후 호출한다. 실패해도 마감 요청을 깨뜨리지 않고, 스케줄러가 다시 시도한다. */
	public void completePendingAsync(Long meetingId) {
		Runnable task = () -> {
			try {
				completePending(meetingId);
			} catch (RuntimeException e) {
				log.error("Failed to prepare place candidates for meeting {}", meetingId, e);
			}
		};
		if (async) {
			executor.execute(task);
		} else {
			task.run();
		}
	}

	/** PENDING 상태의 장소 후보를 만든다. 이미 처리됐거나 대상이 아니면 아무것도 하지 않는다(멱등). */
	public void completePending(Long meetingId) {
		Snapshot snapshot = inNewTransaction(true).execute(status -> readSnapshot(meetingId));
		if (snapshot == null) {
			return;
		}
		List<Proposal> proposals = VoteCountResultGenerator.proposalsOf(snapshot.input());
		if (!(SpatialConsensus.decide(proposals) instanceof ConsensusOutcome.Spatial spatial)) {
			// 마감 뒤에는 응답이 바뀌지 않으므로 여기에 오지 않는다. 오더라도 장소는 추후 협의로 끝낸다.
			finish(meetingId, null, 0, "장소 후보를 만들지 못해 추후 협의로 남겼습니다.");
			return;
		}

		ConsensusOutcome.Spatial enriched = withOfficialPlaces(spatial);
		List<Place> pool = poolOf(enriched);
		List<FinalResultCandidate> candidates = pool.isEmpty() ? List.of()
				: rankingService.rank(new RankRequest(snapshot.title(), snapshot.description(),
						snapshot.participantCount(), pool, PICK)).places().stream()
						.map(ranked -> toCandidate(ranked, enriched.cluster()))
						.toList();
		String reason = candidates.isEmpty() ? "장소 후보를 찾지 못해 추후 협의로 남겼습니다."
				: enriched.regionLabel() + "에 제안이 모여 후보 " + candidates.size() + "곳을 골랐습니다. 호스트가 최종 장소를 고릅니다.";
		finish(meetingId, candidates, enriched.cluster().totalVotes(), reason);
	}

	/** 후보를 저장하고 모임을 마친다. 잠금을 걸고 다시 확인하므로 두 번 처리되지 않는다. */
	private void finish(Long meetingId, List<FinalResultCandidate> candidates, int votes, String reason) {
		inNewTransaction(false).executeWithoutResult(status -> {
			Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
			FinalResult result = finalResultRepository.findByMeetingId(meetingId).orElse(null);
			if (meeting == null || result == null || meeting.getStatus() != MeetingStatus.CLOSED
					|| result.getPlaceStatus() != PlaceStatus.PENDING) {
				return;
			}
			if (candidates == null || candidates.isEmpty()) {
				result.leaveUndecided(reason);
			} else {
				result.awaitHost(candidates, Instant.now().plus(hostWindow), votes, reason);
			}
			meeting.finishProcessing();
		});
	}

	private Snapshot readSnapshot(Long meetingId) {
		Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
		if (meeting == null || meeting.getStatus() != MeetingStatus.CLOSED) {
			return null;
		}
		FinalResult result = finalResultRepository.findByMeetingId(meetingId).orElse(null);
		if (result == null || result.getPlaceStatus() != PlaceStatus.PENDING) {
			return null;
		}
		return new Snapshot(meeting.getTitle(), meeting.getDescription(),
				(int) participantRepository.countByMeetingId(meetingId), inputFactory.build(meeting, LocalDate.now(KST)));
	}

	private record Snapshot(String title, String description, int participantCount, ResultInput input) {
	}

	/** 제안된 장소를 공식 데이터로 다시 조회해 업종 코드를 채운다(검색 업종을 고르는 데 쓴다). 실패하면 저장된 값을 쓴다. */
	private ConsensusOutcome.Spatial withOfficialPlaces(ConsensusOutcome.Spatial spatial) {
		if (!provider.isAvailable()) {
			return spatial;
		}
		List<PlaceVote> members = new ArrayList<>();
		for (PlaceVote member : spatial.cluster().members()) {
			Place place = member.place();
			try {
				place = provider.findById(place.id()).orElse(place);
			} catch (RuntimeException e) {
				log.warn("Could not refresh place {} for candidates: {}", place.id(), e.getClass().getSimpleName());
			}
			members.add(new PlaceVote(place, member.votes(), member.order()));
		}
		return new ConsensusOutcome.Spatial(spatial.regionLabel(), spatial.centerLatitude(), spatial.centerLongitude(),
				Cluster.of(members), spatial.clusterCount(), spatial.totalVotes());
	}

	/** 후보 풀(최대 10곳, 가까운 순). 검색이 실패하면 제안된 장소만으로 만든다. */
	private List<Place> poolOf(ConsensusOutcome.Spatial spatial) {
		List<Place> pool;
		try {
			pool = candidateService.candidates(spatial);
		} catch (RuntimeException e) {
			log.warn("Candidate search failed, using proposed places only: {}", e.getClass().getSimpleName());
			pool = spatial.cluster().members().stream()
					.map(member -> member.place().withDistance(GeoMath.distanceMeters(spatial.centerLatitude(),
							spatial.centerLongitude(), member.place().latitude(), member.place().longitude())))
					.toList();
		}
		return pool.stream().sorted(Comparator.comparingDouble(Place::distanceMeters)).toList();
	}

	private static FinalResultCandidate toCandidate(RankedPlace ranked, Cluster cluster) {
		Map<String, Integer> votesById = new HashMap<>();
		cluster.members().forEach(member -> votesById.put(member.place().id(), member.votes()));
		Place place = ranked.place();
		String reason = ranked.reason() == null ? null : cut(ranked.reason(), REASON_MAX);
		return new FinalResultCandidate(cut(place.name(), 100), place.id(), cut(place.address(), 255),
				cut(place.categoryName(), 100), place.latitude(), place.longitude(),
				votesById.getOrDefault(place.id(), 0), reason);
	}

	private static String cut(String value, int max) {
		if (value == null || value.length() <= max) {
			return value;
		}
		return value.substring(0, max);
	}

	/** 호스트가 후보 중 하나로 최종 장소를 확정한다. */
	@Transactional
	public MeetingResultResponse chooseByHost(Long userId, Long meetingId, ChoosePlaceRequest request) {
		Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "모임을 찾을 수 없습니다."));
		if (!meeting.getHost().getId().equals(userId)) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "이 모임에 접근할 권한이 없습니다.");
		}
		boolean hasId = request.placeId() != null && !request.placeId().isBlank();
		boolean hasName = request.name() != null && !request.name().isBlank();
		if (!hasId && !hasName) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "고를 장소를 알려 주세요.");
		}
		FinalResult result = finalResultRepository.findByMeetingId(meetingId)
				.orElseThrow(() -> new BusinessException(HttpStatus.CONFLICT, "MEETING_NOT_CLOSED", "아직 응답이 마감되지 않았습니다."));
		if (result.getPlaceStatus() != PlaceStatus.AWAITING_HOST) {
			throw new BusinessException(HttpStatus.CONFLICT, "PLACE_NOT_AWAITING", "지금은 장소를 고를 수 없는 상태입니다.");
		}
		FinalResultCandidate chosen = result.getCandidates().stream()
				.filter(candidate -> candidate.matches(hasId ? request.placeId() : null, request.name()))
				.findFirst()
				.orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CANDIDATE", "후보에 없는 장소입니다."));
		result.decidePlace(chosen, DecisionType.HOST_CHOICE, "호스트가 후보 중에서 최종 장소를 선택했습니다.");
		return MeetingResultResponse.forHost(result);
	}

	/**
	 * 호스트가 시한 안에 고르지 않은 모임의 장소를 첫 번째 후보로 확정한다. 시한 전이거나 이미 정해졌으면 하지 않는다.
	 *
	 * @return 이번 호출에서 실제로 정했으면 true
	 */
	public boolean autoPick(Long meetingId) {
		Boolean picked = inNewTransaction(false).execute(status -> {
			Meeting meeting = meetingRepository.findByIdForUpdate(meetingId).orElse(null);
			FinalResult result = finalResultRepository.findByMeetingId(meetingId).orElse(null);
			if (meeting == null || result == null || result.getPlaceStatus() != PlaceStatus.AWAITING_HOST
					|| result.getHostDecisionDeadline() == null
					|| result.getHostDecisionDeadline().isAfter(Instant.now())) {
				return false;
			}
			if (result.getCandidates().isEmpty()) {
				result.leaveUndecided("장소 후보가 없어 추후 협의로 남겼습니다.");
				return true;
			}
			FinalResultCandidate first = result.getCandidates().get(0);
			result.decidePlace(first, DecisionType.AUTO_PICK,
					"호스트가 시한 안에 고르지 않아 첫 번째 후보를 자동으로 선정했습니다.");
			return true;
		});
		return Boolean.TRUE.equals(picked);
	}

	private TransactionTemplate inNewTransaction(boolean readOnly) {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(readOnly);
		return template;
	}
}
