package com.coyote.moyeon.meeting;

import java.time.LocalDate;
import java.util.List;

/** 결과 생성기가 만든 최종 결과. 저장 전 값 객체다. */
public record GeneratedResult(
		LocalDate selectedDate,
		int dateVoteCount,
		List<String> selectedPlaces,
		int placeVoteCount,
		int totalResponses,
		DecisionType decisionType,
		String reason) {
}
