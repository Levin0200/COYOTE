package com.coyote.moyeon.common.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:ratelimit-ip;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"app.rate-limit.ip.auth-per-minute=3",
		"app.rate-limit.ip.invite-per-minute=2"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IpRateLimitFilterTest {

	@Autowired
	MockMvc mockMvc;

	private static RequestPostProcessor fromIp(String ip) {
		return request -> {
			request.setRemoteAddr(ip);
			return request;
		};
	}

	private void login(String ip, int expectedStatus) throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.with(fromIp(ip))
				.header("Origin", "http://localhost:5173")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ip@example.com\",\"password\":\"wrongpass1\"}"))
				.andExpect(status().is(expectedStatus));
	}

	@Test
	void authEndpoints_areLimitedPerIp() throws Exception {
		for (int i = 0; i < 3; i++) {
			login("10.9.0.1", 401);
		}
		mockMvc.perform(post("/api/auth/login")
				.with(fromIp("10.9.0.1"))
				.header("Origin", "http://localhost:5173")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ip@example.com\",\"password\":\"wrongpass1\"}"))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
				.andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
		// 다른 IP는 영향이 없다.
		login("10.9.0.2", 401);
	}

	@Test
	void inviteEndpoints_areLimitedPerIp() throws Exception {
		for (int i = 0; i < 2; i++) {
			mockMvc.perform(get("/api/invites/none").with(fromIp("10.9.1.1"))).andExpect(status().isNotFound());
		}
		mockMvc.perform(get("/api/invites/none").with(fromIp("10.9.1.1"))).andExpect(status().isTooManyRequests());
	}

	@Test
	void healthAndPreflightAreNotCounted() throws Exception {
		for (int i = 0; i < 10; i++) {
			mockMvc.perform(get("/api/health").with(fromIp("10.9.2.1"))).andExpect(status().isOk());
			mockMvc.perform(options("/api/auth/login")
					.with(fromIp("10.9.2.1"))
					.header("Origin", "http://localhost:5173")
					.header("Access-Control-Request-Method", "POST"))
					.andExpect(status().isOk());
		}
	}
}
