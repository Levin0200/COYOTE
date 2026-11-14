package com.coyote.moyeon.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 메일 발송이 실패하면 사용자에게 알리고, 저장된 인증번호는 남지 않는다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailSendFailureTest {

	@Autowired MockMvc mockMvc;
	@Autowired EmailVerificationRepository verificationRepository;
	@MockitoBean EmailSender emailSender;

	@Test
	void aFailedSendReturns503AndKeepsNoCode() throws Exception {
		doThrow(new EmailSendException("Mail server connection failed", null)).when(emailSender)
				.sendVerificationCode(anyString(), anyString());

		mockMvc.perform(post("/api/auth/email-verification").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"mailfail@example.com\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("EMAIL_SEND_FAILED"));

		assertThat(verificationRepository.findByEmail("mailfail@example.com")).isEmpty();
	}
}
