package com.coyote.moyeon.auth;

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

/** 인증번호 발송·로그인 실패 제한 검증. 발송 제한을 켜기 위해 테스트 프로필 기본값을 덮어쓴다. */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:ratelimit-auth;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"app.rate-limit.email-send.cooldown-seconds=60",
		"app.rate-limit.email-send.hourly-max=3",
		"app.rate-limit.email-send.ip-hourly-max=4",
		"app.rate-limit.login.max-failures=3",
		"app.rate-limit.login.ip-max-failures=6",
		"app.rate-limit.login.lock-minutes=15"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRateLimitTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	EmailVerificationRepository verificationRepository;

	private static RequestPostProcessor fromIp(String ip) {
		return request -> {
			request.setRemoteAddr(ip);
			return request;
		};
	}

	private void sendCode(String email, String ip, int expectedStatus) throws Exception {
		mockMvc.perform(post("/api/auth/email-verification")
				.with(fromIp(ip))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\"}"))
				.andExpect(status().is(expectedStatus));
	}

	private void login(String email, String password, String ip, int expectedStatus) throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.with(fromIp(ip))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().is(expectedStatus));
	}

	private void register(String email, String password, String ip) throws Exception {
		sendCode(email, ip, 204);
		String code = verificationRepository.findByEmail(email).orElseThrow().getCode();
		mockMvc.perform(post("/api/auth/email-verification/confirm")
				.with(fromIp(ip))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/auth/signup")
				.with(fromIp(ip))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","name":"김모여","password":"%s","serviceAgree":true,"privacyAgree":true}
						""".formatted(email, password)))
				.andExpect(status().isCreated());
	}

	@Test
	void resendingToSameEmailWithinCooldown_isTooManyRequests() throws Exception {
		sendCode("cool@example.com", "10.1.0.1", 204);
		mockMvc.perform(post("/api/auth/email-verification")
				.with(fromIp("10.1.0.1"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"cool@example.com\"}"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
	}

	@Test
	void sameEmailFromOtherIp_isStillLimitedByCooldown() throws Exception {
		sendCode("cool2@example.com", "10.1.1.1", 204);
		sendCode("cool2@example.com", "10.1.1.2", 429);
	}

	@Test
	void oneIpSendingToManyEmails_isLimitedPerIp() throws Exception {
		for (int i = 0; i < 4; i++) {
			sendCode("many" + i + "@example.com", "10.1.2.1", 204);
		}
		sendCode("many4@example.com", "10.1.2.1", 429);
		sendCode("many5@example.com", "10.1.2.2", 204);
	}

	@Test
	void repeatedLoginFailures_lockThatEmailAndIpPair() throws Exception {
		register("lock@example.com", "password123", "10.2.0.1");
		for (int i = 0; i < 3; i++) {
			login("lock@example.com", "wrongpass1", "10.2.0.9", 401);
		}
		// 잠긴 뒤에는 올바른 비밀번호여도 거부된다.
		login("lock@example.com", "password123", "10.2.0.9", 429);
		// 다른 IP에서는 영향이 없다(남이 일부러 틀려서 계정을 잠그는 것을 막는다).
		login("lock@example.com", "password123", "10.2.0.10", 200);
	}

	@Test
	void successfulLoginClearsTheFailureCount() throws Exception {
		register("clear@example.com", "password123", "10.3.0.1");
		// 이메일+IP 기준 허용 3회, IP 기준 허용 6회라서 실패 2회씩 두 라운드(IP 합계 4회)는 잠기지 않는다.
		for (int round = 0; round < 2; round++) {
			login("clear@example.com", "wrongpass1", "10.3.0.9", 401);
			login("clear@example.com", "wrongpass1", "10.3.0.9", 401);
			login("clear@example.com", "password123", "10.3.0.9", 200);
		}
	}

	@Test
	void loginFailuresAcrossManyEmails_lockTheIp() throws Exception {
		for (int i = 0; i < 6; i++) {
			login("nobody" + i + "@example.com", "wrongpass1", "10.4.0.1", 401);
		}
		login("nobody9@example.com", "wrongpass1", "10.4.0.1", 429);
		login("nobody9@example.com", "wrongpass1", "10.4.0.2", 401);
	}

	@Test
	void tooManyRequestsResponseCarriesTheMessage() throws Exception {
		sendCode("msg@example.com", "10.5.0.1", 204);
		mockMvc.perform(post("/api/auth/email-verification")
				.with(fromIp("10.5.0.1"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"msg@example.com\"}"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("다시 시도")))
				.andExpect(header().doesNotExist("Authorization"));
	}
}
