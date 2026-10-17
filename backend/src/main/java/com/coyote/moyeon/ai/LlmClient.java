package com.coyote.moyeon.ai;

/**
 * AI(LLM) 호출부. 서버 코드는 이 인터페이스만 쓰므로 서비스(OpenRouter 등)를 바꿀 때 구현체만 교체한다.
 * 호출이 실패하면 {@link LlmException}을 던지고, 호출하는 쪽은 규칙 기반 방식으로 대체한다.
 */
public interface LlmClient {

	/** 키가 설정되어 있는 등 호출할 수 있는 상태인가. */
	boolean isAvailable();

	/** system 지시와 user 입력으로 한 번 호출하고 모델의 답변 본문을 돌려준다. */
	String complete(String system, String user, int maxOutputTokens);
}
