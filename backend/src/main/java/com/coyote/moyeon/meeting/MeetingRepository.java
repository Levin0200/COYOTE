package com.coyote.moyeon.meeting;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

	Optional<Meeting> findByInviteToken(String inviteToken);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Meeting m where m.id = :id")
	Optional<Meeting> findByIdForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from Meeting m where m.inviteToken = :inviteToken")
	Optional<Meeting> findByInviteTokenForUpdate(@Param("inviteToken") String inviteToken);
}
