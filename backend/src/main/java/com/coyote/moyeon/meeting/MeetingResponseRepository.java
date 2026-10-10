package com.coyote.moyeon.meeting;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingResponseRepository extends JpaRepository<MeetingResponse, Long> {

	Optional<MeetingResponse> findByParticipantId(Long participantId);

	long countByMeetingId(Long meetingId);

	@EntityGraph(attributePaths = "availableDates")
	@Query("select r from MeetingResponse r where r.meeting.id = :meetingId")
	List<MeetingResponse> findAllWithDatesByMeetingId(@Param("meetingId") Long meetingId);

	long countByMeetingIdAndParticipantRole(Long meetingId, ParticipantRole role);
}
