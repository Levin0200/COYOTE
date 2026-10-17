package com.coyote.moyeon.place;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 장소 추천 요청. 호스트가 모임을 만드는 중에 호출하므로 모임은 아직 없고, 화면에 입력한 제목·설명을 함께 보낸다
 * (AI가 모임 성격을 이해하는 데 쓴다). 좌표는 지도에서 고른 핀 위치다.
 */
public record RecommendRequest(
		@NotNull Double latitude,
		@NotNull Double longitude,
		@Min(PlaceSearchService.MIN_RADIUS_METERS) @Max(PlaceSearchService.MAX_RADIUS_METERS) Integer radiusMeters,
		@Size(max = 100) String keyword,
		@Size(max = 100) String title,
		@Size(max = 1000) String description,
		@Size(max = 100) String region,
		@Min(1) @Max(100) Integer participantCount) {

	public int radiusOrDefault() {
		return radiusMeters == null ? 1000 : radiusMeters;
	}

	public int participantCountOrDefault() {
		return participantCount == null ? 1 : participantCount;
	}
}
