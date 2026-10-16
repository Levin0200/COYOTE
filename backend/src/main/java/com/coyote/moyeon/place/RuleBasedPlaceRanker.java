package com.coyote.moyeon.place;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * AI 없이 고르는 기본 방식. 가까운 순서를 유지하되 같은 세부 업종이 두 곳을 넘지 않게 고르고,
 * 모자라면 가까운 순서로 채운다. AI를 쓸 수 없거나 실패했을 때의 대체이기도 하다.
 */
@Component
public class RuleBasedPlaceRanker {

	private static final int MAX_PER_CATEGORY = 2;

	/** candidates는 가까운 순서로 정렬되어 있어야 한다. */
	public List<RankedPlace> rank(List<Place> candidates, int pick) {
		List<RankedPlace> chosen = new ArrayList<>();
		Set<String> chosenIds = new HashSet<>();
		Map<String, Integer> perCategory = new HashMap<>();
		for (Place place : candidates) {
			if (chosen.size() == pick) {
				break;
			}
			if (perCategory.getOrDefault(place.smallCategoryCode(), 0) < MAX_PER_CATEGORY) {
				chosen.add(new RankedPlace(place, null));
				chosenIds.add(place.id());
				perCategory.merge(place.smallCategoryCode(), 1, Integer::sum);
			}
		}
		for (Place place : candidates) {
			if (chosen.size() == pick) {
				break;
			}
			if (chosenIds.add(place.id())) {
				chosen.add(new RankedPlace(place, null));
			}
		}
		return List.copyOf(chosen);
	}
}
