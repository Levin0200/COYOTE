package com.coyote.moyeon.user;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coyote.moyeon.meeting.FinalResultRepository;
import com.coyote.moyeon.meeting.MeetingRepository;
import com.coyote.moyeon.meeting.MeetingResponseRepository;
import com.coyote.moyeon.meeting.ParticipantRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	UserRepository userRepository;
	@Mock
	MeetingRepository meetingRepository;
	@Mock
	MeetingResponseRepository responseRepository;
	@Mock
	ParticipantRepository participantRepository;
	@Mock
	FinalResultRepository finalResultRepository;

	UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, meetingRepository, responseRepository,
				participantRepository, finalResultRepository);
	}

	@Test
	void deleteMe_deletesUser() {
		User user = new User("delete@example.com", "encoded-password", "탈퇴회원", null);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(meetingRepository.findByHostIdOrderByIdDesc(1L)).thenReturn(List.of());

		userService.deleteMe(1L);

		verify(userRepository).delete(user);
		verify(userRepository).flush();
	}
}
