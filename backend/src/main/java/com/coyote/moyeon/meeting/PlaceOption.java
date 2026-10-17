package com.coyote.moyeon.meeting;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 호스트가 정한 추천 후보 한 곳. 장소 ID 없이 이름만 받은 예전 방식의 후보는 placeId와 좌표가 null이다.
 * distance는 화면에 보여 주는 거리 문구("0.5km")이고 서버는 해석하지 않는다.
 */
@Embeddable
public record PlaceOption(
		@Column(name = "option_name", nullable = false, length = 100) String name,
		@Column(name = "distance", length = 20) String distance,
		@Column(name = "place_id", length = 64) String placeId,
		@Column(name = "address", length = 255) String address,
		@Column(name = "latitude") Double latitude,
		@Column(name = "longitude") Double longitude,
		@Column(name = "category", length = 100) String category) {

	/** 이름과 거리만 있는 후보(장소 ID 없음). */
	public PlaceOption(String name, String distance) {
		this(name, distance, null, null, null, null, null);
	}

	public static PlaceOption of(PlaceInfo place, String distance) {
		return new PlaceOption(place.name(), distance, place.id(), place.address(), place.latitude(),
				place.longitude(), place.category());
	}

	/** 좌표가 있는 후보면 장소 정보로 돌려준다. 이름만 있는 후보는 null. */
	public PlaceInfo toPlaceInfo() {
		return placeId == null && latitude == null ? null
				: new PlaceInfo(placeId, name, address, category, latitude, longitude);
	}
}
