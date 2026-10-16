package com.coyote.moyeon.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** AI를 쓰지 않을 때(app.ai.provider=none)의 구현체. 항상 사용 불가로 보고하므로 규칙 기반 방식만 쓰인다. */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "none")
public class NoopLlmClient implements LlmClient {

	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public String complete(String system, String user, int maxOutputTokens) {
		throw new LlmException("AI를 사용하지 않도록 설정되어 있습니다.");
	}
}
