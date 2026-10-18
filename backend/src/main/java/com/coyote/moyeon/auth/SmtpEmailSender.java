package com.coyote.moyeon.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP로 인증번호 메일을 실제로 보낸다(app.mail.provider=smtp). 접속 정보는 spring.mail.*, 보내는 사람은 app.mail.from이다.
 *
 * <p>설정이 비어 있으면 서버를 시작하지 않는다(발송이 조용히 실패하는 것을 막기 위해). 본문은 번호와 고정 문구뿐이고
 * 사용자가 입력한 값은 수신 주소에만 쓰이므로 메일 헤더 주입 위험이 없다. 인증번호는 로그에 남기지 않는다.</p>
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

	static final String SUBJECT = "[모여ON] 이메일 인증번호";

	private final JavaMailSender mailSender;
	private final String from;
	private final String fromName;

	public SmtpEmailSender(JavaMailSender mailSender, @Value("${app.mail.from:}") String from,
			@Value("${app.mail.from-name:모여ON}") String fromName, @Value("${spring.mail.username:}") String username,
			@Value("${spring.mail.password:}") String password) {
		if (username.isBlank() || password.isBlank() || from.isBlank()) {
			throw new IllegalStateException(
					"app.mail.provider=smtp이면 MAIL_USERNAME, MAIL_PASSWORD(Gmail은 앱 비밀번호)와 보내는 주소(MAIL_FROM, 비우면 MAIL_USERNAME)가 필요합니다.");
		}
		this.mailSender = mailSender;
		this.from = from;
		this.fromName = fromName;
	}

	@Override
	public void sendVerificationCode(String email, String code) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setFrom(new InternetAddress(from, fromName, "UTF-8"));
			helper.setTo(email);
			helper.setSubject(SUBJECT);
			helper.setText(body(code), false);
			mailSender.send(message);
		} catch (MailException | MessagingException | UnsupportedEncodingException e) {
			// 원인 메시지에는 접속 오류 설명만 들어가고 인증번호나 비밀번호는 없다.
			throw new EmailSendException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
		}
	}

	static String body(String code) {
		return "모여ON 이메일 인증번호입니다.\n\n"
				+ "인증번호: " + code + "\n\n"
				+ "이 번호는 " + AuthService.CODE_VALID_FOR.toMinutes() + "분 동안만 유효합니다.\n"
				+ "본인이 요청하지 않았다면 이 메일을 무시해 주세요.\n";
	}
}
