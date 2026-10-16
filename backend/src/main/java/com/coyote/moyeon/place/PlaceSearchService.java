package com.coyote.moyeon.place;

import com.coyote.moyeon.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 가까운 장소를 찾는다. 공공데이터 API는 거리순 정렬을 하지 않고 반경이 최대 2km이므로,
 * 작은 반경(500m, 1km, 2km)부터 조회해 후보가 충분해지면 멈추고, 그보다 넓은 범위는 사각형 조회로 넓힌다.
 * 결과는 서버에서 거리를 계산해 가까운 순서로 정렬한다.
 */
@Service
public class PlaceSearchService {

	public static final int MIN_RADIUS_METERS = 100;
	/** 사각형 조회의 한 변이 9km를 넘으면 API가 거부하므로 반경은 4.5km까지만 받는다. */
	public static final int MAX_RADIUS_METERS = 4500;
	public static final int DEFAULT_LIMIT = 20;
	public static final int MAX_LIMIT = 30;
	private static final int RADIUS_QUERY_MAX_METERS = 2000;
	private static final int[] STEPS = { 500, 1000, 2000 };
	private static final int MAX_FILTERS = 5;
	/** 같은 건물(주소)에 점포가 몰려 있어도 추천 후보가 한 건물로만 채워지지 않게 주소당 최대 개수를 제한한다. */
	static final int MAX_PER_ADDRESS = 2;
	/** 실시간 영업상태 API 호출이 과도해지지 않게 최종 순위 상위 후보만 검증한다. */
	private static final int STATUS_VERIFICATION_EXTRA = 6;
	private static final int STATUS_VERIFICATION_MAX = 30;
	/** 모임 장소로 부적절한 업종(일반 유흥 주점, 무도 유흥 주점)은 검색 결과에서 뺀다. */
	private static final Set<String> EXCLUDED_SMALL_CODES = Set.of("I21101", "I21102");
	private static final int KEYWORD_MAX_LENGTH = 100;

	public record Result(List<Place> places, List<String> appliedCategories, List<String> ignoredKeywords,
			int searchedRadiusMeters) {
	}

	private final PlaceSearchProvider provider;
	private final KeywordCategoryMapper mapper;
	private final PlaceStatusVerifier statusVerifier;

	@Autowired
	public PlaceSearchService(PlaceSearchProvider provider, KeywordCategoryMapper mapper,
			PlaceStatusVerifier statusVerifier) {
		this.provider = provider;
		this.mapper = mapper;
		this.statusVerifier = statusVerifier;
	}

	public PlaceSearchService(PlaceSearchProvider provider, KeywordCategoryMapper mapper) {
		this(provider, mapper, PlaceStatusVerifier.permissive());
	}

	public Result search(double latitude, double longitude, int radiusMeters, String keyword, int limit) {
		if (keyword != null && keyword.length() > KEYWORD_MAX_LENGTH) {
			throw invalid("키워드는 " + KEYWORD_MAX_LENGTH + "자 이하여야 합니다.");
		}
		KeywordCategoryMapper.Mapping mapping = mapper.map(keyword);
		List<CategoryFilter> filters = mapping.filters().isEmpty() ? List.of(KeywordCategoryMapper.DEFAULT_FILTER)
				: mapping.filters();
		List<String> nameTerms = mapping.ignoredKeywords().stream().filter(term -> term.length() >= 2).toList();
		Result result = searchWithFilters(latitude, longitude, radiusMeters, filters, nameTerms, limit);
		return new Result(result.places(), result.appliedCategories(), mapping.ignoredKeywords(),
				result.searchedRadiusMeters());
	}

