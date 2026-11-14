package com.coyote.moyeon.meeting;

import com.coyote.moyeon.place.Place;
import com.coyote.moyeon.place.consensus.ConsensusOutcome;
import com.coyote.moyeon.place.consensus.PlaceVote;
import com.coyote.moyeon.place.consensus.Proposal;
import com.coyote.moyeon.place.consensus.SpatialConsensus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 득표 집계 기본 생성기. 날짜 동률은 오늘과 가장 가까운 하나를 고른다.
 *
 * <p>장소는 정책에 따라 다르다.</p>
 * <ul>
 * <li>FIXED·RECOMMEND: 후보별 득표를 센다. 단독 1위면 확정(DECIDED), 공동 1위면 호스트가 고르게 한다(AWAITING_HOST),
 * 득표가 없으면 추후 협의(NONE).</li>
 * <li>OPEN: 게스트가 제안한 장소를 {@link SpatialConsensus}로 종합한다. 단독 1위면 확정, 제안이 없으면 추후 협의,
 * 제안이 흩어졌으면 후보를 만들어야 하므로 PENDING으로 두고 마감 직후 {@link PlaceDecisionService}가 마무리한다.
 * 좌표 없이 이름만 받은 예전 제안은 합의에 쓸 수 없어, 좌표 있는 제안이 하나도 없을 때만 이름 득표로 센다.</li>
 * </ul>
 */
@Component
public class VoteCountResultGenerator implements FinalResultGenerator {

	@Override
	public GeneratedResult generate(ResultInput input) {
		Map<LocalDate, Integer> dateCounts = new HashMap<>();
		for (ResultInput.ResponseSnapshot response : input.responses()) {
			response.dates().forEach(date -> dateCounts.merge(date, 1, Integer::sum));
		}
		if (dateCounts.isEmpty()) {
			throw new IllegalStateException("결과를 계산할 날짜 응답이 없습니다.");
		}

		int bestDateCount = dateCounts.values().stream().max(Integer::compare).orElseThrow();
		List<LocalDate> tiedDates = dateCounts.entrySet().stream()
				.filter(e -> e.getValue() == bestDateCount).map(Map.Entry::getKey).sorted().toList();
		LocalDate selectedDate = tiedDates.stream()
				.min(Comparator.comparingLong((LocalDate d) -> Math.abs(ChronoUnit.DAYS.between(input.today(), d)))
						.thenComparing(Comparator.naturalOrder()))
				.orElseThrow();
		String dateReason = tiedDates.size() > 1
				? "날짜 공동 1위 중 오늘과 가장 가까운 날짜를 우선했습니다. "
				: "가장 많은 참여자가 선택한 날짜를 반영했습니다. ";

		PlaceOutcome place = input.policyMode() == PlacePolicyMode.OPEN ? decideOpen(input) : decideByVotes(input);
		DecisionType decisionType = place.decisionType() != null ? place.decisionType()
				: (tiedDates.size() > 1 || place.status() == PlaceStatus.AWAITING_HOST ? DecisionType.TIE_BREAK
						: DecisionType.MAJORITY);

		return new GeneratedResult(selectedDate, bestDateCount, place.places(), place.votes(),
				input.responses().size(), decisionType, dateReason + place.reason(), place.status(),
				place.candidates());
	}

	/** 장소를 아직 못 정했거나(PENDING·AWAITING_HOST) 정한 결과. decisionType이 null이면 날짜 동률 여부로 정한다. */
	private record PlaceOutcome(PlaceStatus status, List<SelectedPlace> places, int votes,
			List<FinalResultCandidate> candidates, String reason, DecisionType decisionType) {

		static PlaceOutcome decided(SelectedPlace place, int votes, String reason) {
			return new PlaceOutcome(PlaceStatus.DECIDED, List.of(place), votes, List.of(), reason, null);
		}

		static PlaceOutcome none(String reason) {
			return new PlaceOutcome(PlaceStatus.NONE, List.of(), 0, List.of(), reason, null);
		}

		static PlaceOutcome awaitManualHostChoice(String reason) {
			return new PlaceOutcome(PlaceStatus.AWAITING_HOST, List.of(), 0, List.of(), reason,
					DecisionType.TIE_BREAK);
		}
	}

	/** FIXED·RECOMMEND(그리고 이름만 받은 OPEN 제안): 장소 ID가 있으면 ID로, 없으면 이름으로 득표를 센다. */
	private PlaceOutcome decideByVotes(ResultInput input) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		Map<String, FinalResultCandidate> candidatesByKey = new HashMap<>();
		for (ResultInput.ResponseSnapshot response : input.responses()) {
			String place = response.placePreference();
			if (place == null || response.placeMode() == PlaceMode.OPEN) {
				continue;
			}
			PlaceInfo info = response.place();
			String key = info != null && info.id() != null ? "id:" + info.id() : "name:" + place;
			counts.merge(key, 1, Integer::sum);
			candidatesByKey.putIfAbsent(key, candidateOf(place, info));
		}

