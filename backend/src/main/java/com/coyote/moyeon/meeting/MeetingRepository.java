package com.coyote.moyeon.meeting;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	Optional<Meeting> findByInviteToken(String inviteToken);
}
