package com.coyote.moyeon.place;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RuleBasedPlaceRankerTest {

	private final RuleBasedPlaceRanker ranker = new RuleBasedPlaceRanker();

	@Test
	void keepsTheNearestFirstButLimitsEachCategoryToTwo() {
		// 가까운 5곳이 모두 카페(A)이고 그 뒤에 다른 업종이 있다.
		List<Place> pool = List.of(RankTestPlaces.place(1, "A", "카페", 100), RankTestPlaces.place(2, "A", "카페", 200),
				RankTestPlaces.place(3, "A", "카페", 300), RankTestPlaces.place(4, "A", "카페", 400),
				RankTestPlaces.place(5, "B", "한식", 500), RankTestPlaces.place(6, "C", "분식", 600));

		List<RankedPlace> result = ranker.rank(pool, 4);

		assertThat(result).extracting(r -> r.place().id()).containsExactly("p1", "p2", "p5", "p6");
		assertThat(result).extracting(RankedPlace::reason).containsOnlyNulls();
	}

	@Test
	void fillsTheRemainingSlotsWithTheNearestWhenVarietyRunsOut() {
		List<Place> pool = RankTestPlaces.pool(6, "A");

		assertThat(ranker.rank(pool, 5)).extracting(r -> r.place().id()).containsExactly("p1", "p2", "p3", "p4", "p5");
	}

	@Test
	void returnsEverythingWhenThereAreFewerCandidatesThanRequested() {
		List<Place> pool = RankTestPlaces.pool(3, "A", "B");

		assertThat(ranker.rank(pool, 6)).hasSize(3);
		assertThat(ranker.rank(List.of(), 3)).isEmpty();
	}

	@Test
	void neverReturnsTheSamePlaceTwice() {
		List<Place> pool = RankTestPlaces.pool(10, "A", "B", "C");

		assertThat(ranker.rank(pool, 6)).extracting(r -> r.place().id()).doesNotHaveDuplicates();
	}
}
