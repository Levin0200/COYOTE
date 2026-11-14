package com.coyote.moyeon.common.ratelimit;

import com.coyote.moyeon.common.exception.BusinessException;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 서비스 코드에서 제한을 적용할 때 쓰는 도우미. 초과하면 429 TOO_MANY_REQUESTS 예외를 던진다. */
@Component
public class RateLimitGuard {

	private final RateLimiter rateLimiter;

	public RateLimitGuard(RateLimiter rateLimiter) {
		this.rateLimiter = rateLimiter;
	}

	public void assertAllowed(RateLimiter.Decision decision) {
		if (!decision.allowed()) {
			throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS",
					"요청이 너무 많습니다. " + decision.retryAfterSeconds() + "초 후 다시 시도해 주세요.");
		}
	}

	/** 실패 횟수를 늘리지 않고, 이미 잠겨 있으면 예외를 던진다. */
	public void assertNotLocked(String key, int maxFailures, Duration lock) {
		assertAllowed(rateLimiter.check(key, maxFailures, lock));
	}

	/** 실패를 기록하고, 허용 횟수에 도달하면 lock 시간 동안 이 키를 잠근다. */
	public void recordFailure(String key, int maxFailures, Duration lock) {
		rateLimiter.record(key, lock);
		if (!rateLimiter.check(key, maxFailures, lock).allowed()) {
			rateLimiter.block(key, lock);
		}
	}

	public void clear(String key) {
		rateLimiter.reset(key);
	}
}
