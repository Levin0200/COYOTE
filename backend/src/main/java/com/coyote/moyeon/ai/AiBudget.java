package com.coyote.moyeon.ai;

import com.coyote.moyeon.common.ratelimit.RateLimiter;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** 서버 전체의 하루 AI 호출 횟수 상한(비용 상한). 넘으면 규칙 기반 방식으로만 동작한다. */
@Component
public class AiBudget {

	private final RateLimiter rateLimiter;
	private final AiProperties properties;

	public AiBudget(RateLimiter rateLimiter, AiProperties properties) {
		this.rateLimiter = rateLimiter;
		this.properties = properties;
	}

	/** 오늘 남은 호출이 있으면 1회를 쓰고 true. */
	public boolean tryAcquire() {
		return rateLimiter.tryAcquire("ai-daily", properties.getDailyLimit(), Duration.ofHours(24)).allowed();
	}
}
