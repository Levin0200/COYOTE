package com.coyote.moyeon.meeting;

import com.coyote.moyeon.place.Place;

/**
 * 모임에 저장하는 장소 정보. 장소 ID(공공데이터는 상가업소번호)와 공식 이름·주소·업종·좌표를 담는다.
 * 이름만 알고 있는 예전 방식의 응답에서는 id와 좌표가 null일 수 있다.
 */
public record PlaceInfo(String id, String name, String address, String category, Double latitude, Double longitude) {

	private static final int NAME_MAX = 100;
	private static final int ADDRESS_MAX = 255;
	private static final int CATEGORY_MAX = 100;

	/** 검색·조회 결과를 저장 형태로 바꾼다. 컬럼 길이를 넘는 값은 잘라 낸다. */
	public static PlaceInfo from(Place place) {
		return new PlaceInfo(place.id(), cut(place.name(), NAME_MAX), cut(place.address(), ADDRESS_MAX),
				cut(place.categoryName(), CATEGORY_MAX), place.latitude(), place.longitude());
	}

	private static String cut(String value, int maxCodePoints) {
		if (value == null || value.codePointCount(0, value.length()) <= maxCodePoints) {
			return value;
		}
		return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
	}
}
