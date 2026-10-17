package com.coyote.moyeon.place;

import java.util.List;

/**
 * 장소 검색 응답. appliedCategories는 키워드에서 알아들은 업종, ignoredKeywords는 업종으로 바꾸지 못해
 * 이름 우선 정렬에만 쓴 단어다(예: "조용한"). searchedRadiusMeters는 실제로 조회한 반경이다.
 */
public record PlaceSearchResponse(List<PlaceView> places, List<String> appliedCategories,
		List<String> ignoredKeywords, int searchedRadiusMeters) {

	public record PlaceView(String id, String name, String category, String address, double latitude,
			double longitude, int distanceMeters) {

		static PlaceView from(Place place) {
			return new PlaceView(place.id(), place.name(), place.categoryName(), place.address(), place.latitude(),
					place.longitude(), (int) Math.round(place.distanceMeters()));
		}
	}

	static PlaceSearchResponse from(PlaceSearchService.Result result) {
		return new PlaceSearchResponse(result.places().stream().map(PlaceView::from).toList(),
				result.appliedCategories(), result.ignoredKeywords(), result.searchedRadiusMeters());
	}
}
