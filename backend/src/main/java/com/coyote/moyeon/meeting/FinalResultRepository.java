package com.coyote.moyeon.meeting;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinalResultRepository extends JpaRepository<FinalResult, Long> {

	Optional<FinalResult> findByMeetingId(Long meetingId);

	@EntityGraph(attributePaths = "selectedPlaces")
	@Query("select f from FinalResult f join fetch f.meeting m where m.host.id = :hostId")
	List<FinalResult> findAllByHostId(@Param("hostId") Long hostId);
}
