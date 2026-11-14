package com.coyote.moyeon.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 결과 생성기의 장소 결정 규칙(정책별). 외부 호출 없이 입력만으로 검증한다. */
class VoteCountResultGeneratorTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 17);
	private static final LocalDate DAY = TODAY.plusDays(3);

	private final VoteCountResultGenerator generator = new VoteCountResultGenerator();

	private static PlaceInfo place(String id, double lat, double lon) {
		return new PlaceInfo(id, "장소 " + id, "주소 " + id, "음식 > 한식", lat, lon);
	}

	private static ResultInput.ResponseSnapshot response(PlaceMode mode, PlaceInfo place) {
		return new ResultInput.ResponseSnapshot(Set.of(DAY), mode, place == null ? null : place.name(), place);
	}

	private static ResultInput input(PlacePolicyMode policy, List<PlaceOption> options, ResultInput.ResponseSnapshot... responses) {
		return new ResultInput(TODAY, policy, options, List.of(responses));
	}

	@Test
	void recommendWithoutAnyPlaceVoteIsLeftUndecided() {
		GeneratedResult result = generator.generate(input(PlacePolicyMode.RECOMMEND, List.of(),
				response(PlaceMode.OPEN, null), response(PlaceMode.OPEN, null)));

		assertEquals(PlaceStatus.NONE, result.placeStatus());
		assertTrue(result.selectedPlaces().isEmpty());
		assertTrue(result.candidates().isEmpty());
		assertEquals(DecisionType.MAJORITY, result.decisionType());
	}

	@Test
	void recommendWithASingleWinnerIsDecided() {
		PlaceInfo a = place("a", 37.50, 127.03);
		PlaceInfo b = place("b", 37.51, 127.03);
		GeneratedResult result = generator.generate(input(PlacePolicyMode.RECOMMEND, List.of(),
				response(PlaceMode.RECOMMEND, a), response(PlaceMode.RECOMMEND, a), response(PlaceMode.RECOMMEND, b)));

		assertEquals(PlaceStatus.DECIDED, result.placeStatus());
		assertEquals("장소 a", result.selectedPlaces().get(0).name());
		assertEquals(2, result.placeVoteCount());
	}

	@Test
	void tiedCandidatesKeepTheOrderTheHostGaveThem() {
		PlaceInfo a = place("a", 37.50, 127.03);
		PlaceInfo b = place("b", 37.51, 127.03);
		List<PlaceOption> options = List.of(PlaceOption.of(b, "1km"), PlaceOption.of(a, "2km"));
		GeneratedResult result = generator.generate(input(PlacePolicyMode.RECOMMEND, options,
				response(PlaceMode.RECOMMEND, a), response(PlaceMode.RECOMMEND, b)));

		assertEquals(PlaceStatus.AWAITING_HOST, result.placeStatus());
		assertEquals(DecisionType.TIE_BREAK, result.decisionType());
		assertEquals(List.of("장소 b", "장소 a"), result.candidates().stream().map(FinalResultCandidate::name).toList());
		assertTrue(result.selectedPlaces().isEmpty());
	}

	@Test
	void openWithScatteredProposalsWaitsForCandidates() {
		GeneratedResult result = generator.generate(input(PlacePolicyMode.OPEN, List.of(),
				response(PlaceMode.PROPOSE, place("a", 37.50, 127.03)),
				response(PlaceMode.PROPOSE, place("b", 37.51, 127.03))));

		assertEquals(PlaceStatus.PENDING, result.placeStatus());
		assertEquals(DecisionType.SPATIAL, result.decisionType());
		assertTrue(result.selectedPlaces().isEmpty());
	}

	@Test
	void openProposalsWithoutCoordinatesFallBackToNameVotes() {
		GeneratedResult result = generator.generate(input(PlacePolicyMode.OPEN, List.of(),
				new ResultInput.ResponseSnapshot(Set.of(DAY), PlaceMode.PROPOSE, "이름만 있는 장소", null)));

		assertEquals(PlaceStatus.DECIDED, result.placeStatus());
		assertEquals("이름만 있는 장소", result.selectedPlaces().get(0).name());
	}

	@Test
	void openWithoutProposalsWaitsForTheHostToSearchAndChoose() {
		GeneratedResult result = generator.generate(input(PlacePolicyMode.OPEN, List.of(), response(PlaceMode.OPEN, null)));

		assertEquals(PlaceStatus.AWAITING_HOST, result.placeStatus());
		assertTrue(result.candidates().isEmpty());
	}
}
