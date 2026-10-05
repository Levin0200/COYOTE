package com.coyote.moyeon.meeting;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

	Optional<Participant> findByMeetingIdAndGuestId(Long meetingId, String guestId);

	Optional<Participant> findByMeetingIdAndRole(Long meetingId, ParticipantRole role);

	long countByMeetingId(Long meetingId);
}
