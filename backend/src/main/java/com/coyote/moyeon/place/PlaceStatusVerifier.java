package com.coyote.moyeon.place;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 검색 제공자가 준 장소의 현재 영업 상태를 보조 데이터로 확인한다.
 * 검증 자료에 없는 장소는 UNKNOWN으로 두어 원래 검색 결과를 막지 않는다.
 */
public interface PlaceStatusVerifier {

	Status statusOf(Place place);

	default Map<Place, Status> statusesOf(Collection<Place> places) {
		Map<Place, Status> result = new LinkedHashMap<>();
		for (Place place : places) {
			result.put(place, statusOf(place));
		}
		return Map.copyOf(result);
	}

	enum Status {
		OPEN,
		CLOSED,
		UNKNOWN
	}

	static PlaceStatusVerifier permissive() {
		return place -> Status.UNKNOWN;
	}
}
