package com.coyote.moyeon.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coyote.moyeon.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PlaceSearchServiceTest {

	private static final double LAT = 37.4979;
	private static final double LON = 127.0276;

	/** 호출 횟수를 세는 제공자. 실제 데이터는 FakePlaceProvider가 만든다. */
	private static class CountingProvider implements PlaceSearchProvider {
		private final FakePlaceProvider delegate = new FakePlaceProvider();
		int radiusCalls;
		int boxCalls;

		@Override
		public boolean isAvailable() {
			return true;
		}

		@Override
		public List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter) {
			radiusCalls++;
			return delegate.findInRadius(latitude, longitude, radiusMeters, filter);
		}

		@Override
		public List<Place> findInBox(double minLongitude, double minLatitude, double maxLongitude,
				double maxLatitude, CategoryFilter filter) {
			boxCalls++;
			return delegate.findInBox(minLongitude, minLatitude, maxLongitude, maxLatitude, filter);
		}
	}

	private final CountingProvider provider = new CountingProvider();
	private final PlaceSearchService service = new PlaceSearchService(provider, new KeywordCategoryMapper());

	@Test
	void stopsAtTheSmallestRingThatHasEnoughPlaces() {
		PlaceSearchService.Result result = service.search(LAT, LON, 3000, null, 5);

		assertThat(result.places()).hasSize(5);
		assertThat(result.searchedRadiusMeters()).isEqualTo(500);
		assertThat(provider.radiusCalls).isEqualTo(1);
		assertThat(provider.boxCalls).isZero();
	}

	@Test
	void resultsAreSortedByDistanceAndWithinTheRadius() {
		PlaceSearchService.Result result = service.search(LAT, LON, 1000, null, 30);

		List<Double> distances = result.places().stream().map(Place::distanceMeters).toList();
		assertThat(distances).isSorted();
		assertThat(distances).allMatch(distance -> distance <= 1000);
		assertThat(result.places()).extracting(Place::id).doesNotHaveDuplicates();
	}

	@Test
	void widensTheRingsStepByStepWhenThereAreNotEnoughPlaces() {
		PlaceSearchService.Result result = service.search(LAT, LON, 3000, null, 30);

		// 500m(12곳), 1km(24곳)로는 30곳이 안 돼 2km까지 넓힌 뒤 멈춘다. 2km 이하는 반경 조회만 쓴다.
		assertThat(result.places()).hasSize(30);
		assertThat(result.searchedRadiusMeters()).isEqualTo(2000);
		assertThat(provider.radiusCalls).isEqualTo(3);
		assertThat(provider.boxCalls).isZero();
	}

	@Test
	void usesTheBoxQueryBeyondTwoKilometers() {
		PlaceSearchService.Result result = service.search(LAT, LON, 4500, "노래방", 30);

		// 노래방은 9곳마다 한 곳이라 4.5km 안에 13곳뿐이다. 마지막 단계(4.5km)는 사각형 조회를 쓴다.
		assertThat(result.places()).hasSize(13);
		assertThat(result.searchedRadiusMeters()).isEqualTo(4500);
		assertThat(provider.boxCalls).isEqualTo(1);
		assertThat(result.places()).allMatch(place -> place.distanceMeters() <= 4500);
		assertThat(result.places()).extracting(Place::smallCategoryCode).containsOnly("R10407");
	}

	@Test
	void appliesKeywordCategoriesAndReportsIgnoredWords() {
		PlaceSearchService.Result result = service.search(LAT, LON, 2000, "조용한 카페", 5);

		assertThat(result.places()).hasSize(5);
		assertThat(result.places()).extracting(Place::smallCategoryCode).containsOnly("I21201");
		assertThat(result.appliedCategories()).containsExactly("카페");
		assertThat(result.ignoredKeywords()).containsExactly("조용한");
	}

	@Test
	void withoutKeywordOnlyFoodPlacesAreReturned() {
		PlaceSearchService.Result result = service.search(LAT, LON, 2000, null, 30);

		assertThat(result.appliedCategories()).containsExactly("음식");
		assertThat(result.places()).extracting(Place::largeCategoryCode).containsOnly("I2");
	}

	@Test
	void placesWhoseNameMatchesAnUnknownWordComeFirst() {
		PlaceSearchProvider cafes = new PlaceSearchProvider() {
			@Override
			public boolean isAvailable() {
				return true;
			}

			@Override
			public List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter) {
				return List.of(
						new Place("1", "동네카페", "I2", "I212", "I21201", "음식 > 카페", "서울 강남구 1", LAT + 0.001, LON, 0),
						new Place("2", "이웃카페", "I2", "I212", "I21201", "음식 > 카페", "서울 강남구 2", LAT + 0.002, LON, 0),
						new Place("3", "스타벅스 강남점", "I2", "I212", "I21201", "음식 > 카페", "서울 강남구 3", LAT + 0.004, LON, 0));
			}

			@Override
			public List<Place> findInBox(double a, double b, double c, double d, CategoryFilter filter) {
				return List.of();
			}
		};

		PlaceSearchService.Result result = new PlaceSearchService(cafes, new KeywordCategoryMapper())
				.search(LAT, LON, 1000, "스타벅스", 3);

		// 가장 멀어도 이름에 "스타벅스"가 들어간 곳이 먼저 나온다.
		assertThat(result.places()).extracting(Place::name).containsExactly("스타벅스 강남점", "동네카페", "이웃카페");
		assertThat(result.ignoredKeywords()).containsExactly("스타벅스");
	}

	@Test
	void radiusAndLimitAreClampedToTheAllowedRange() {
		assertThat(service.search(LAT, LON, 99999, "노래방", 1).searchedRadiusMeters()).isEqualTo(500);
		assertThat(service.search(LAT, LON, 99999, "노래방", 30).searchedRadiusMeters())
				.isEqualTo(PlaceSearchService.MAX_RADIUS_METERS);
		assertThat(service.search(LAT, LON, 1, "카페", 1).searchedRadiusMeters())
				.isEqualTo(PlaceSearchService.MIN_RADIUS_METERS);
		assertThat(service.search(LAT, LON, 2000, null, 1000).places()).hasSize(PlaceSearchService.MAX_LIMIT);
		assertThat(service.search(LAT, LON, 2000, null, 0).places()).hasSize(1);
	}

	@Test
	void ringsAreBuiltFromTheRequestedRadius() {
		assertThat(PlaceSearchService.rings(300)).containsExactly(300);
		assertThat(PlaceSearchService.rings(500)).containsExactly(500);
		assertThat(PlaceSearchService.rings(1500)).containsExactly(500, 1000, 1500);
		assertThat(PlaceSearchService.rings(2000)).containsExactly(500, 1000, 2000);
		assertThat(PlaceSearchService.rings(3000)).containsExactly(500, 1000, 2000, 3000);
	}

	@Test
	void rejectsCoordinatesOutsideKorea() {
		for (double[] point : new double[][] { { 0, 0 }, { 48.85, 2.35 }, { 37.5, 140.0 }, { 20.0, 127.0 } }) {
			assertThatThrownBy(() -> service.search(point[0], point[1], 1000, null, 10))
					.isInstanceOfSatisfying(BusinessException.class, e -> {
						assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
						assertThat(e.getCode()).isEqualTo("INVALID_REQUEST");
					});
		}
	}

	@Test
	void rejectsTooLongKeywords() {
		assertThatThrownBy(() -> service.search(LAT, LON, 1000, "가".repeat(101), 10))
				.isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_REQUEST"));
	}

	@Test
	void reportsUnavailableWhenTheProviderHasNoKey() {
		PlaceSearchProvider unavailable = new PlaceSearchProvider() {
			@Override
			public boolean isAvailable() {
				return false;
			}

			@Override
			public List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter) {
				throw new AssertionError("키가 없으면 호출하면 안 된다");
			}

			@Override
			public List<Place> findInBox(double a, double b, double c, double d, CategoryFilter filter) {
				throw new AssertionError("키가 없으면 호출하면 안 된다");
			}
		};

		assertThatThrownBy(() -> new PlaceSearchService(unavailable, new KeywordCategoryMapper()).search(LAT, LON, 1000, null, 10))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
					assertThat(e.getCode()).isEqualTo("PLACE_SEARCH_UNAVAILABLE");
				});
	}

	@Test
	void atMostTwoPlacesPerAddressAreReturned() {
		PlaceSearchProvider oneBuilding = new PlaceSearchProvider() {
			@Override
			public boolean isAvailable() {
				return true;
			}

			@Override
			public List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter) {
				List<Place> places = new ArrayList<>();
				for (int i = 0; i < 5; i++) {
					places.add(new Place("m" + i, "같은건물카페" + i, "I2", "I212", "I21201", "음식 > 카페", "서울 강남구 대로 396",
							LAT + 0.0005, LON, 0));
				}
				places.add(new Place("x", "옆건물카페", "I2", "I212", "I21201", "음식 > 카페", "서울 강남구 대로 400", LAT + 0.002, LON, 0));
				places.add(new Place("y", "먼건물카페", "I2", "I212", "I21201", "음식 > 카페", "서울 강남구 대로 410", LAT + 0.003, LON, 0));
				return places;
			}

			@Override
			public List<Place> findInBox(double a, double b, double c, double d, CategoryFilter filter) {
				return List.of();
			}
		};

		PlaceSearchService.Result result = new PlaceSearchService(oneBuilding, new KeywordCategoryMapper())
				.search(LAT, LON, 500, null, 4);

		assertThat(result.places()).extracting(Place::name)
				.containsExactly("같은건물카페0", "같은건물카페1", "옆건물카페", "먼건물카페");
	}

	@Test
	void entertainmentBarsAreNeverReturned() {
		PlaceSearchProvider bars = new PlaceSearchProvider() {
			@Override
			public boolean isAvailable() {
				return true;
			}

			@Override
			public List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter) {
				return List.of(
						new Place("1", "요리주점", "I2", "I211", "I21104", "음식 > 주점 > 요리 주점", "서울 강남구 1", LAT + 0.001, LON, 0),
						new Place("2", "룸살롱", "I2", "I211", "I21101", "음식 > 주점 > 일반 유흥 주점", "서울 강남구 2", LAT + 0.0005, LON, 0),
						new Place("3", "클럽", "I2", "I211", "I21102", "음식 > 주점 > 무도 유흥 주점", "서울 강남구 3", LAT + 0.0006, LON, 0));
			}

			@Override
			public List<Place> findInBox(double a, double b, double c, double d, CategoryFilter filter) {
				return List.of();
			}
		};

		PlaceSearchService.Result result = new PlaceSearchService(bars, new KeywordCategoryMapper())
				.search(LAT, LON, 500, "주점", 10);

		assertThat(result.places()).extracting(Place::name).containsExactly("요리주점");
	}

	@Test
	void samePlaceWithDifferentIdsIsReturnedOnce() {
		PlaceSearchProvider duplicates = new PlaceSearchProvider() {
			@Override
			public boolean isAvailable() {
				return true;
			}

			@Override
			public List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter) {
				List<Place> places = new ArrayList<>();
				places.add(new Place("a", "중복카페", "I2", "I212", "I21201", "음식 > 카페", "서울 강남구 1", LAT + 0.001, LON, 0));
				places.add(new Place("b", "중복카페", "I2", "I212", "I21201", "음식 > 카페", "서울 강남구 1", LAT + 0.001, LON, 0));
				places.add(new Place("c", "다른카페", "I2", "I212", "I21201", "음식 > 카페", "서울 강남구 2", LAT + 0.002, LON, 0));
				return places;
			}

			@Override
			public List<Place> findInBox(double a, double b, double c, double d, CategoryFilter filter) {
				return List.of();
			}
		};

		PlaceSearchService.Result result = new PlaceSearchService(duplicates, new KeywordCategoryMapper())
				.search(LAT, LON, 500, null, 10);

		assertThat(result.places()).extracting(Place::name).containsExactly("중복카페", "다른카페");
	}
}
