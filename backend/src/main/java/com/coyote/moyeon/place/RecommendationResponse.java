package com.coyote.moyeon.place;

import java.util.List;

/**
 * 장소 추천 결과. places는 추천 순서대로이고, 첫 화면에는 앞에서 pageSize개를 보여 주고 "새로고침"을 누르면
 * 나머지를 보여 준다(추가 호출 없음). aiUsed가 false면 AI 없이 규칙(가까운 순서·업종 다양성)으로 고른 것이다.
 */
public record RecommendationResponse(List<RecommendedPlace> places, int pageSize, boolean aiUsed,
		List<String> appliedCategories, List<String> ignoredKeywords, int searchedRadiusMeters) {

	public static final int PAGE_SIZE = 3;

	public record RecommendedPlace(String id, String name, String category, String address, double latitude,
			double longitude, int distanceMeters, String reason) {

		static RecommendedPlace from(RankedPlace ranked) {
			Place place = ranked.place();
			return new RecommendedPlace(place.id(), place.name(), place.categoryName(), place.address(),
					place.latitude(), place.longitude(), (int) Math.round(place.distanceMeters()), ranked.reason());
		}
	}

	static RecommendationResponse from(PlaceSearchService.Result pool, Ranking ranking) {
		return new RecommendationResponse(ranking.places().stream().map(RecommendedPlace::from).toList(), PAGE_SIZE,
				ranking.aiUsed(), pool.appliedCategories(), pool.ignoredKeywords(), pool.searchedRadiusMeters());
	}
}
