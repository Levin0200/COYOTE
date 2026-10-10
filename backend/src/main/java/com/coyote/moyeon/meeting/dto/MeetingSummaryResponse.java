package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.DecisionType;
import com.coyote.moyeon.meeting.FinalResult;
import com.coyote.moyeon.meeting.MeetingStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 내 모임 목록의 한 항목. 결과가 있으면 요약만 포함한다. */
public record MeetingSummaryResponse(
		Long meetingId,
		String inviteToken,
		String title,
		String description,
		Instant responseDeadline,
		MeetingStatus status,
		boolean closed,
		long participantCount,
		long responseCount,
		Instant createdAt,
		ResultSummary result) {

	public record ResultSummary(
			LocalDate selectedDate,
			List<String> selectedPlaces,
			String selectedPlace,
			DecisionType decisionType) {

		public static ResultSummary from(FinalResult result) {
			MeetingResultResponse full = MeetingResultResponse.from(result);
			return new ResultSummary(full.selectedDate(), full.selectedPlaces(), full.selectedPlace(),
					full.decisionType());
		}
	}
}
