package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.MeetingStatus;
import java.time.Instant;

public record MeetingDetailResponse(
		Long id,
		String inviteToken,
		String title,
		String description,
		Instant responseDueDate,
		Instant linkExpirationDate,
		MeetingStatus status,
		boolean closed,
		long participantCount,
		long responseCount,
		Instant createdAt,
		MyResponseView myResponse) {
}
