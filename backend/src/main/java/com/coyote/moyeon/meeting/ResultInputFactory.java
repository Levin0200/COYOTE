package com.coyote.moyeon.meeting;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 모임의 응답을 결과 생성기 입력으로 바꾼다. 응답은 제출 순서(먼저 낸 순)로 넘긴다. */
@Component
public class ResultInputFactory {

	private final MeetingResponseRepository responseRepository;

	public ResultInputFactory(MeetingResponseRepository responseRepository) {
		this.responseRepository = responseRepository;
	}

	public ResultInput build(Meeting meeting, LocalDate today) {
		List<ResultInput.ResponseSnapshot> snapshots = responseRepository.findAllWithDatesByMeetingId(meeting.getId())
				.stream()
				.sorted(Comparator.comparing(MeetingResponse::getCreatedAt).thenComparing(MeetingResponse::getId))
				.map(r -> new ResultInput.ResponseSnapshot(Set.copyOf(r.getAvailableDates()), r.getPlaceMode(),
						r.getPlacePreference(), r.toPlaceInfo()))
				.toList();
		return new ResultInput(today, meeting.getPlacePolicyMode(), List.copyOf(meeting.getPlaceOptions()), snapshots);
	}
}
