package com.coyote.moyeon.common.ratelimit;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/**
 * 서버 한 대 기준의 인메모리 구현(슬라이딩 윈도우). 키별 상태는 ConcurrentHashMap.compute 안에서만 바꿔
 * 동시 요청에도 횟수가 정확하다. 서버를 재시작하면 기록이 초기화된다.
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

	private static final int SWEEP_INTERVAL = 1000;

	private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
	private final AtomicInteger calls = new AtomicInteger();

	private static final class State {
		private final ArrayDeque<Long> hits = new ArrayDeque<>();
		private long blockedUntil;
		private long expiresAt;
	}

	@Override
	public Decision tryAcquire(String key, int max, Duration window) {
		return evaluate(key, max, window, true);
	}

	@Override
	public Decision check(String key, int max, Duration window) {
		return evaluate(key, max, window, false);
	}

	@Override
	public void record(String key, Duration window) {
		long now = System.currentTimeMillis();
		long windowMs = window.toMillis();
		states.compute(key, (k, existing) -> {
			State state = existing == null ? new State() : existing;
			purge(state, now, windowMs);
			state.hits.addLast(now);
			touch(state, windowMs);
			return state;
		});
		sweepIfDue(now);
	}

	@Override
	public void block(String key, Duration duration) {
		long now = System.currentTimeMillis();
		states.compute(key, (k, existing) -> {
			State state = existing == null ? new State() : existing;
			state.blockedUntil = now + duration.toMillis();
			state.expiresAt = Math.max(state.expiresAt, state.blockedUntil);
			return state;
		});
	}

	@Override
	public void reset(String key) {
		states.remove(key);
	}

	private Decision evaluate(String key, int max, Duration window, boolean consume) {
		long now = System.currentTimeMillis();
		long windowMs = window.toMillis();
		Decision[] result = new Decision[1];
		states.compute(key, (k, existing) -> {
			State state = existing == null ? new State() : existing;
			purge(state, now, windowMs);
			if (state.blockedUntil > now) {
				result[0] = Decision.denied(state.blockedUntil - now);
			} else if (state.hits.size() >= max) {
				result[0] = Decision.denied(state.hits.peekFirst() + windowMs - now);
			} else {
				result[0] = Decision.ok();
				if (consume) {
					state.hits.addLast(now);
				}
			}
			touch(state, windowMs);
			return state;
		});
		sweepIfDue(now);
		return result[0];
	}

	private void purge(State state, long now, long windowMs) {
		while (!state.hits.isEmpty() && state.hits.peekFirst() <= now - windowMs) {
			state.hits.pollFirst();
		}
	}

	private void touch(State state, long windowMs) {
		long last = state.hits.isEmpty() ? 0 : state.hits.peekLast();
		state.expiresAt = Math.max(state.blockedUntil, last + windowMs);
	}

	/** 일정 호출마다 더 이상 의미 없는 키를 지워 메모리가 계속 늘지 않게 한다. */
	private void sweepIfDue(long now) {
		if (calls.incrementAndGet() % SWEEP_INTERVAL != 0) {
			return;
		}
		for (String key : states.keySet()) {
			states.computeIfPresent(key, (k, state) -> state.expiresAt < now ? null : state);
		}
	}
}
