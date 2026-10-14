package com.coyote.moyeon.user;

import com.coyote.moyeon.common.exception.BusinessException;
import com.coyote.moyeon.common.ratelimit.RateLimitGuard;
import com.coyote.moyeon.common.ratelimit.RateLimitProperties;
import com.coyote.moyeon.meeting.FinalResultRepository;
import com.coyote.moyeon.meeting.Meeting;
import com.coyote.moyeon.meeting.MeetingRepository;
import com.coyote.moyeon.meeting.MeetingResponseRepository;
import com.coyote.moyeon.meeting.ParticipantRepository;
import com.coyote.moyeon.user.dto.UpdateUserRequest;
import com.coyote.moyeon.user.dto.UserResponse;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final MeetingRepository meetingRepository;
	private final MeetingResponseRepository responseRepository;
	private final ParticipantRepository participantRepository;
	private final FinalResultRepository finalResultRepository;
	private final PasswordEncoder passwordEncoder;
	private final RateLimitGuard guard;
	private final RateLimitProperties limits;

	public UserService(UserRepository userRepository, MeetingRepository meetingRepository,
			MeetingResponseRepository responseRepository, ParticipantRepository participantRepository,
			FinalResultRepository finalResultRepository, PasswordEncoder passwordEncoder, RateLimitGuard guard,
			RateLimitProperties limits) {
		this.userRepository = userRepository;
		this.meetingRepository = meetingRepository;
		this.responseRepository = responseRepository;
		this.participantRepository = participantRepository;
		this.finalResultRepository = finalResultRepository;
		this.passwordEncoder = passwordEncoder;
		this.guard = guard;
		this.limits = limits;
	}

	@Transactional(readOnly = true)
	public UserResponse getMe(Long userId) {
		return UserResponse.from(find(userId));
	}

	@Transactional
	public UserResponse updateMe(Long userId, UpdateUserRequest request) {
		User user = find(userId);
		String address = request.address() == null || request.address().isBlank() ? null : request.address().trim();
		user.updateProfile(request.name().trim(), address);
		return UserResponse.from(user);
	}

	/**
	 * 회원 탈퇴. 로그인 토큰만으로 계정과 모임이 영구 삭제되지 않도록 비밀번호를 한 번 더 확인한다.
	 * 비밀번호 실패가 이어지면 로그인과 같은 기준으로 잠근다.
	 * 비밀번호가 틀렸을 때 401을 쓰면 프론트가 로그아웃 처리하므로 403을 쓴다.
	 */
	@Transactional
	public void deleteMe(Long userId, String password) {
		User user = find(userId);
		String key = "delete-fail:" + userId;
		Duration lock = Duration.ofMinutes(limits.getLogin().getLockMinutes());
		int maxFailures = limits.getLogin().getMaxFailures();
		guard.assertNotLocked(key, maxFailures, lock);
		if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
			guard.recordFailure(key, maxFailures, lock);
			throw new BusinessException(HttpStatus.FORBIDDEN, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다.");
		}
		guard.clear(key);
		for (Meeting meeting : meetingRepository.findByHostIdOrderByIdDesc(userId)) {
			finalResultRepository.findByMeetingId(meeting.getId()).ifPresent(finalResultRepository::delete);
			finalResultRepository.flush();

			responseRepository.deleteAll(responseRepository.findAllWithDatesByMeetingId(meeting.getId()));
			responseRepository.flush();

			participantRepository.deleteAll(participantRepository.findByMeetingIdOrderByIdAsc(meeting.getId()));
			participantRepository.flush();

			meetingRepository.delete(meeting);
			meetingRepository.flush();
		}
		userRepository.delete(user);
		userRepository.flush();
	}

	private User find(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
	}
}
