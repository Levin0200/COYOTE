package com.coyote.moyeon.common.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHeadersAndCorsTest {

	private static final String ORIGIN = "http://localhost:5173";
	private static final String SECRET = "a-secret-with-at-least-32-bytes-0123456789";

	@Autowired
	MockMvc mockMvc;

	private static SecurityConfig config(String origins, String profile) {
		MockEnvironment env = new MockEnvironment();
		env.setActiveProfiles(profile);
		return new SecurityConfig(SECRET, origins, env);
	}

	@Test
	void responsesCarrySecurityHeaders() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
				.andExpect(header().string("Referrer-Policy", "no-referrer"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"));
	}

	@Test
	void preflightAllowsTheHeadersTheFrontendUses() throws Exception {
		mockMvc.perform(options("/api/meetings")
				.header("Origin", ORIGIN)
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "authorization,content-type,x-guest-id"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", ORIGIN))
				.andExpect(header().exists("Access-Control-Max-Age"));
	}

	@Test
	void preflightRejectsOtherHeadersAndOrigins() throws Exception {
		mockMvc.perform(options("/api/meetings")
				.header("Origin", ORIGIN)
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "x-evil"))
				.andExpect(status().isForbidden());
		mockMvc.perform(options("/api/meetings")
				.header("Origin", "http://evil.example.com")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden());
	}

	@Test
	void retryAfterIsExposedToTheBrowser() throws Exception {
		mockMvc.perform(get("/api/health").header("Origin", ORIGIN))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Expose-Headers", "Retry-After"));
	}

	@Test
	void outsideDevAndTest_wildcardOrBlankOriginsFailStartup() {
		assertThrows(IllegalStateException.class, () -> config("*", "prod"));
		assertThrows(IllegalStateException.class, () -> config("http://a.example.com, *", "prod"));
		assertThrows(IllegalStateException.class, () -> config("  ", "prod"));
		assertThrows(IllegalStateException.class, () -> config("", "mariadb"));
	}

	@Test
	void explicitOriginsAreAcceptedEverywhere() {
		assertDoesNotThrow(() -> config("https://moyeon.example.com,https://www.moyeon.example.com", "prod"));
		assertDoesNotThrow(() -> config(ORIGIN, "dev"));
	}
}
