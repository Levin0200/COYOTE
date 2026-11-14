package com.coyote.moyeon.place;

/**
 * 검색된 장소 한 곳. id는 검색 제공자가 붙인 번호(공공데이터는 상가업소번호)이고,
 * 거리는 검색 중심에서의 직선 거리(미터)다.
 */
public record Place(String id, String name, String largeCategoryCode, String mediumCategoryCode,
		String smallCategoryCode, String categoryName, String address, double latitude, double longitude,
		double distanceMeters) {

	public Place withDistance(double meters) {
		return new Place(id, name, largeCategoryCode, mediumCategoryCode, smallCategoryCode, categoryName, address,
				latitude, longitude, meters);
	}
}
