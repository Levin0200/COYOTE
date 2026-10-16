package com.coyote.moyeon.place;

import java.util.List;

/**
 * 장소 데이터 제공자. 서버 코드는 이 인터페이스만 쓰므로 데이터 출처(공공데이터, 카카오 등)를 구현체만 바꿔 교체할 수 있다.
 * 결과는 거리순으로 정렬되어 있지 않고, 거리 값도 채워지지 않는다(PlaceSearchService가 처리한다).
 */
public interface PlaceSearchProvider {

	/** 키가 설정되어 있는 등 검색을 할 수 있는 상태인가. */
	boolean isAvailable();

	/** 중심에서 반경(미터) 안의 장소. filter가 null이면 업종을 거르지 않는다. */
	List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter);

	/** 사각형 영역 안의 장소. 한 변이 9km를 넘으면 안 된다. */
	List<Place> findInBox(double minLongitude, double minLatitude, double maxLongitude, double maxLatitude,
			CategoryFilter filter);
}
