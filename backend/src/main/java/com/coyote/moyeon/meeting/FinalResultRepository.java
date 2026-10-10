package com.coyote.moyeon.meeting;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinalResultRepository extends JpaRepository<FinalResult, Long> {

	Optional<FinalResult> findByMeetingId(Long meetingId);
}
