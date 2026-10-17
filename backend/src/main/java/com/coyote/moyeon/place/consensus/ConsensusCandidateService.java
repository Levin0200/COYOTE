package com.coyote.moyeon.place.consensus;

import com.coyote.moyeon.place.CategoryFilter;
import com.coyote.moyeon.place.GeoMath;
import com.coyote.moyeon.place.Place;
import com.coyote.moyeon.place.PlaceSearchService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 합의 지역의 대표 중심점 주변에서 최종 후보를 만든다(건너뛰기 모임에서 단독 1위가 없을 때, 호스트가 고를 후보).
 * 군집 안에서 게스트가 제안한 장소를 먼저 넣고, 모자라면 중심점 주변 검색으로 채워 최대 {@value #MAX_CANDIDATES}곳을 만든다.
 * 검색 업종은 군집에서 가장 많이 제안된 업종을 우선한다.
 */
@Service
public class ConsensusCandidateService {

	public static final int MAX_CANDIDATES = 10;
	public static final int FALLBACK_PICKS = 3;
	private static final int FILL_RADIUS_METERS = 1000;
	private static final int PREFERRED_CATEGORIES = 2;
	private static final int MAX_PER_ADDRESS = 2;

	private final PlaceSearchService placeSearchService;

	public ConsensusCandidateService(PlaceSearchService placeSearchService) {
		this.placeSearchService = placeSearchService;
	}

	public List<Place> candidates(ConsensusOutcome.Spatial spatial) {
		double lat = spatial.centerLatitude();
		double lon = spatial.centerLongitude();

		List<Place> result = new ArrayList<>();
		Set<String> ids = new HashSet<>();
		Map<String, Integer> perAddress = new HashMap<>();
		List<PlaceVote> members = spatial.cluster().members().stream()
				.sorted(Comparator.comparingInt(PlaceVote::votes).reversed()
						.thenComparingDouble(vote -> distanceToCenter(vote.place(), lat, lon)))
				.toList();
		for (PlaceVote member : members) {
			if (result.size() == MAX_CANDIDATES) {
				break;
			}
			if (ids.add(member.place().id())) {
				result.add(member.place().withDistance(distanceToCenter(member.place(), lat, lon)));
				perAddress.merge(member.place().address(), 1, Integer::sum);
			}
		}

		if (result.size() < MAX_CANDIDATES) {
			PlaceSearchService.Result found = placeSearchService.searchWithFilters(lat, lon, FILL_RADIUS_METERS,
					preferredFilters(members), List.of(), MAX_CANDIDATES * 2);
			for (Place place : found.places()) {
				if (result.size() == MAX_CANDIDATES) {
					break;
				}
				int sameAddress = perAddress.getOrDefault(place.address(), 0);
				if (!ids.contains(place.id()) && sameAddress < MAX_PER_ADDRESS) {
					ids.add(place.id());
					perAddress.merge(place.address(), 1, Integer::sum);
					result.add(place);
				}
			}
		}
		return List.copyOf(result);
	}

	/** AI를 쓸 수 없을 때의 대체 선정: 대표 중심점에 가장 가까운 후보 count곳. */
	public static List<Place> fallbackPick(List<Place> candidates, int count) {
		return candidates.stream().sorted(Comparator.comparingDouble(Place::distanceMeters)).limit(count).toList();
	}

	/** 군집에서 득표가 많은 순으로 상위 업종(중분류)을 필터로 만든다. 없으면 빈 목록(검색이 음식 대분류를 쓴다). */
	private static List<CategoryFilter> preferredFilters(List<PlaceVote> members) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		Map<String, String> labels = new HashMap<>();
		for (PlaceVote member : members) {
			String code = member.place().mediumCategoryCode();
			if (code == null || code.isBlank()) {
				continue;
			}
			counts.merge(code, member.votes(), Integer::sum);
			labels.putIfAbsent(code, mediumLabel(member.place()));
		}
		return counts.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
				.limit(PREFERRED_CATEGORIES)
				.map(entry -> CategoryFilter.medium(entry.getKey(), labels.get(entry.getKey())))
				.toList();
	}

	/** "음식 > 한식 > 백반/한정식"에서 가운데(중분류) 이름을 꺼낸다. */
	private static String mediumLabel(Place place) {
		String[] parts = place.categoryName().split(" > ");
		return parts.length >= 2 ? parts[1].trim() : place.mediumCategoryCode();
	}

	private static double distanceToCenter(Place place, double lat, double lon) {
		return GeoMath.distanceMeters(lat, lon, place.latitude(), place.longitude());
	}
}
