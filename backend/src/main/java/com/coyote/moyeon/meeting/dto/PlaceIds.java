package com.coyote.moyeon.meeting.dto;

import java.util.LinkedHashSet;
import java.util.Set;

/** 요청 DTO에서 장소 ID를 모으는 도우미. */
final class PlaceIds {

	private PlaceIds() {
	}

	static Set<String> collect(String chosenPlaceId, PlacePolicyData policy) {
		Set<String> ids = new LinkedHashSet<>();
		if (chosenPlaceId != null && !chosenPlaceId.isBlank()) {
			ids.add(chosenPlaceId.trim());
		}
		if (policy != null && policy.options() != null) {
			for (PlaceOptionData option : policy.options()) {
				if (option != null && option.placeId() != null && !option.placeId().isBlank()) {
					ids.add(option.placeId().trim());
				}
			}
		}
		return ids;
	}
}
