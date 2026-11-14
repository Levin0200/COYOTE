package com.coyote.moyeon.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

	private final InMemoryRateLimiter limiter = new InMemoryRateLimiter();

	@Test
	void allowsUpToMaxThenDenies() {
		Duration window = Duration.ofMinutes(1);
		for (int i = 0; i < 3; i++) {
			assertThat(limiter.tryAcquire("k", 3, window).allowed()).isTrue();
		}
		RateLimiter.Decision denied = limiter.tryAcquire("k", 3, window);
		assertThat(denied.allowed()).isFalse();
		assertThat(denied.retryAfterSeconds()).isBetween(1L, 60L);
	}

	@Test
	void keysAreIndependent() {
		assertThat(limiter.tryAcquire("a", 1, Duration.ofMinutes(1)).allowed()).isTrue();
		assertThat(limiter.tryAcquire("a", 1, Duration.ofMinutes(1)).allowed()).isFalse();
		assertThat(limiter.tryAcquire("b", 1, Duration.ofMinutes(1)).allowed()).isTrue();
	}

	@Test
	void allowsAgainAfterWindowPasses() throws Exception {
		Duration window = Duration.ofMillis(100);
		assertThat(limiter.tryAcquire("k", 1, window).allowed()).isTrue();
		assertThat(limiter.tryAcquire("k", 1, window).allowed()).isFalse();
		Thread.sleep(150);
		assertThat(limiter.tryAcquire("k", 1, window).allowed()).isTrue();
	}

	@Test
	void checkDoesNotConsume() {
		Duration window = Duration.ofMinutes(1);
		for (int i = 0; i < 10; i++) {
			assertThat(limiter.check("k", 1, window).allowed()).isTrue();
		}
		limiter.record("k", window);
		assertThat(limiter.check("k", 1, window).allowed()).isFalse();
	}

	@Test
	void blockDeniesUntilItEndsAndResetClearsIt() throws Exception {
		Duration window = Duration.ofMinutes(1);
		limiter.block("k", Duration.ofMillis(100));
		assertThat(limiter.check("k", 5, window).allowed()).isFalse();
		Thread.sleep(150);
		assertThat(limiter.check("k", 5, window).allowed()).isTrue();

		limiter.block("k", Duration.ofMinutes(1));
		limiter.reset("k");
		assertThat(limiter.check("k", 5, window).allowed()).isTrue();
	}

	@Test
	void concurrentRequestsNeverExceedMax() throws Exception {
		int max = 10;
		AtomicInteger allowed = new AtomicInteger();
		ExecutorService pool = Executors.newFixedThreadPool(16);
		try {
			java.util.List<Future<?>> futures = new java.util.ArrayList<>();
			for (int i = 0; i < 200; i++) {
				futures.add(pool.submit(() -> {
					if (limiter.tryAcquire("shared", max, Duration.ofMinutes(1)).allowed()) {
						allowed.incrementAndGet();
					}
				}));
			}
			for (Future<?> f : futures) {
				f.get();
			}
		} finally {
			pool.shutdownNow();
		}
		assertThat(allowed.get()).isEqualTo(max);
	}
}