		int best = counts.values().stream().max(Integer::compare).orElse(0);
		if (best == 0) {
			return PlaceOutcome.none("유효한 장소 응답이 없어 장소는 추후 협의로 남겼습니다.");
		}
		List<FinalResultCandidate> tied = counts.entrySet().stream().filter(e -> e.getValue() == best)
				.map(e -> withVotes(candidatesByKey.get(e.getKey()), best))
				.sorted(Comparator.comparingInt((FinalResultCandidate c) -> optionIndex(input.options(), c))
						.thenComparing(FinalResultCandidate::name))
				.toList();
		if (tied.size() == 1) {
			return PlaceOutcome.decided(tied.get(0).toSelectedPlace(), best, "가장 많은 참여자가 선택한 장소를 반영했습니다.");
		}
		return new PlaceOutcome(PlaceStatus.AWAITING_HOST, List.of(), best, tied,
				"장소 공동 1위가 있어 호스트가 최종 장소를 고릅니다.", DecisionType.TIE_BREAK);
	}

	/** OPEN: 좌표가 있는 제안을 공간적으로 종합한다. */
	private PlaceOutcome decideOpen(ResultInput input) {
		List<Proposal> proposals = proposalsOf(input);
		if (proposals.isEmpty()) {
			boolean hasNameOnly = input.responses().stream()
					.anyMatch(r -> r.placeMode() == PlaceMode.PROPOSE && r.placePreference() != null);
			return hasNameOnly ? decideByVotes(input)
					: PlaceOutcome.awaitManualHostChoice(
							"모든 참여자가 장소 의견을 건너뛰어 호스트가 최종 장소를 직접 선택합니다.");
		}
		ConsensusOutcome outcome = SpatialConsensus.decide(proposals);
		if (outcome instanceof ConsensusOutcome.Majority majority) {
			PlaceVote winner = majority.winner();
			Place place = winner.place();
			return PlaceOutcome.decided(new SelectedPlace(place.name(), place.id(), place.address(),
					place.latitude(), place.longitude()), winner.votes(), "가장 많은 참여자가 제안한 장소를 반영했습니다.");
		}
		return new PlaceOutcome(PlaceStatus.PENDING, List.of(), 0, List.of(), "제안된 장소가 흩어져 후보를 분석하고 있습니다.",
				DecisionType.SPATIAL);
	}

	/** 장소 ID와 좌표가 있는 PROPOSE 응답만 합의 대상이다. 순서는 응답 순서다. */
	public static List<Proposal> proposalsOf(ResultInput input) {
		List<Proposal> proposals = new ArrayList<>();
		int order = 0;
		for (ResultInput.ResponseSnapshot response : input.responses()) {
			order++;
			PlaceInfo info = response.place();
			if (response.placeMode() != PlaceMode.PROPOSE || info == null || info.id() == null
					|| info.latitude() == null || info.longitude() == null) {
				continue;
			}
			proposals.add(new Proposal(toPlace(info), order));
		}
		return proposals;
	}

	/** 저장된 장소 정보를 합의 계산용 장소로 바꾼다(업종 코드는 저장하지 않으므로 null). */
	public static Place toPlace(PlaceInfo info) {
		return new Place(info.id(), info.name(), null, null, null, info.category() == null ? "" : info.category(),
				info.address() == null ? "" : info.address(), info.latitude(), info.longitude(), 0);
	}

	private static FinalResultCandidate candidateOf(String name, PlaceInfo info) {
		if (info == null) {
			return new FinalResultCandidate(name, null, null, null, null, null, 0, null);
		}
		return new FinalResultCandidate(name, info.id(), info.address(), info.category(), info.latitude(),
				info.longitude(), 0, null);
	}

	private static FinalResultCandidate withVotes(FinalResultCandidate candidate, int votes) {
		return new FinalResultCandidate(candidate.name(), candidate.placeId(), candidate.address(),
				candidate.category(), candidate.latitude(), candidate.longitude(), votes, candidate.reason());
	}

	/** 호스트가 정한 추천 후보 순서에서 몇 번째인지(없으면 맨 뒤). */
	private static int optionIndex(List<PlaceOption> options, FinalResultCandidate candidate) {
		for (int i = 0; i < options.size(); i++) {
			PlaceOption option = options.get(i);
			if (candidate.matches(option.placeId(), option.name())) {
				return i;
			}
		}
		return Integer.MAX_VALUE;
	}
}
