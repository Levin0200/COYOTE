package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.ParticipantRole;
import java.time.LocalDate;
import java.util.List;

/** 마감된 모임의 투표 상세. 호스트에게만, 마감 후에만 공개한다. 응답하지 않은 멤버는 dates가 빈 목록이다. */
public record MeetingVotesResponse(
		long participantCount,
		long responseCount,
		List<DateCount> dateCounts,
		List<MemberVote> members) {

	public record DateCount(LocalDate date, long count) {
	}

	public record MemberVote(String nickname, ParticipantRole role, List<LocalDate> dates) {
	}
}
