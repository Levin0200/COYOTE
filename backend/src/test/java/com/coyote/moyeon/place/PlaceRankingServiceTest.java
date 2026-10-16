package com.coyote.moyeon.place;

import static org.assertj.core.api.Assertions.assertThat;

import com.coyote.moyeon.ai.AiBudget;
import com.coyote.moyeon.ai.AiProperties;
import com.coyote.moyeon.ai.LlmClient;
import com.coyote.moyeon.ai.LlmException;
import com.coyote.moyeon.common.ratelimit.InMemoryRateLimiter;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceRankingServiceTest {

	private static class StubLlm implements LlmClient {
		boolean available = true;
		String answer = "";
		int calls;

		@Override
		public boolean isAvailable() {
			return available;
		}

		@Override
		public String complete(String system, String user, int maxOutputTokens) {
			calls++;
			if (answer == null) {
				throw new LlmException("실패");
			}
			return answer;
		}
	}

	private final StubLlm llm = new StubLlm();
	private final AiProperties properties = new AiProperties();
	private final PlaceRankingService service = new PlaceRankingService(new LlmPlaceRanker(llm, properties),
			new RuleBasedPlaceRanker(), new AiBudget(new InMemoryRateLimiter(), properties));
	private final List<Place> pool = RankTestPlaces.pool(12, "A", "B", "C");

	private Ranking rank(int pick) {
		return service.rank(new RankRequest("모임", "설명", 4, pool, pick));
	}

	@Test
	void aiChoosesWhenThereAreMoreCandidatesThanSlots() {
		llm.answer = "{\"picks\":[{\"n\":5,\"why\":\"좋아요\"},{\"n\":2},{\"n\":9},{\"n\":1},{\"n\":7},{\"n\":3}]}";

		Ranking ranking = rank(6);

		assertThat(ranking.aiUsed()).isTrue();
		assertThat(ranking.places()).extracting(r -> r.place().id()).containsExactly("p5", "p2", "p9", "p1", "p7", "p3");
		assertThat(ranking.places().get(0).reason()).isEqualTo("좋아요");
		assertThat(llm.calls).isEqualTo(1);
	}

	@Test
	void whenAiReturnsTooFewTheRestIsFilledByTheRules() {
		llm.answer = "{\"picks\":[{\"n\":8},{\"n\":4}]}";

		Ranking ranking = rank(5);

		assertThat(ranking.aiUsed()).isTrue();
		assertThat(ranking.places()).hasSize(5);
		assertThat(ranking.places()).extracting(r -> r.place().id()).startsWith("p8", "p4").doesNotHaveDuplicates();
	}

	@Test
	void noAiCallIsMadeWhenTheCandidatesAreNotMoreThanTheSlots() {
		Ranking ranking = service.rank(new RankRequest("모임", "설명", 4, RankTestPlaces.pool(6, "A"), 6));

		assertThat(ranking.aiUsed()).isFalse();
		assertThat(ranking.places()).hasSize(6);
		assertThat(llm.calls).isZero();
	}

	@Test
	void rulesAreUsedWhenAiIsNotAvailable() {
		llm.available = false;

		Ranking ranking = rank(6);

		assertThat(ranking.aiUsed()).isFalse();
		assertThat(ranking.places()).hasSize(6);
		assertThat(llm.calls).isZero();
	}

	@Test
	void rulesAreUsedWhenAiFailsOrAnswersNonsense() {
		llm.answer = null;
		assertThat(rank(6).aiUsed()).isFalse();
		llm.answer = "죄송하지만 도와드릴 수 없습니다.";
		Ranking ranking = rank(6);
		assertThat(ranking.aiUsed()).isFalse();
		assertThat(ranking.places()).hasSize(6);
		assertThat(llm.calls).isEqualTo(2);
	}

	@Test
	void theDailyBudgetStopsAiCallsButNotTheService() {
		properties.setDailyLimit(2);
		llm.answer = "{\"picks\":[{\"n\":1}]}";

		assertThat(rank(3).aiUsed()).isTrue();
		assertThat(rank(3).aiUsed()).isTrue();
		Ranking over = rank(3);

		assertThat(over.aiUsed()).isFalse();
		assertThat(over.places()).hasSize(3);
		assertThat(llm.calls).isEqualTo(2);
	}
}
