package com.coyote.moyeon.meeting;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 마감 시각이 지났는데 아직 열려 있는 모임을 주기적으로 마감한다. 서버 시작 시에도 한 번 실행한다. */
@Component
public class MeetingCloseScheduler {

	private static final Logger log = LoggerFactory.getLogger(MeetingCloseScheduler.class);

	private final MeetingRepository meetingRepository;
	private final MeetingClosingService closingService;
	private final boolean enabled;

	public MeetingCloseScheduler(MeetingRepository meetingRepository, MeetingClosingService closingService,
			@Value("${app.scheduling.enabled:true}") boolean enabled) {
		this.meetingRepository = meetingRepository;
		this.closingService = closingService;
		this.enabled = enabled;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onStartup() {
		if (enabled) {
			closeDueMeetings();
		}
	}

	@Scheduled(fixedDelayString = "${app.scheduling.interval-ms:60000}",
			initialDelayString = "${app.scheduling.interval-ms:60000}")
	public void scheduledRun() {
		if (enabled) {
			closeDueMeetings();
		}
	}

	/** 한 건이 실패해도 나머지는 계속 처리한다. 마감한 모임 수를 돌려준다. */
	public int closeDueMeetings() {
		List<Long> dueIds = meetingRepository.findDueMeetingIds(MeetingStatus.COLLECTING, Instant.now());
		int closed = 0;
		for (Long id : dueIds) {
			try {
				if (closingService.closeIfOpen(id, false)) {
					closed++;
				}
			} catch (RuntimeException e) {
				log.error("Failed to close meeting {}", id, e);
			}
		}
		return closed;
	}
}
