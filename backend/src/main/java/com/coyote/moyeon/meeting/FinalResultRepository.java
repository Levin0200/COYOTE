package com.coyote.moyeon.meeting;

import java.time.Instant;
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

	/** 후보 분석을 시작한 지 오래됐는데 아직 PENDING인 모임(서버가 중간에 멈춘 경우 다시 처리한다). */
	@Query("select f.meeting.id from FinalResult f where f.placeStatus = :status and f.createdAt < :before")
	List<Long> findMeetingIdsByPlaceStatusCreatedBefore(@Param("status") PlaceStatus status,
			@Param("before") Instant before);

	/** 호스트의 선택 시한이 지난 모임. */
	@Query("select f.meeting.id from FinalResult f where f.placeStatus = :status and f.hostDecisionDeadline <= :now")
	List<Long> findMeetingIdsByPlaceStatusDeadlineUntil(@Param("status") PlaceStatus status, @Param("now") Instant now);
}
