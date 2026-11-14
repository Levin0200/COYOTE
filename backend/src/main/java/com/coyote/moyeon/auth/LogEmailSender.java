package com.coyote.moyeon.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 인증번호를 서버 로그에 출력하는 개발용 구현체. 실제 메일을 보내지 않는다.
 * app.mail.provider=log(기본값)일 때만 쓴다. 실제 서비스(prod)는 app.mail.provider=smtp라서 {@link SmtpEmailSender}를 쓴다.
 */
@Component
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

	@Override
	public void sendVerificationCode(String email, String code) {
		log.info("[DEV] email verification code for {}: {}", email, code);
	}
}
