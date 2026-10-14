package com.coyote.moyeon.common.ratelimit;

import java.time.Duration;

/**
 * 키(이메일, IP 등) 단위 요청 횟수 제한. 지금은 인메모리 구현만 있고,
 * 서버를 여러 대로 늘리거나 재시작 후에도 제한을 유지해야 하면 DB 구현으로 교체한다.
 * 같은 키에는 항상 같은 window를 쓴다(키 이름에 용도를 넣는다).
 */
public interface RateLimiter {

	/** window 안에 max회까지 허용한다. 허용되면 사용 횟수를 1 늘린다(확인과 기록이 한 번에 처리된다). */
	Decision tryAcquire(String key, int max, Duration window);

	/** 사용 횟수를 늘리지 않고 허용 여부만 확인한다. */
	Decision check(String key, int max, Duration window);

	/** 사용 횟수를 1 늘린다(실패 횟수 기록용). */
	void record(String key, Duration window);

	/** 지정한 시간 동안 이 키를 차단한다. */
	void block(String key, Duration duration);

	/** 이 키의 기록과 차단을 모두 지운다. */
	void reset(String key);

	record Decision(boolean allowed, long retryAfterSeconds) {

		public static Decision ok() {
			return new Decision(true, 0);
		}

		public static Decision denied(long retryAfterMillis) {
			return new Decision(false, Math.max(1, (retryAfterMillis + 999) / 1000));
		}
	}
}
