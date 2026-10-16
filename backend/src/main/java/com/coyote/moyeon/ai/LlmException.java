package com.coyote.moyeon.ai;

/** AI 호출 실패. 메시지에는 요청 주소나 인증키를 담지 않는다. */
public class LlmException extends RuntimeException {

	public LlmException(String message) {
		super(message);
	}
}
