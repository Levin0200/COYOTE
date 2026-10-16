package com.coyote.moyeon.meeting;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 득표 집계 기본 생성기. 날짜 동률은 오늘과 가장 가까운 하나, 장소 동률은 전부 선택한다. */
@Component
public class VoteCountResultGenerator implements FinalResultGenerator {

	@Override
	public GeneratedResult generate(ResultInput input) {
		Map<LocalDate, Integer> dateCounts = new HashMap<>();
		// 장소는 장소 ID가 있으면 ID로, 없으면(이름만 받은 응답) 이름으로 센다.
		Map<String, Integer> placeCounts = new HashMap<>();
		Map<String, SelectedPlace> placesByKey = new HashMap<>();
		for (ResultInput.ResponseSnapshot response : input.responses()) {
			response.dates().forEach(date -> dateCounts.merge(date, 1, Integer::sum));
			String place = response.placePreference();
			if (place != null && response.placeMode() != PlaceMode.OPEN) {
				PlaceInfo info = response.place();
				String key = info != null && info.id() != null ? "id:" + info.id() : "name:" + place;
				placeCounts.merge(key, 1, Integer::sum);
				placesByKey.putIfAbsent(key, toSelectedPlace(place, info));
			}
		}
		if (dateCounts.isEmpty()) {
			throw new IllegalStateException("결과를 계산할 날짜 응답이 없습니다.");
		}

		int bestDateCount = dateCounts.values().stream().max(Integer::compare).orElseThrow();
		List<LocalDate> tiedDates = dateCounts.entrySet().stream()
				.filter(e -> e.getValue() == bestDateCount).map(Map.Entry::getKey).sorted().toList();
		LocalDate selectedDate = tiedDates.stream()
				.min(Comparator.comparingLong((LocalDate d) -> Math.abs(ChronoUnit.DAYS.between(input.today(), d)))
						.thenComparing(Comparator.naturalOrder()))
				.orElseThrow();

		int bestPlaceCount = placeCounts.values().stream().max(Integer::compare).orElse(0);
		List<SelectedPlace> selectedPlaces = bestPlaceCount == 0 ? List.of()
				: placeCounts.entrySet().stream().filter(e -> e.getValue() == bestPlaceCount)
						.map(e -> placesByKey.get(e.getKey()))
						.sorted(Comparator.comparing(SelectedPlace::name)
								.thenComparing(place -> place.placeId() == null ? "" : place.placeId()))
						.toList();

		boolean tieBreak = tiedDates.size() > 1 || selectedPlaces.size() > 1;
		String dateReason = tiedDates.size() > 1
				? "날짜 공동 1위 중 오늘과 가장 가까운 날짜를 우선했습니다. "
				: "가장 많은 참여자가 선택한 날짜를 반영했습니다. ";
		String placeReason = selectedPlaces.size() > 1 ? "장소 공동 1위 선택지는 모두 표시합니다."
				: selectedPlaces.size() == 1 ? "가장 많은 참여자가 선택한 장소를 반영했습니다."
				: "유효한 장소 응답이 없어 장소는 추후 협의로 남겼습니다.";

		return new GeneratedResult(selectedDate, bestDateCount, selectedPlaces, bestPlaceCount,
				input.responses().size(), tieBreak ? DecisionType.TIE_BREAK : DecisionType.MAJORITY,
				dateReason + placeReason);
	}

	private static SelectedPlace toSelectedPlace(String name, PlaceInfo info) {
		return info == null ? SelectedPlace.ofName(name)
				: new SelectedPlace(name, info.id(), info.address(), info.latitude(), info.longitude());
	}
}