	/**
	 * 업종 필터를 직접 지정해 가까운 장소를 찾는다(키워드 해석 없이). 합의 지역 주변 후보처럼 서버가 업종을 정하는 경우에 쓴다.
	 * nameTerms에 이름이 맞는 곳은 거리와 상관없이 먼저 나온다. filters가 비어 있으면 음식 대분류를 쓴다.
	 */
	public Result searchWithFilters(double latitude, double longitude, int radiusMeters, List<CategoryFilter> filters,
			List<String> nameTerms, int limit) {
		if (latitude < 33.0 || latitude > 38.7 || longitude < 124.5 || longitude > 132.0) {
			throw invalid("대한민국 안의 좌표를 입력해주세요.");
		}
		if (!provider.isAvailable()) {
			throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "PLACE_SEARCH_UNAVAILABLE",
					"장소 검색을 지금 사용할 수 없습니다.");
		}
		int radius = Math.max(MIN_RADIUS_METERS, Math.min(MAX_RADIUS_METERS, radiusMeters));
		int count = Math.max(1, Math.min(MAX_LIMIT, limit));
		List<CategoryFilter> applied = filters.isEmpty() ? List.of(KeywordCategoryMapper.DEFAULT_FILTER)
				: filters.stream().limit(MAX_FILTERS).toList();

		List<Place> ranked = List.of();
		int searched = radius;
		for (int ring : rings(radius)) {
			searched = ring;
			Map<String, Place> found = new LinkedHashMap<>();
			for (CategoryFilter filter : applied) {
				for (Place place : fetch(latitude, longitude, ring, filter)) {
					found.putIfAbsent(place.id(), place);
				}
			}
			ranked = rank(found.values(), latitude, longitude, ring, nameTerms, count);
			if (ranked.size() >= count) {
				break;
			}
		}
		return new Result(ranked, applied.stream().map(CategoryFilter::label).toList(), List.of(), searched);
	}

	private List<Place> fetch(double latitude, double longitude, int ring, CategoryFilter filter) {
		if (ring <= RADIUS_QUERY_MAX_METERS) {
			return provider.findInRadius(latitude, longitude, ring, filter);
		}
		double[] box = GeoMath.boundingBox(latitude, longitude, ring);
		return provider.findInBox(box[0], box[1], box[2], box[3], filter);
	}

	/** 조회할 반경 목록. 예: 3000 -> [500, 1000, 2000, 3000], 300 -> [300]. */
	static List<Integer> rings(int radius) {
		List<Integer> rings = new ArrayList<>();
		for (int step : STEPS) {
			if (step < radius) {
				rings.add(step);
			}
		}
		rings.add(radius);
		return rings;
	}

	private List<Place> rank(Collection<Place> candidates, double latitude, double longitude, int radius,
			List<String> nameTerms, int limit) {
		Set<String> seen = new HashSet<>();
		List<Place> within = new ArrayList<>();
		for (Place place : candidates) {
			double distance = GeoMath.distanceMeters(latitude, longitude, place.latitude(), place.longitude());
			// 같은 건물의 같은 상호가 중복으로 내려오는 경우를 하나로 합친다.
			if (distance <= radius && !EXCLUDED_SMALL_CODES.contains(place.smallCategoryCode())
					&& seen.add(place.name() + "|" + place.address())) {
				within.add(place.withDistance(distance));
			}
		}
		Comparator<Place> byName = Comparator.comparing((Place place) -> !matchesAny(place, nameTerms));
		within.sort(byName.thenComparingDouble(Place::distanceMeters));

		Map<String, Integer> perAddress = new HashMap<>();
		List<Place> verificationCandidates = new ArrayList<>();
		int verificationLimit = Math.min(STATUS_VERIFICATION_MAX, limit + STATUS_VERIFICATION_EXTRA);
		for (Place place : within) {
			String address = place.address().isBlank() ? place.id() : place.address();
			if (perAddress.merge(address, 1, Integer::sum) <= MAX_PER_ADDRESS) {
				verificationCandidates.add(place);
				if (verificationCandidates.size() == verificationLimit) {
					break;
				}
			}
		}

		Map<Place, PlaceStatusVerifier.Status> statuses = statusVerifier.statusesOf(verificationCandidates);
		List<Place> result = new ArrayList<>();
		for (Place place : verificationCandidates) {
			if (statuses.getOrDefault(place, PlaceStatusVerifier.Status.UNKNOWN) != PlaceStatusVerifier.Status.CLOSED) {
				result.add(place);
				if (result.size() == limit) {
					break;
				}
			}
		}
		return List.copyOf(result);
	}

	private static boolean matchesAny(Place place, List<String> terms) {
		String name = place.name().toLowerCase();
		return terms.stream().anyMatch(name::contains);
	}

	private static BusinessException invalid(String message) {
		return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
	}
}
