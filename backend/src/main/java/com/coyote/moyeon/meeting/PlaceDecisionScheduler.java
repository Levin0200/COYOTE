package com.coyote.moyeon.meeting;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 장소 결정의 뒷정리. 호스트가 시한 안에 고르지 않은 모임을 자동으로 확정하고, 후보 분석 중에 서버가 멈춰 PENDING으로
 * 남은 모임을 다시 처리한다. 서버 시작 시에도 한 번 실행한다.
 */
@Component
public class PlaceDecisionScheduler {

	private static final Logger log = LoggerFactory.getLogger(PlaceDecisionScheduler.class);
	/** 이 시간이 지나도 PENDING이면 멈춘 것으로 보고 다시 처리한다. 정상 처리는 보통 수 초 안에 끝난다. */
	private static final Duration STUCK_AFTER = Duration.ofMinutes(2);

	private final FinalResultRepository finalResultRepository;
	private final PlaceDecisionService placeDecisionService;
	private final boolean enabled;

	public PlaceDecisionScheduler(FinalResultRepository finalResultRepository, PlaceDecisionService placeDecisionService,
			@Value("${app.scheduling.enabled:true}") boolean enabled) {
		this.finalResultRepository = finalResultRepository;
		this.placeDecisionService = placeDecisionService;
		this.enabled = enabled;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onStartup() {
		if (enabled) {
			run();
		}
	}

	@Scheduled(fixedDelayString = "${app.scheduling.interval-ms:60000}",
			initialDelayString = "${app.scheduling.interval-ms:60000}")
	public void scheduledRun() {
		if (enabled) {
			run();
		}
	}

	/** 처리한 모임 수(자동 확정 + 다시 처리)를 돌려준다. 한 건이 실패해도 나머지는 계속한다. */
	public int run() {
		int handled = 0;
		Instant now = Instant.now();
		List<Long> stuck = finalResultRepository.findMeetingIdsByPlaceStatusCreatedBefore(PlaceStatus.PENDING,
				now.minus(STUCK_AFTER));
		for (Long id : stuck) {
			try {
				placeDecisionService.completePending(id);
				handled++;
			} catch (RuntimeException e) {
				log.error("Failed to retry place candidates for meeting {}", id, e);
			}
		}
		List<Long> overdue = finalResultRepository.findMeetingIdsByPlaceStatusDeadlineUntil(PlaceStatus.AWAITING_HOST, now);
		for (Long id : overdue) {
			try {
				if (placeDecisionService.autoPick(id)) {
					handled++;
				}
			} catch (RuntimeException e) {
				log.error("Failed to auto-pick the place for meeting {}", id, e);
			}
		}
		return handled;
	}
}
