package com.coyote.moyeon.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coyote.moyeon.common.exception.BusinessException;
import com.coyote.moyeon.common.ratelimit.InMemoryRateLimiter;
import com.coyote.moyeon.common.ratelimit.RateLimitGuard;
import com.coyote.moyeon.common.ratelimit.RateLimitProperties;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

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
	@Mock
	PasswordEncoder passwordEncoder;

	UserService userService;

	@BeforeEach
	void setUp() {
		RateLimitGuard guard = new RateLimitGuard(new InMemoryRateLimiter());
		userService = new UserService(userRepository, meetingRepository, responseRepository,
				participantRepository, finalResultRepository, passwordEncoder, guard, new RateLimitProperties());
	}

	@Test
	void deleteMe_deletesUser() {
		User user = new User("delete@example.com", "encoded-password", "탈퇴회원", null);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
		when(meetingRepository.findByHostIdOrderByIdDesc(1L)).thenReturn(List.of());

		userService.deleteMe(1L, "password123");

		verify(userRepository).delete(user);
		verify(userRepository).flush();
	}

	@Test
	void deleteMe_withWrongPassword_isForbiddenAndKeepsUser() {
		User user = new User("keep@example.com", "encoded-password", "회원", null);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

		assertThatThrownBy(() -> userService.deleteMe(1L, "wrong-password"))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
					assertThat(e.getCode()).isEqualTo("INVALID_PASSWORD");
				});

		verify(userRepository, never()).delete(user);
	}

	@Test
	void deleteMe_repeatedWrongPasswords_lockTheAccountDeletion() {
		User user = new User("lock@example.com", "encoded-password", "회원", null);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

		for (int i = 0; i < 5; i++) {
			assertThatThrownBy(() -> userService.deleteMe(1L, "wrong-password"))
					.isInstanceOfSatisfying(BusinessException.class,
							e -> assertThat(e.getCode()).isEqualTo("INVALID_PASSWORD"));
		}
		// 잠긴 뒤에는 올바른 비밀번호도 거부된다.
		assertThatThrownBy(() -> userService.deleteMe(1L, "password123"))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
					assertThat(e.getCode()).isEqualTo("TOO_MANY_REQUESTS");
				});
		verify(userRepository, never()).delete(user);
	}
}
