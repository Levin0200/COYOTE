package com.coyote.moyeon.place.consensus;

import com.coyote.moyeon.place.GeoMath;
import com.coyote.moyeon.place.Place;

/** 합의 알고리즘 테스트용 장소 만들기. 기준점에서 방위각과 거리로 위치를 정해 거리 조건을 정확히 맞춘다. */
final class TestPlaces {

	/** 강남역 근처 */
	static final double[] GANGNAM = { 37.4979, 127.0276 };
	/** 홍대입구역 근처(강남과 약 9km) */
	static final double[] HONGDAE = { 37.5572, 126.9245 };

	private TestPlaces() {
	}

	static Place place(String id, double latitude, double longitude) {
		return place(id, "서울특별시 강남구 테헤란로 " + id, "I201", "I20101", latitude, longitude);
	}

	static Place place(String id, String address, String mediumCode, String smallCode, double latitude,
			double longitude) {
		String medium = "I201".equals(mediumCode) ? "한식" : "비알코올 음료점";
		return new Place(id, "장소" + id, "I2", mediumCode, smallCode, "음식 > " + medium + " > 세부", address, latitude,
				longitude, 0);
	}

	/** base에서 bearing(도) 방향으로 meters만큼 떨어진 장소. 동쪽은 90도. */
	static Place at(String id, double[] base, double bearing, double meters) {
		double[] p = GeoMath.offset(base[0], base[1], bearing, meters);
		return place(id, p[0], p[1]);
	}

	static Proposal proposal(Place place, int order) {
		return new Proposal(place, order);
	}
}
