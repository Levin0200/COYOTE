package com.coyote.moyeon.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coyote.moyeon.auth.JwtTokenService;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountDeletionTest {

	private static final AtomicInteger SEQ = new AtomicInteger();

	@Autowired MockMvc mockMvc;
	@Autowired UserRepository userRepository;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JwtTokenService jwtTokenService;

	private String newUserToken() {
		User user = userRepository.save(new User("delete-flow-" + SEQ.incrementAndGet() + "@example.com",
				passwordEncoder.encode("password123"), "탈퇴회원", null));
		return jwtTokenService.issue(user.getId()).accessToken();
	}

	private org.springframework.test.web.servlet.ResultActions deleteMe(String token, String body) throws Exception {
		var request = delete("/api/users/me").header("Authorization", "Bearer " + token);
		if (body != null) {
			request = request.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return mockMvc.perform(request);
	}

	@Test
	void withoutPassword_isBadRequest() throws Exception {
		String token = newUserToken();
		deleteMe(token, null).andExpect(status().isBadRequest());
		deleteMe(token, "{\"password\":\"\"}").andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
	}

	@Test
	void wrongPassword_isForbiddenNotUnauthorized() throws Exception {
		String token = newUserToken();
		deleteMe(token, "{\"password\":\"wrongpass1\"}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
	}

	@Test
	void correctPassword_deletesTheAccount() throws Exception {
		String token = newUserToken();
		deleteMe(token, "{\"password\":\"password123\"}").andExpect(status().isNoContent());
		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	void withoutToken_isUnauthorized() throws Exception {
		mockMvc.perform(delete("/api/users/me").contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"password123\"}")).andExpect(status().isUnauthorized());
	}
}
