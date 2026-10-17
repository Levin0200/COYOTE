package com.coyote.moyeon.meeting;

import java.time.LocalDate;
import java.util.List;

/**
 * 결과 생성기가 만든 최종 결과. 저장 전 값 객체다.
 * placeStatus가 DECIDED면 selectedPlaces가 최종 장소이고, AWAITING_HOST면 candidates 중에서 호스트가 고른다.
 * PENDING이면 후보는 마감 직후 따로 만든다.
 */
public record GeneratedResult(
		LocalDate selectedDate,
		int dateVoteCount,
		List<SelectedPlace> selectedPlaces,
		int placeVoteCount,
		int totalResponses,
		DecisionType decisionType,
		String reason,
		PlaceStatus placeStatus,
		List<FinalResultCandidate> candidates) {
}
