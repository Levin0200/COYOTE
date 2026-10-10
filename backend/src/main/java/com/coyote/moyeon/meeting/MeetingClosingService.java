package com.coyote.moyeon.meeting;

import com.coyote.moyeon.common.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 모임 마감 처리. 수동 마감, 스케줄러, 결과 조회 시 지연 마감이 모두 이 메서드를 사용한다. */
@Service
public class MeetingClosingService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@PersistenceContext
	private EntityManager entityManager;

	private final MeetingRepository meetingRepository;
	private final MeetingResponseRepository responseRepository;
	private final FinalResultRepository finalResultRepository;
	private final FinalResultGenerator generator;

	public MeetingClosingService(MeetingRepository meetingRepository, MeetingResponseRepository responseRepository,
			FinalResultRepository finalResultRepository, FinalResultGenerator generator) {
		this.meetingRepository = meetingRepository;
		this.responseRepository = responseRepository;
		this.finalResultRepository = finalResultRepository;
		this.generator = generator;
	}

	/**
	 * 아직 열려 있는 모임이면 결과를 계산·저장하고 COMPLETED로 바꾼다. 이미 마감됐으면 아무것도 하지 않는다(멱등).
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
		List<ResultInput.ResponseSnapshot> snapshots = responseRepository.findAllWithDatesByMeetingId(meetingId).stream()
				.map(r -> new ResultInput.ResponseSnapshot(Set.copyOf(r.getAvailableDates()), r.getPlaceMode(),
						r.getPlacePreference()))
				.toList();
		GeneratedResult generated = generator.generate(new ResultInput(LocalDate.now(KST), snapshots));
		finalResultRepository.save(new FinalResult(meeting, generated));
		meeting.complete(manual && now.isBefore(meeting.getResponseDueDate()));
		return true;
	}
}
