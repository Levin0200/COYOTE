package com.coyote.moyeon.meeting;

import com.coyote.moyeon.common.exception.BusinessException;
import com.coyote.moyeon.meeting.dto.CreateMeetingRequest;
import com.coyote.moyeon.meeting.dto.GuestResponseRequest;
import com.coyote.moyeon.meeting.dto.InviteInfoResponse;
import com.coyote.moyeon.meeting.dto.MeetingDetailResponse;
import com.coyote.moyeon.meeting.dto.MyResponseView;
import com.coyote.moyeon.meeting.dto.ResponseData;
import com.coyote.moyeon.user.User;
import com.coyote.moyeon.user.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
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
		NormalizedResponse normalized = normalize(request.toResponseData());

		String description = request.description() == null || request.description().isBlank()
				? null : request.description().trim();
		Meeting meeting = meetingRepository.save(new Meeting(newInviteToken(), host, request.title().trim(),
				description, request.responseDueDate(), request.linkExpirationDate()));
		Participant hostParticipant = participantRepository.save(
				Participant.host(meeting, host, truncate(host.getName().trim(), NICKNAME_MAX)));
		MeetingResponse response = new MeetingResponse(meeting, hostParticipant);
		response.update(normalized.dates(), normalized.placeMode(), normalized.region(), normalized.placePreference());
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
				meeting.getLinkExpirationDate(), meeting.getStatus(), meeting.isClosed(now),
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
		NormalizedResponse normalized = normalize(request.toResponseData());
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
		response.update(normalized.dates(), normalized.placeMode(), normalized.region(), normalized.placePreference());
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

	private record NormalizedResponse(Set<LocalDate> dates, PlaceMode placeMode, String region, String placePreference) {
	}

	private NormalizedResponse normalize(ResponseData data) {
		LocalDate today = LocalDate.now(KST);
		TreeSet<LocalDate> dates = new TreeSet<>(data.selectedDates());
		if (dates.first().isBefore(today)) {
			throw invalid("오늘 이전 날짜는 선택할 수 없습니다.");
		}
		String region = blankToNull(data.region());
		String placePreference = blankToNull(data.placePreference());
		if (data.placeMode() == PlaceMode.SKIP) {
			region = null;
			placePreference = null;
		} else if (placePreference == null) {
			throw invalid("장소를 선택해주세요.");
		}
		return new NormalizedResponse(dates, data.placeMode(), region, placePreference);
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
