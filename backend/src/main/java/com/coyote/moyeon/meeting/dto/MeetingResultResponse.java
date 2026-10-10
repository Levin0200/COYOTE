package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.DecisionType;
import com.coyote.moyeon.meeting.FinalResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 마감 후 공개되는 집계 결과. 개별 응답과 닉네임은 포함하지 않는다. */
public record MeetingResultResponse(
		LocalDate selectedDate,
		List<String> selectedPlaces,
		String selectedPlace,
		DecisionType decisionType,
		String reason,
		int dateVoteCount,
		int placeVoteCount,
		int totalResponses,
		Instant createdAt) {

	private static final String UNDECIDED_PLACE = "추후 협의";

	public static MeetingResultResponse from(FinalResult result) {
		List<String> places = List.copyOf(result.getSelectedPlaces());
		return new MeetingResultResponse(result.getSelectedDate(), places,
				places.isEmpty() ? UNDECIDED_PLACE : String.join(" · ", places), result.getDecisionType(),
				result.getReason(), result.getDateVoteCount(), result.getPlaceVoteCount(),
				result.getTotalResponses(), result.getCreatedAt());
	}
}
