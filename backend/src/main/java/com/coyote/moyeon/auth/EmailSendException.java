package com.coyote.moyeon.auth;

/** 메일 발송에 실패했다(서버 연결·인증 실패, 주소 거부 등). 메시지에는 인증번호와 전체 주소를 넣지 않는다. */
public class EmailSendException extends RuntimeException {

	public EmailSendException(String message, Throwable cause) {
		super(message, cause);
	}

	/** 로그용으로 주소를 가린다. "abcdef@gmail.com" -> "a***@gmail.com" */
	static String mask(String email) {
		int at = email == null ? -1 : email.indexOf('@');
		if (at <= 0) {
			return "***";
		}
		return email.charAt(0) + "***" + email.substring(at);
	}
}
