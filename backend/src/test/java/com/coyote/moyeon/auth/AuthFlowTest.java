package com.coyote.moyeon.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	EmailVerificationRepository verificationRepository;

	private void sendCode(String email) throws Exception {
		mockMvc.perform(post("/api/auth/email-verification")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\"}"))
				.andExpect(status().isNoContent());
	}

	private String codeFor(String email) {
		return verificationRepository.findByEmail(email.toLowerCase()).orElseThrow().getCode();
	}

	private void confirm(String email, String code, int expectedStatus) throws Exception {
		mockMvc.perform(post("/api/auth/email-verification/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
				.andExpect(status().is(expectedStatus));
	}

	private String signupBody(String email, String password) {
		return """
				{"email":"%s","name":"김모여","password":"%s","address":"인천광역시 동구",
				 "serviceAgree":true,"privacyAgree":true}
				""".formatted(email, password);
	}

	private void register(String email, String password) throws Exception {
		sendCode(email);
		confirm(email, codeFor(email), 204);
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupBody(email, password)))
				.andExpect(status().isCreated());
	}

	private String login(String email, String password) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	@Test
	void fullFlow_signup_login_me_update() throws Exception {
		String email = "flow@example.com";
		register(email, "password123");
		String token = login(email, "password123");

		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.name").value("김모여"))
				.andExpect(jsonPath("$.address").value("인천광역시 동구"))
				.andExpect(jsonPath("$.password").doesNotExist());

		mockMvc.perform(patch("/api/users/me").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"이모여\",\"address\":\"\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("이모여"))
				.andExpect(jsonPath("$.address").doesNotExist());
	}

	@Test
	void emailIsNormalizedToLowerCase() throws Exception {
		register("Mixed@Example.com", "password123");
		login("mixed@example.com", "password123");
	}

	@Test
	void signupWithoutVerification_isRejected() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupBody("noverify@example.com", "password123")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
	}

	@Test
	void signupWithoutAgreement_isRejected() throws Exception {
		String email = "noagree@example.com";
		sendCode(email);
		confirm(email, codeFor(email), 204);
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","name":"a","password":"password123","serviceAgree":true,"privacyAgree":false}
						""".formatted(email)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void shortPassword_isRejected() throws Exception {
		String email = "short@example.com";
		sendCode(email);
		confirm(email, codeFor(email), 204);
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupBody(email, "short")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void wrongCodeFiveTimes_invalidatesTheCode() throws Exception {
		String email = "bruteforce@example.com";
		sendCode(email);
		String realCode = codeFor(email);
		String wrongCode = realCode.equals("000000") ? "000001" : "000000";
		for (int i = 0; i < 5; i++) {
			confirm(email, wrongCode, 400);
		}
		confirm(email, realCode, 400);
	}

	@Test
	void resendingCode_invalidatesThePreviousOne() throws Exception {
		String email = "resend@example.com";
		sendCode(email);
		String first = codeFor(email);
		String second;
		do {
			sendCode(email);
			second = codeFor(email);
		} while (second.equals(first));
		confirm(email, first, 400);
		confirm(email, second, 204);
	}

	@Test
	void sendingCodeToRegisteredEmail_isConflict() throws Exception {
		register("dup@example.com", "password123");
		mockMvc.perform(post("/api/auth/email-verification")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"dup@example.com\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
	}

	@Test
	void loginWithWrongPasswordOrUnknownEmail_isUnauthorizedWithSameMessage() throws Exception {
		register("login@example.com", "password123");
		for (String[] c : new String[][] { { "login@example.com", "wrongpass1" }, { "nobody@example.com", "password123" } }) {
			mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"" + c[0] + "\",\"password\":\"" + c[1] + "\"}"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
		}
	}

	@Test
	void meWithoutOrWithBadToken_isUnauthorized() throws Exception {
		mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not.a.token"))
				.andExpect(status().isUnauthorized());
	}
}
