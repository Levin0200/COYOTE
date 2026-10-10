package com.coyote.moyeon.meeting;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

	Optional<Participant> findByMeetingIdAndGuestId(Long meetingId, String guestId);

	Optional<Participant> findByMeetingIdAndRole(Long meetingId, ParticipantRole role);

	long countByMeetingId(Long meetingId);

	@Query("select new com.coyote.moyeon.meeting.MeetingCount(p.meeting.id, count(p)) from Participant p "
			+ "where p.meeting.host.id = :hostId group by p.meeting.id")
	List<MeetingCount> countByHost(@Param("hostId") Long hostId);
}
