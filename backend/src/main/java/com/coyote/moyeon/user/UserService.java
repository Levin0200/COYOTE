package com.coyote.moyeon.user;

import com.coyote.moyeon.common.exception.BusinessException;
import com.coyote.moyeon.meeting.FinalResultRepository;
import com.coyote.moyeon.meeting.Meeting;
import com.coyote.moyeon.meeting.MeetingRepository;
import com.coyote.moyeon.meeting.MeetingResponseRepository;
import com.coyote.moyeon.meeting.ParticipantRepository;
import com.coyote.moyeon.user.dto.UpdateUserRequest;
import com.coyote.moyeon.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final MeetingRepository meetingRepository;
	private final MeetingResponseRepository responseRepository;
	private final ParticipantRepository participantRepository;
	private final FinalResultRepository finalResultRepository;

	public UserService(UserRepository userRepository, MeetingRepository meetingRepository,
			MeetingResponseRepository responseRepository, ParticipantRepository participantRepository,
			FinalResultRepository finalResultRepository) {
		this.userRepository = userRepository;
		this.meetingRepository = meetingRepository;
		this.responseRepository = responseRepository;
		this.participantRepository = participantRepository;
		this.finalResultRepository = finalResultRepository;
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

	@Transactional
	public void deleteMe(Long userId) {
		User user = find(userId);
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
