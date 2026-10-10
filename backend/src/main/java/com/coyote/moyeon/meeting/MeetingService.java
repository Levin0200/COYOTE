package com.coyote.moyeon.meeting;

import com.coyote.moyeon.common.exception.BusinessException;
import com.coyote.moyeon.meeting.dto.CreateMeetingRequest;
import com.coyote.moyeon.meeting.dto.GuestResponseRequest;
import com.coyote.moyeon.meeting.dto.InviteInfoResponse;
import com.coyote.moyeon.meeting.dto.MeetingDetailResponse;
import com.coyote.moyeon.meeting.dto.MyResponseView;
import com.coyote.moyeon.meeting.dto.PlaceOptionData;
import com.coyote.moyeon.meeting.dto.PlacePolicyData;
import com.coyote.moyeon.meeting.dto.PlacePolicyView;
import com.coyote.moyeon.meeting.dto.ResponseData;
import com.coyote.moyeon.user.User;
import com.coyote.moyeon.user.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingService {

	public record SubmitResult(boolean created, MyResponseView response) {
	}

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int NICKNAME_MAX = 20;
	private static final long CANDIDATE_RANGE_MAX_DAYS = 62;
	private static final int PLACE_OPTIONS_MAX = 10;
	private static final Pattern GUEST_ID = Pattern.compile(
			"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	private final SecureRandom random = new SecureRandom();
	private final UserRepository userRepository;
	private final MeetingRepository meetingRepository;
	private final ParticipantRepository participantRepository;
	private final MeetingResponseRepository responseRepository;

	public MeetingService(UserRepository userRepository, MeetingRepository meetingRepository,
			ParticipantRepository participantRepository, MeetingResponseRepository responseRepository) {
		this.userRepository = userRepository;
		this.meetingRepository = meetingRepository;
		this.participantRepository = participantRepository;
		this.responseRepository = responseRepository;
	}

	@Transactional
	public MeetingDetailResponse create(Long userId, CreateMeetingRequest request) {
		User host = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
		Instant now = Instant.now();
		validateDeadlines(request.responseDueDate(), request.linkExpirationDate(), now);
		validateCandidateRange(request.candidateStartDate(), request.candidateEndDate(), LocalDate.now(KST));
		PolicyValues policy = normalizePolicy(request.placePolicy());

		String description = request.description() == null || request.description().isBlank()
				? null : request.description().trim();
		Meeting draft = new Meeting(newInviteToken(), host, request.title().trim(), description,
				request.responseDueDate(), request.linkExpirationDate(), request.candidateStartDate(),
				request.candidateEndDate(), policy.mode(), policy.region(), policy.radius(), policy.keyword(),
				policy.fixedPlace(), policy.options());
		NormalizedResponse normalized = normalize(request.toResponseData(), draft, true);

		Meeting meeting = meetingRepository.save(draft);
		Participant hostParticipant = participantRepository.save(
				Participant.host(meeting, host, truncate(host.getName().trim(), NICKNAME_MAX)));
		MeetingResponse response = new MeetingResponse(meeting, hostParticipant);
		response.update(normalized.dates(), normalized.placeMode(), normalized.region(), normalized.placePreference(),
				normalized.radius(), normalized.keyword());
		responseRepository.save(response);
		return toDetail(meeting, MyResponseView.from(response), now);
	}

	@Transactional(readOnly = true)
	public MeetingDetailResponse getForHost(Long userId, Long meetingId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "모임을 찾을 수 없습니다."));
		if (!meeting.getHost().getId().equals(userId)) {
			throw new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", "이 모임에 접근할 권한이 없습니다.");
		}
		MyResponseView mine = participantRepository.findByMeetingIdAndRole(meetingId, ParticipantRole.HOST)
				.flatMap(p -> responseRepository.findByParticipantId(p.getId()))
				.map(MyResponseView::from)
				.orElse(null);
		return toDetail(meeting, mine, Instant.now());
	}

	@Transactional(readOnly = true)
	public InviteInfoResponse getInvite(String inviteToken) {
		Instant now = Instant.now();
		Meeting meeting = findAccessibleByToken(inviteToken, now);
		return new InviteInfoResponse(meeting.getTitle(), meeting.getDescription(), meeting.getResponseDueDate(),
				meeting.getLinkExpirationDate(), meeting.getCandidateStartDate(), meeting.getCandidateEndDate(),
				PlacePolicyView.from(meeting), meeting.getStatus(), meeting.isClosed(now),
				participantRepository.countByMeetingId(meeting.getId()),
				responseRepository.countByMeetingId(meeting.getId()));
	}

	@Transactional
	public SubmitResult submitGuestResponse(String inviteToken, String rawGuestId, GuestResponseRequest request) {
		Instant now = Instant.now();
		Meeting meeting = findAccessibleByToken(inviteToken, now);
		String guestId = normalizeGuestId(rawGuestId);
		if (meeting.isClosed(now)) {
			throw new BusinessException(HttpStatus.CONFLICT, "MEETING_CLOSED", "응답이 마감되어 수정할 수 없습니다.");
		}
		NormalizedResponse normalized = normalize(request.toResponseData(), meeting, false);
		String nickname = request.nickname().trim();

		boolean created = false;
		Participant participant = participantRepository.findByMeetingIdAndGuestId(meeting.getId(), guestId).orElse(null);
		if (participant == null) {
			try {
				participant = participantRepository.saveAndFlush(Participant.guest(meeting, guestId, nickname));
			} catch (DataIntegrityViolationException e) {
				throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_SUBMISSION", "잠시 후 다시 시도해주세요.");
			}
			created = true;
		} else {
			participant.changeNickname(nickname);
		}

		MeetingResponse response = responseRepository.findByParticipantId(participant.getId()).orElse(null);
		if (response == null) {
			response = new MeetingResponse(meeting, participant);
			created = true;
		}
		response.update(normalized.dates(), normalized.placeMode(), normalized.region(), normalized.placePreference(),
				normalized.radius(), normalized.keyword());
		responseRepository.save(response);
		return new SubmitResult(created, MyResponseView.from(response));
	}

	@Transactional(readOnly = true)
	public MyResponseView getMyResponse(String inviteToken, String rawGuestId) {
		Meeting meeting = findAccessibleByToken(inviteToken, Instant.now());
		String guestId = normalizeGuestId(rawGuestId);
		return participantRepository.findByMeetingIdAndGuestId(meeting.getId(), guestId)
				.flatMap(p -> responseRepository.findByParticipantId(p.getId()))
				.map(MyResponseView::from)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RESPONSE_NOT_FOUND", "저장된 응답이 없습니다."));
	}

	private Meeting findAccessibleByToken(String inviteToken, Instant now) {
		Meeting meeting = meetingRepository.findByInviteToken(inviteToken)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "유효하지 않은 초대 링크입니다."));
		if (meeting.isLinkExpired(now)) {
			throw new BusinessException(HttpStatus.GONE, "INVITE_EXPIRED", "만료된 초대 링크입니다.");
		}
		return meeting;
	}

	private MeetingDetailResponse toDetail(Meeting meeting, MyResponseView mine, Instant now) {
		return new MeetingDetailResponse(meeting.getId(), meeting.getInviteToken(), meeting.getTitle(),
				meeting.getDescription(), meeting.getResponseDueDate(), meeting.getLinkExpirationDate(),
				meeting.getCandidateStartDate(), meeting.getCandidateEndDate(), PlacePolicyView.from(meeting),
				meeting.getStatus(), meeting.isClosed(now), participantRepository.countByMeetingId(meeting.getId()),
				responseRepository.countByMeetingId(meeting.getId()), meeting.getCreatedAt(), mine);
	}

	private void validateDeadlines(Instant responseDueDate, Instant linkExpirationDate, Instant now) {
		if (!responseDueDate.isAfter(now)) {
			throw invalid("응답 마감일은 현재 이후로 설정해주세요.");
		}
		ZonedDateTime due = responseDueDate.atZone(KST);
		if (due.getMinute() % 10 != 0 || due.getSecond() != 0 || due.getNano() != 0) {
			throw invalid("응답 마감일은 10분 단위로 설정해주세요.");
		}
		if (linkExpirationDate != null && !linkExpirationDate.isAfter(responseDueDate)) {
			throw invalid("링크 만료일은 응답 마감일 이후로 설정해주세요.");
		}
	}

	private void validateCandidateRange(LocalDate start, LocalDate end, LocalDate today) {
		if (start.isBefore(today)) {
			throw invalid("후보 시작일은 오늘 이후로 설정해주세요.");
		}
		if (start.isAfter(end)) {
			throw invalid("후보 시작일은 종료일보다 늦을 수 없습니다.");
		}
		if (ChronoUnit.DAYS.between(start, end) + 1 > CANDIDATE_RANGE_MAX_DAYS) {
			throw invalid("후보 날짜 범위는 최대 " + CANDIDATE_RANGE_MAX_DAYS + "일까지 설정할 수 있습니다.");
		}
	}

	private record PolicyValues(PlacePolicyMode mode, String region, Integer radius, String keyword,
			String fixedPlace, List<PlaceOption> options) {
	}

	private PolicyValues normalizePolicy(PlacePolicyData data) {
		switch (data.mode()) {
			case FIXED -> {
				String fixedPlace = blankToNull(data.fixedPlace());
				if (fixedPlace == null) {
					throw invalid("확정 장소를 입력해주세요.");
				}
				return new PolicyValues(PlacePolicyMode.FIXED, null, null, null, fixedPlace, List.of());
			}
			case RECOMMEND -> {
				String region = blankToNull(data.region());
				if (region == null) {
					throw invalid("추천 기준 지역을 입력해주세요.");
				}
				if (data.radius() == null) {
					throw invalid("추천 범위를 선택해주세요.");
				}
				List<PlaceOptionData> given = data.options() == null ? List.of() : data.options();
				if (given.isEmpty() || given.size() > PLACE_OPTIONS_MAX) {
					throw invalid("추천 후보는 1개 이상 " + PLACE_OPTIONS_MAX + "개 이하로 입력해주세요.");
				}
				List<PlaceOption> options = new ArrayList<>();
				HashSet<String> names = new HashSet<>();
				for (PlaceOptionData option : given) {
					String name = option.name().trim();
					if (!names.add(name)) {
						throw invalid("추천 후보 이름이 중복되었습니다.");
					}
					options.add(new PlaceOption(name, blankToNull(option.distance())));
				}
				return new PolicyValues(PlacePolicyMode.RECOMMEND, region, data.radius(), blankToNull(data.keyword()),
						null, options);
			}
			default -> {
				return new PolicyValues(PlacePolicyMode.OPEN, null, null, null, null, List.of());
			}
		}
	}

	private record NormalizedResponse(Set<LocalDate> dates, PlaceMode placeMode, String region, String placePreference,
			Integer radius, String keyword) {
	}

	private NormalizedResponse normalize(ResponseData data, Meeting meeting, boolean host) {
		LocalDate today = LocalDate.now(KST);
		TreeSet<LocalDate> dates = new TreeSet<>(data.selectedDates());
		if (dates.first().isBefore(today)) {
			throw invalid("오늘 이전 날짜는 선택할 수 없습니다.");
		}
		if (dates.first().isBefore(meeting.getCandidateStartDate())
				|| dates.last().isAfter(meeting.getCandidateEndDate())) {
			throw invalid("선택한 날짜가 후보 날짜 범위를 벗어났습니다.");
		}
		PlaceMode mode = data.placeMode();
		String place = blankToNull(data.placePreference());

		switch (meeting.getPlacePolicyMode()) {
			case FIXED -> {
				if (mode != PlaceMode.FIXED) {
					throw invalid("장소가 확정된 모임에서는 확정 장소로만 응답할 수 있습니다.");
				}
				if (place != null && !place.equals(meeting.getFixedPlace())) {
					throw invalid("확정된 장소와 다른 장소는 선택할 수 없습니다.");
				}
				return new NormalizedResponse(dates, PlaceMode.FIXED, null, meeting.getFixedPlace(), null, null);
			}
			case RECOMMEND -> {
				if (mode != PlaceMode.RECOMMEND) {
					throw invalid("추천 후보 중에서 장소를 선택해주세요.");
				}
				if (place == null || meeting.getPlaceOptions().stream().noneMatch(o -> o.name().equals(place))) {
					throw invalid("추천 후보 중에서 장소를 선택해주세요.");
				}
				return new NormalizedResponse(dates, PlaceMode.RECOMMEND, meeting.getPolicyRegion(), place,
						meeting.getPolicyRadiusKm(), meeting.getPolicyKeyword());
			}
			default -> {
				if (mode == PlaceMode.OPEN) {
					return new NormalizedResponse(dates, PlaceMode.OPEN, null, null, null, null);
				}
				if (mode != PlaceMode.PROPOSE || host) {
					throw invalid(host ? "호스트는 장소 제안 방식을 사용할 수 없습니다."
							: "이 모임에서는 장소 제안 또는 건너뛰기만 선택할 수 있습니다.");
				}
				if (place == null) {
					throw invalid("제안할 장소를 입력해주세요.");
				}
				return new NormalizedResponse(dates, PlaceMode.PROPOSE, blankToNull(data.region()), place,
						data.recommendationRadius(), blankToNull(data.recommendationKeyword()));
			}
		}
	}

	private String normalizeGuestId(String rawGuestId) {
		if (rawGuestId == null || !GUEST_ID.matcher(rawGuestId.trim()).matches()) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_GUEST_ID", "올바르지 않은 게스트 식별자입니다.");
		}
		return rawGuestId.trim().toLowerCase(Locale.ROOT);
	}

	private String newInviteToken() {
		byte[] bytes = new byte[24];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String truncate(String value, int maxCodePoints) {
		if (value.codePointCount(0, value.length()) <= maxCodePoints) {
			return value;
		}
		return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private BusinessException invalid(String message) {
		return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
	}
}
