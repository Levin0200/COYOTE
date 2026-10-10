package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.MeetingStatus;
import java.time.Instant;
import java.time.LocalDate;

public record InviteInfoResponse(
		String title,
		String description,
		Instant responseDeadline,
		Instant linkExpirationDate,
		LocalDate candidateStartDate,
		LocalDate candidateEndDate,
		PlacePolicyView placePolicy,
		MeetingStatus status,
		boolean closed,
		long participantCount,
		long responseCount) {
}
