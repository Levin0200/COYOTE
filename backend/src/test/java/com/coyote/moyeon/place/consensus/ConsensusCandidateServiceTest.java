package com.coyote.moyeon.place.consensus;

import static com.coyote.moyeon.place.consensus.TestPlaces.GANGNAM;
import static com.coyote.moyeon.place.consensus.TestPlaces.place;
import static com.coyote.moyeon.place.consensus.TestPlaces.proposal;
import static org.assertj.core.api.Assertions.assertThat;

import com.coyote.moyeon.place.CategoryFilter;
import com.coyote.moyeon.place.FakePlaceProvider;
import com.coyote.moyeon.place.GeoMath;
import com.coyote.moyeon.place.KeywordCategoryMapper;
import com.coyote.moyeon.place.Place;
import com.coyote.moyeon.place.PlaceSearchProvider;
import com.coyote.moyeon.place.PlaceSearchService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsensusCandidateServiceTest {

	private final FakePlaceProvider fake = new FakePlaceProvider();
	private int searchCalls;

	private final PlaceSearchProvider counting = new PlaceSearchProvider() {
		@Override
		public boolean isAvailable() {
			return true;
		}

		@Override
		public List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter) {
			searchCalls++;
			return fake.findInRadius(latitude, longitude, radiusMeters, filter);
		}

		@Override
		public List<Place> findInBox(double a, double b, double c, double d, CategoryFilter filter) {
			searchCalls++;
			return fake.findInBox(a, b, c, d, filter);
		}
	};

	private final ConsensusCandidateService service = new ConsensusCandidateService(
			new PlaceSearchService(counting, new KeywordCategoryMapper()));

	private static Place cafe(String id, double[] position, String address) {
		return place(id, address, "I212", "I21201", position[0], position[1]);
	}

	private static Place korean(String id, double[] position, String address) {
		return place(id, address, "I201", "I20101", position[0], position[1]);
	}

	private static ConsensusOutcome.Spatial spatial(List<Proposal> proposals) {
		return (ConsensusOutcome.Spatial) SpatialConsensus.decide(proposals);
	}

	@Test
	void proposedPlacesComeFirstAndTheRestIsFilledByNearbySearch() {
		Place a = cafe("a", GeoMath.offset(GANGNAM[0], GANGNAM[1], 90, 0), "서울특별시 강남구 테헤란로 1");
		Place b = cafe("b", GeoMath.offset(GANGNAM[0], GANGNAM[1], 90, 600), "서울특별시 강남구 테헤란로 2");

		List<Place> candidates = service.candidates(spatial(List.of(proposal(a, 1), proposal(b, 2))));

		// 테스트용 가짜 데이터에는 반경 1km 안에 카페가 3곳뿐이라 제안 2곳 + 검색 3곳이다.
		assertThat(candidates).hasSize(5);
		assertThat(candidates.subList(0, 2)).extracting(Place::id).containsExactlyInAnyOrder("a", "b");
		assertThat(candidates).extracting(Place::id).doesNotHaveDuplicates();
		// 제안이 카페였으므로 채워 넣은 곳도 같은 업종(카페가 속한 중분류)이다.
		assertThat(candidates.subList(2, candidates.size())).extracting(Place::mediumCategoryCode).containsOnly("I212");
		// 모든 후보에는 대표 중심점까지의 거리가 들어 있다.
		assertThat(candidates).allMatch(place -> place.distanceMeters() >= 0);
		assertThat(candidates.subList(2, candidates.size())).extracting(Place::distanceMeters).isSorted();
	}

	@Test
	void doesNotSearchWhenEnoughPlacesWereProposed() {
		List<Proposal> proposals = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			double[] p = GeoMath.offset(GANGNAM[0], GANGNAM[1], i * 30, 300);
			proposals.add(proposal(cafe("p" + i, p, "서울특별시 강남구 가로 " + i), i + 1));
		}

		List<Place> candidates = service.candidates(spatial(proposals));

		assertThat(candidates).hasSize(ConsensusCandidateService.MAX_CANDIDATES);
		assertThat(candidates).extracting(Place::id).allMatch(id -> id.startsWith("p"));
		assertThat(searchCalls).isZero();
	}

	@Test
	void searchesTheMostProposedCategories() {
		Place k1 = korean("k1", GeoMath.offset(GANGNAM[0], GANGNAM[1], 0, 0), "서울특별시 강남구 가로 1");
		Place k2 = korean("k2", GeoMath.offset(GANGNAM[0], GANGNAM[1], 90, 400), "서울특별시 강남구 가로 2");
		Place c1 = cafe("c1", GeoMath.offset(GANGNAM[0], GANGNAM[1], 180, 400), "서울특별시 강남구 가로 3");

		List<Place> candidates = service.candidates(spatial(List.of(proposal(k1, 1), proposal(k2, 2), proposal(c1, 3))));

		assertThat(candidates.subList(3, candidates.size())).extracting(Place::mediumCategoryCode)
				.containsOnly("I201", "I212");
		assertThat(candidates).hasSize(ConsensusCandidateService.MAX_CANDIDATES);
	}

	@Test
	void fallbackPicksTheCandidatesNearestToTheCenter() {
		Place a = cafe("a", GeoMath.offset(GANGNAM[0], GANGNAM[1], 90, 0), "서울특별시 강남구 테헤란로 1");
		Place b = cafe("b", GeoMath.offset(GANGNAM[0], GANGNAM[1], 90, 600), "서울특별시 강남구 테헤란로 2");
		List<Place> candidates = service.candidates(spatial(List.of(proposal(a, 1), proposal(b, 2))));

		List<Place> picks = ConsensusCandidateService.fallbackPick(candidates, ConsensusCandidateService.FALLBACK_PICKS);

		assertThat(picks).hasSize(3);
		assertThat(picks).extracting(Place::distanceMeters).isSorted();
		double thirdDistance = picks.get(2).distanceMeters();
		assertThat(candidates).filteredOn(place -> !picks.contains(place))
				.allMatch(place -> place.distanceMeters() >= thirdDistance);
	}
}
