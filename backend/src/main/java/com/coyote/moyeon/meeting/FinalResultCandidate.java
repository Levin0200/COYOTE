package com.coyote.moyeon.meeting;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 호스트가 고를 수 있는 최종 장소 후보. 장소 ID 없이 이름만 받은 예전 응답의 후보는 이름 외 값이 null이다.
 * votes는 이 장소를 고르거나 제안한 응답 수이고, reason은 AI가 붙인 짧은 이유(없으면 null)다.
 */
@Embeddable
public record FinalResultCandidate(
		@Column(name = "candidate_name", nullable = false, length = 100) String name,
		@Column(name = "place_id", length = 64) String placeId,
		@Column(name = "address", length = 255) String address,
		@Column(name = "category", length = 100) String category,
		@Column(name = "latitude") Double latitude,
		@Column(name = "longitude") Double longitude,
		@Column(name = "votes", nullable = false) int votes,
		@Column(name = "reason", length = 200) String reason) {

	public SelectedPlace toSelectedPlace() {
		return new SelectedPlace(name, placeId, address, latitude, longitude);
	}

	/** 같은 장소인지: 장소 ID가 있으면 ID로, 없으면 이름으로 비교한다. */
	public boolean matches(String placeId, String name) {
		if (placeId != null && !placeId.isBlank()) {
			return placeId.equals(this.placeId);
		}
		return this.placeId == null && name != null && name.equals(this.name);
	}
}
