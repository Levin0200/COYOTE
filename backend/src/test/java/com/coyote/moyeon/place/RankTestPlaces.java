package com.coyote.moyeon.place;

import java.util.ArrayList;
import java.util.List;

/** 고르기 테스트용 장소 만들기. */
final class RankTestPlaces {

	private RankTestPlaces() {
	}

	static Place place(int n, String smallCode, String smallName, double distance) {
		return new Place("p" + n, "상호" + n, "I2", "I2xx", smallCode, "음식 > 중분류 > " + smallName, "서울특별시 강남구 가로 " + n,
				37.5 + n * 0.0001, 127.0 + n * 0.0001, distance);
	}

	/** 가까운 순서의 후보 count곳. 업종은 categories를 돌려 가며 쓴다. */
	static List<Place> pool(int count, String... categories) {
		List<Place> places = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			String code = categories[(i - 1) % categories.length];
			places.add(place(i, code, "업종" + code, 100.0 * i));
		}
		return places;
	}
}
