package com.coyote.moyeon.place;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class KeywordCategoryMapperTest {

	private final KeywordCategoryMapper mapper = new KeywordCategoryMapper();

	private static List<String> codes(KeywordCategoryMapper.Mapping mapping) {
		return mapping.filters().stream().map(CategoryFilter::code).toList();
	}

	@Test
	void mapsKnownWordsAndKeepsUnknownOnesAsIgnored() {
		KeywordCategoryMapper.Mapping mapping = mapper.map("조용한 카페, 한식");

		assertThat(mapping.filters()).extracting(CategoryFilter::label).containsExactly("카페", "한식");
		assertThat(codes(mapping)).containsExactly("I21201", "I201");
		assertThat(mapping.filters().get(0).level()).isEqualTo(CategoryFilter.Level.SMALL);
		assertThat(mapping.filters().get(1).level()).isEqualTo(CategoryFilter.Level.MEDIUM);
		assertThat(mapping.ignoredKeywords()).containsExactly("조용한");
	}

	@Test
	void longerWordsWinOverShorterOnesInsideTheToken() {
		assertThat(codes(mapper.map("스터디카페"))).containsExactly("R10202");
		assertThat(codes(mapper.map("쌀국수"))).containsExactly("I20501");
		assertThat(codes(mapper.map("칼국수"))).containsExactly("I20105");
		assertThat(codes(mapper.map("삼겹살"))).containsExactly("I20107");
		assertThat(codes(mapper.map("마라탕"))).containsExactly("I20202");
	}

	@Test
	void oneWordCanMapToSeveralCategories() {
		assertThat(codes(mapper.map("고기"))).containsExactly("I20107", "I20108");
		assertThat(codes(mapper.map("술집"))).containsExactly("I21103", "I21104");
		assertThat(codes(mapper.map("디저트"))).containsExactly("I21201", "I21001", "I21008");
	}

	@Test
	void repeatedCategoriesAreOnlyAppliedOnce() {
		assertThat(codes(mapper.map("카페 커피 커피숍"))).containsExactly("I21201");
	}

	@Test
	void wordsInsideLongerTokensStillMatch() {
		assertThat(codes(mapper.map("분위기좋은카페에서"))).containsExactly("I21201");
	}

	@Test
	void unknownBrandNamesAreKeptForNameMatching() {
		KeywordCategoryMapper.Mapping mapping = mapper.map("스타벅스");

		assertThat(mapping.filters()).isEmpty();
		assertThat(mapping.ignoredKeywords()).containsExactly("스타벅스");
	}

	@Test
	void blankKeywordMapsToNothing() {
		for (String keyword : new String[] { null, "", "   ", " , / " }) {
			KeywordCategoryMapper.Mapping mapping = mapper.map(keyword);
			assertThat(mapping.filters()).isEmpty();
			assertThat(mapping.ignoredKeywords()).isEmpty();
		}
	}

	@Test
	void mixedCaseAndPunctuationAreNormalized() {
		assertThat(codes(mapper.map("PC방/노래방"))).containsExactly("R10406", "R10407");
	}
}
