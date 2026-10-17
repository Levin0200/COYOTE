package com.coyote.moyeon.meeting;

import com.coyote.moyeon.common.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 모임 마감 처리. 수동 마감, 스케줄러, 결과 조회 시 지연 마감이 모두 이 메서드를 사용한다.
 *
 * <p>마감은 두 단계다. 1단계(여기, 잠금 안)는 날짜와 바로 정해지는 장소를 저장한다. 장소 후보를 만들어야 하는
 * 경우(건너뛰기 모임에서 제안이 흩어졌을 때)만 모임을 CLOSED로 두고, 커밋 후 외부 API·AI가 필요한 2단계를
 * {@link PlaceDecisionService}가 잠금 밖에서 마무리한다.</p>
 */
@Service
public class MeetingClosingService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@PersistenceContext
	private EntityManager entityManager;

	private final MeetingRepository meetingRepository;
	private final FinalResultRepository finalResultRepository;
	private final FinalResultGenerator generator;
	private final ResultInputFactory inputFactory;
	private final PlaceDecisionService placeDecisionService;
	private final Duration hostWindow;

	public MeetingClosingService(MeetingRepository meetingRepository, FinalResultRepository finalResultRepository,
			FinalResultGenerator generator, ResultInputFactory inputFactory, PlaceDecisionService placeDecisionService,
			@Value("${app.place-decision.host-window-hours:24}") long hostWindowHours) {
		this.meetingRepository = meetingRepository;
		this.finalResultRepository = finalResultRepository;
		this.generator = generator;
		this.inputFactory = inputFactory;
		this.placeDecisionService = placeDecisionService;
		this.hostWindow = Duration.ofHours(hostWindowHours);
	}

	/**
	 * 아직 열려 있는 모임이면 결과를 계산·저장하고 마감한다. 이미 마감됐으면 아무것도 하지 않는다(멱등).
	 *
	 * @return 이번 호출에서 실제로 마감했으면 true
	 */
	@Transactional
	public boolean closeIfOpen(Long meetingId, boolean manual) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND", "모임을 찾을 수 없습니다."));
		// 같은 세션에서 이미 읽은 값이 오래됐을 수 있으므로 잠금을 걸면서 최신 상태로 다시 읽는다.
		entityManager.refresh(meeting, LockModeType.PESSIMISTIC_WRITE);
		if (meeting.getStatus() != MeetingStatus.COLLECTING) {
			return false;
		}
		Instant now = Instant.now();
		GeneratedResult generated = generator.generate(inputFactory.build(meeting, LocalDate.now(KST)));
		FinalResult result = new FinalResult(meeting, generated);
		if (generated.placeStatus() == PlaceStatus.AWAITING_HOST) {
			result.startHostWindow(now.plus(hostWindow));
		}
		finalResultRepository.save(result);

		boolean manuallyClosed = manual && now.isBefore(meeting.getResponseDueDate());
		if (generated.placeStatus() == PlaceStatus.PENDING) {
			meeting.close(manuallyClosed);
			runAfterCommit(() -> placeDecisionService.completePendingAsync(meetingId));
		} else {
			meeting.complete(manuallyClosed);
		}
		return true;
	}

	private static void runAfterCommit(Runnable task) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			task.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				task.run();
			}
		});
	}
}
