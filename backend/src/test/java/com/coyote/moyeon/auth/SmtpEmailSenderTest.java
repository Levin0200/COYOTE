package com.coyote.moyeon.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/** 메일 내용과 설정 검증을 SMTP 서버 없이 확인한다. */
class SmtpEmailSenderTest {

	private final JavaMailSender mailSender = mock(JavaMailSender.class);

	private SmtpEmailSender sender() {
		return new SmtpEmailSender(mailSender, "sender@gmail.com", "모여ON", "sender@gmail.com", "app-password");
	}

	@Test
	void buildsTheMessageWithRecipientSubjectAndCode() throws Exception {
		when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

		sender().sendVerificationCode("guest@example.com", "123456");

		ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
		verify(mailSender).send(captor.capture());
		MimeMessage message = captor.getValue();
		assertThat(message.getRecipients(Message.RecipientType.TO)).hasSize(1);
		assertThat(((InternetAddress) message.getRecipients(Message.RecipientType.TO)[0]).getAddress())
				.isEqualTo("guest@example.com");
		InternetAddress from = (InternetAddress) message.getFrom()[0];
		assertThat(from.getAddress()).isEqualTo("sender@gmail.com");
		assertThat(from.getPersonal()).isEqualTo("모여ON");
		assertThat(message.getSubject()).isEqualTo("[모여ON] 이메일 인증번호");
		String body = (String) message.getContent();
		assertThat(body).contains("123456").contains("5분");
	}

	@Test
	void aSendFailureBecomesAnEmailSendExceptionWithoutTheCode() {
		when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
		doThrow(new MailSendException("Mail server connection failed")).when(mailSender).send(any(MimeMessage.class));

		assertThatThrownBy(() -> sender().sendVerificationCode("guest@example.com", "654321"))
				.isInstanceOf(EmailSendException.class)
				.satisfies(e -> assertThat(e.getMessage()).doesNotContain("654321").doesNotContain("guest@example.com"));
	}

	@Test
	void missingCredentialsStopTheStartup() {
		assertThatThrownBy(() -> new SmtpEmailSender(mailSender, "sender@gmail.com", "모여ON", "", "app-password"))
				.isInstanceOf(IllegalStateException.class).hasMessageContaining("MAIL_USERNAME");
		assertThatThrownBy(() -> new SmtpEmailSender(mailSender, "sender@gmail.com", "모여ON", "sender@gmail.com", ""))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new SmtpEmailSender(mailSender, "", "모여ON", "sender@gmail.com", "app-password"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void theLogAddressIsMasked() {
		assertThat(EmailSendException.mask("abcdef@gmail.com")).isEqualTo("a***@gmail.com");
		assertThat(EmailSendException.mask("no-at-sign")).isEqualTo("***");
	}
}
