package com.coyote.moyeon.meeting;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 결과 생성기에 넘기는 입력. 닉네임 같은 식별 정보는 넘기지 않는다.
 * responses는 응답을 낸 순서대로이고(합의에서 제안 순서로 쓴다), options는 호스트가 정한 추천 후보 순서다.
 */
public record ResultInput(LocalDate today, PlacePolicyMode policyMode, List<PlaceOption> options,
		List<ResponseSnapshot> responses) {

	public record ResponseSnapshot(Set<LocalDate> dates, PlaceMode placeMode, String placePreference, PlaceInfo place) {
	}
}
