package com.coyote.moyeon.auth;

public interface EmailSender {

	void sendVerificationCode(String email, String code);
}
