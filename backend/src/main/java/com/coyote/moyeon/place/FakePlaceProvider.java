package com.coyote.moyeon.place;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 개발·테스트용 고정 데이터 제공자(app.place.provider=fake). 검색 중심 주변에 거리가 일정하게 늘어나는 장소를 만든다.
 * i번째 장소는 중심에서 80m + 35m * i 떨어져 있고 업종은 순서대로 돌아가며, id는 "fake-i"다.
 */
@Component
@ConditionalOnProperty(name = "app.place.provider", havingValue = "fake")
public class FakePlaceProvider implements PlaceSearchProvider {

	public static final int PLACE_COUNT = 120;

	private record Kind(String large, String medium, String small, String name, String category) {
	}

	private static final List<Kind> KINDS = List.of(
			new Kind("I2", "I212", "I21201", "카페", "음식 > 비알코올 음료점 > 카페"),
			new Kind("I2", "I201", "I20101", "백반집", "음식 > 한식 > 백반/한정식"),
			new Kind("I2", "I210", "I21006", "치킨집", "음식 > 기타 간이 음식점 > 치킨"),
			new Kind("I2", "I203", "I20301", "초밥집", "음식 > 일식 > 일식 회/초밥"),
			new Kind("I2", "I202", "I20201", "중국집", "음식 > 중식 > 중국집"),
			new Kind("I2", "I204", "I20402", "파스타집", "음식 > 서양식 > 파스타/스테이크"),
			new Kind("I2", "I211", "I21104", "요리주점", "음식 > 주점 > 요리 주점"),
			new Kind("I2", "I201", "I20107", "고깃집", "음식 > 한식 > 돼지고기 구이/찜"),
			new Kind("R1", "R104", "R10407", "노래방", "예술·스포츠 > 유원지·오락 > 노래방"));

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter) {
		List<Place> result = new ArrayList<>();
		for (Place place : generate(latitude, longitude)) {
			if (GeoMath.distanceMeters(latitude, longitude, place.latitude(), place.longitude()) <= radiusMeters
					&& (filter == null || filter.matches(place))) {
				result.add(place);
			}
		}
		return result;
	}

	@Override
	public List<Place> findInBox(double minLongitude, double minLatitude, double maxLongitude, double maxLatitude,
			CategoryFilter filter) {
		double centerLat = (minLatitude + maxLatitude) / 2;
		double centerLon = (minLongitude + maxLongitude) / 2;
		List<Place> result = new ArrayList<>();
		for (Place place : generate(centerLat, centerLon)) {
			boolean inside = place.longitude() >= minLongitude && place.longitude() <= maxLongitude
					&& place.latitude() >= minLatitude && place.latitude() <= maxLatitude;
			if (inside && (filter == null || filter.matches(place))) {
				result.add(place);
			}
		}
		return result;
	}

	private static List<Place> generate(double centerLat, double centerLon) {
		List<Place> places = new ArrayList<>(PLACE_COUNT);
		for (int i = 0; i < PLACE_COUNT; i++) {
			Kind kind = KINDS.get(i % KINDS.size());
			double[] position = GeoMath.offset(centerLat, centerLon, (i * 47) % 360, 80 + 35.0 * i);
			places.add(new Place("fake-" + i, "테스트" + kind.name() + " " + i, kind.large(), kind.medium(), kind.small(),
					kind.category(), "테스트시 테스트로 " + i, position[0], position[1], 0));
		}
		return places;
	}
}
