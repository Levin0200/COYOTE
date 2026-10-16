package com.coyote.moyeon.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 인증번호를 서버 로그에 출력하는 개발용 구현체. 실제 메일을 보내지 않는다.
 * 로컬 MariaDB 테스트용 mariadb 프로필에서도 쓰고, 실제 서비스(prod)에서는 쓰지 않는다.
 */
@Component
@Profile({"dev", "test", "mariadb"})
public class LogEmailSender implements EmailSender {

	private static final Logger log = LoggerFactory.getLogger(LogEmailSender.class);

	@Override
	public void sendVerificationCode(String email, String code) {
		log.info("[DEV] email verification code for {}: {}", email, code);
	}
}
