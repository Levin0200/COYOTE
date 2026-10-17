package com.coyote.moyeon.meeting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coyote.moyeon.auth.JwtTokenService;
import com.coyote.moyeon.user.User;
import com.coyote.moyeon.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 모임당 참여 인원 상한(호스트 포함). 테스트에서는 상한을 3명으로 낮춰 검증한다. */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:participant-limit;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"app.meeting.max-participants=3"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParticipantLimitTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final String GUEST_ID_HEADER = "X-Guest-Id";

	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JwtTokenService jwtTokenService;

	private String newHostToken(String email) {
		User user = userRepository.save(new User(email, passwordEncoder.encode("password123"), "호스트", null));
		return jwtTokenService.issue(user.getId()).accessToken();
	}

	private static String day(int plusDays) {
		return LocalDate.now(KST).plusDays(plusDays).toString();
	}

	private String createOpenMeeting(String token) throws Exception {
		Instant later = Instant.now().plus(2, ChronoUnit.DAYS);
		Instant due = Instant.ofEpochSecond((later.getEpochSecond() / 600 + 1) * 600);
		String body = """
				{"title":"인원 제한","description":"설명","responseDeadline":"%s","linkExpirationDate":null,
				 "candidateStartDate":"%s","candidateEndDate":"%s","placePolicy":{"mode":"OPEN"},
				 "selectedDates":["%s"],"placeMode":"OPEN","region":"송도"}
				""".formatted(due, day(1), day(10), day(2));
		String response = mockMvc.perform(post("/api/meetings").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.inviteToken");
	}

	private ResultActions join(String inviteToken, String guestId) throws Exception {
		return mockMvc.perform(post("/api/invites/" + inviteToken + "/join").header(GUEST_ID_HEADER, guestId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"게스트\"}"));
	}

	private ResultActions respond(String inviteToken, String guestId) throws Exception {
		return mockMvc.perform(put("/api/invites/" + inviteToken + "/response").header(GUEST_ID_HEADER, guestId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"게스트\",\"selectedDates\":[\"" + day(2) + "\"],\"placeMode\":\"OPEN\"}"));
	}

	@Test
	void newGuestsBeyondTheLimit_areRejected() throws Exception {
		String inviteToken = createOpenMeeting(newHostToken("limit-host-1@example.com"));
		// 호스트 1명 + 게스트 2명 = 상한 3명
		String first = UUID.randomUUID().toString();
		join(inviteToken, first).andExpect(status().isCreated());
		join(inviteToken, UUID.randomUUID().toString()).andExpect(status().isCreated());

		join(inviteToken, UUID.randomUUID().toString())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PARTICIPANT_LIMIT"));
		respond(inviteToken, UUID.randomUUID().toString())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PARTICIPANT_LIMIT"));

		// 이미 참여한 게스트는 인원이 가득 차도 다시 참여하거나 응답을 낼 수 있다.
		join(inviteToken, first).andExpect(status().isOk());
		respond(inviteToken, first).andExpect(status().is2xxSuccessful());
	}

	@Test
	void eachMeetingHasItsOwnLimit() throws Exception {
		String full = createOpenMeeting(newHostToken("limit-host-2@example.com"));
		join(full, UUID.randomUUID().toString()).andExpect(status().isCreated());
		join(full, UUID.randomUUID().toString()).andExpect(status().isCreated());
		join(full, UUID.randomUUID().toString()).andExpect(status().isConflict());

		String other = createOpenMeeting(newHostToken("limit-host-3@example.com"));
		join(other, UUID.randomUUID().toString()).andExpect(status().isCreated());
	}
}
