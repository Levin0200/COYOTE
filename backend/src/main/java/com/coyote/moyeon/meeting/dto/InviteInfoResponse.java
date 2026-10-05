package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.MeetingStatus;
import java.time.Instant;

public record InviteInfoResponse(
		String title,
		String description,
		Instant responseDueDate,
		Instant linkExpirationDate,
		MeetingStatus status,
		boolean closed,
		long participantCount,
		long responseCount) {
}
