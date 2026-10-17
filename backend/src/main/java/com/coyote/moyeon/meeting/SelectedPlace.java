package com.coyote.moyeon.meeting;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** 최종 결과로 정해진 장소. 이름 외의 값은 장소 ID 없이 이름만 받은 예전 응답에서는 null이다. */
@Embeddable
public record SelectedPlace(
		@Column(name = "place_name", nullable = false, length = 100) String name,
		@Column(name = "place_id", length = 64) String placeId,
		@Column(name = "address", length = 255) String address,
		@Column(name = "latitude") Double latitude,
		@Column(name = "longitude") Double longitude) {

	public static SelectedPlace ofName(String name) {
		return new SelectedPlace(name, null, null, null, null);
	}
}
