package com.coyote.moyeon.place;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** 장소 검색 호출 제한: 외부 API 호출량을 지키기 위해 호스트는 사용자별, 게스트는 IP별로 제한한다. */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:place-search-limit;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"app.rate-limit.place-search.max=3"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlaceSearchRateLimitTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final AtomicInteger SEQ = new AtomicInteger();

	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JwtTokenService jwtTokenService;

	private String newHostToken() {
		User user = userRepository.save(new User("place-limit-" + SEQ.incrementAndGet() + "@example.com",
				passwordEncoder.encode("password123"), "호스트", null));
		return jwtTokenService.issue(user.getId()).accessToken();
	}

	private static RequestPostProcessor fromIp(String ip) {
		return request -> {
			request.setRemoteAddr(ip);
			return request;
		};
	}

	private ResultActions hostSearch(String token) throws Exception {
		return mockMvc.perform(get("/api/places/search").header("Authorization", "Bearer " + token)
				.param("latitude", "37.4979").param("longitude", "127.0276"));
	}

	private ResultActions guestSearch(String inviteToken, String ip) throws Exception {
		return mockMvc.perform(get("/api/invites/" + inviteToken + "/places/search").with(fromIp(ip))
				.header("X-Guest-Id", UUID.randomUUID().toString())
				.param("latitude", "37.4979").param("longitude", "127.0276"));
	}

	private String createOpenMeetingToken(String hostToken) throws Exception {
		Instant later = Instant.now().plus(2, ChronoUnit.DAYS);
		Instant due = Instant.ofEpochSecond((later.getEpochSecond() / 600 + 1) * 600);
		LocalDate today = LocalDate.now(KST);
		String body = """
				{"title":"호출 제한","description":null,"responseDeadline":"%s","linkExpirationDate":null,
				 "candidateStartDate":"%s","candidateEndDate":"%s","placePolicy":{"mode":"OPEN"},
				 "selectedDates":["%s"],"placeMode":"OPEN"}
				""".formatted(due, today.plusDays(1), today.plusDays(10), today.plusDays(2));
		String response = mockMvc.perform(post("/api/meetings").header("Authorization", "Bearer " + hostToken)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(response, "$.inviteToken");
	}

	@Test
	void hostSearchesAreLimitedPerUser() throws Exception {
		String token = newHostToken();
		for (int i = 0; i < 3; i++) {
			hostSearch(token).andExpect(status().isOk());
		}
		hostSearch(token).andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
		// 다른 사용자는 영향을 받지 않는다.
		hostSearch(newHostToken()).andExpect(status().isOk());
	}

	@Test
	void guestSearchesAreLimitedPerIp() throws Exception {
		String inviteToken = createOpenMeetingToken(newHostToken());
		for (int i = 0; i < 3; i++) {
			guestSearch(inviteToken, "10.20.0.1").andExpect(status().isOk());
		}
		guestSearch(inviteToken, "10.20.0.1").andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
		guestSearch(inviteToken, "10.20.0.2").andExpect(status().isOk());
	}
}
