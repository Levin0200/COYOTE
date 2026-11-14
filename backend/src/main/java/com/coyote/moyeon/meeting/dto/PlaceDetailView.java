package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.PlaceInfo;
import com.coyote.moyeon.meeting.SelectedPlace;

/** 장소 한 곳의 상세 정보. 장소 ID 없이 이름만 받은 예전 응답은 이름 외 값이 null이다. */
public record PlaceDetailView(String id, String name, String address, String category, Double latitude,
		Double longitude) {

	public static PlaceDetailView from(PlaceInfo place) {
		return place == null ? null
				: new PlaceDetailView(place.id(), place.name(), place.address(), place.category(), place.latitude(),
						place.longitude());
	}

	public static PlaceDetailView from(SelectedPlace place) {
		return new PlaceDetailView(place.placeId(), place.name(), place.address(), null, place.latitude(),
				place.longitude());
	}
}
