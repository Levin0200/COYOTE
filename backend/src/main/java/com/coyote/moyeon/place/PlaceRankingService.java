package com.coyote.moyeon.place;

import com.coyote.moyeon.ai.AiBudget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 후보 중에서 장소를 고른다. 후보 수가 표시 개수 이하더라도 AI가 모임 성격에 맞게 순서를 정하고 이유를 붙인다.
 * AI를 쓸 수 없거나(키 없음) 하루 상한을 넘었거나 호출이 실패하면 규칙 기반으로 대체한다.
 * AI가 고른 수가 모자라면 남은 자리는 규칙 기반으로 채운다.
 */
@Service
public class PlaceRankingService {

	private final LlmPlaceRanker llmRanker;
	private final RuleBasedPlaceRanker ruleRanker;
	private final AiBudget budget;

	public PlaceRankingService(LlmPlaceRanker llmRanker, RuleBasedPlaceRanker ruleRanker, AiBudget budget) {
		this.llmRanker = llmRanker;
		this.ruleRanker = ruleRanker;
		this.budget = budget;
	}

	public Ranking rank(RankRequest request) {
		List<Place> pool = request.candidates();
		int pick = Math.min(request.pick(), pool.size());
		if (pool.isEmpty() || !llmRanker.isAvailable() || !budget.tryAcquire()) {
			return new Ranking(ruleRanker.rank(pool, pick), false);
		}
		Optional<List<RankedPlace>> byAi = llmRanker.rank(request);
		if (byAi.isEmpty()) {
			return new Ranking(ruleRanker.rank(pool, pick), false);
		}
		List<RankedPlace> result = new ArrayList<>(byAi.get());
		if (result.size() < pick) {
			Set<String> chosen = new HashSet<>();
			result.forEach(ranked -> chosen.add(ranked.place().id()));
			List<Place> remaining = pool.stream().filter(place -> !chosen.contains(place.id())).toList();
			result.addAll(ruleRanker.rank(remaining, pick - result.size()));
		}
		return new Ranking(List.copyOf(result), true);
	}
}
