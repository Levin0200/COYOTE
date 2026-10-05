package com.coyote.moyeon.meeting;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingResponseRepository extends JpaRepository<MeetingResponse, Long> {

	Optional<MeetingResponse> findByParticipantId(Long participantId);

	long countByMeetingId(Long meetingId);
}
