package com.coyote.moyeon.place;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coyote.moyeon.auth.JwtTokenService;
import com.coyote.moyeon.user.User;
import com.coyote.moyeon.user.UserRepository;
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

/** AI 추천 호출 제한: 사용자 한 명이 1시간에 요청할 수 있는 횟수를 제한한다. */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:ai-recommend-limit;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"app.ai.user-hourly-limit=2"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiRecommendRateLimitTest {

	private static final AtomicInteger SEQ = new AtomicInteger();

	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JwtTokenService jwtTokenService;

	private String newHostToken() {
		User user = userRepository.save(new User("ai-limit-" + SEQ.incrementAndGet() + "@example.com",
				passwordEncoder.encode("password123"), "호스트", null));
		return jwtTokenService.issue(user.getId()).accessToken();
	}

	private ResultActions recommend(String token) throws Exception {
		return mockMvc.perform(post("/api/places/recommend").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":37.4979,\"longitude\":127.0276}"));
	}

	@Test
	void recommendationsAreLimitedPerUser() throws Exception {
		String token = newHostToken();
		recommend(token).andExpect(status().isOk());
		recommend(token).andExpect(status().isOk());

		recommend(token).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
		// 다른 사용자는 영향을 받지 않는다.
		recommend(newHostToken()).andExpect(status().isOk());
	}
}
