package com.coyote.moyeon.place.consensus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 군집에 지역 이름을 붙인다. 도로명주소에서 시·군·구를 뽑아 득표가 가장 많은 곳을 "강남구 일대" 형태로 쓴다.
 * "홍대입구역 일대" 같은 더 자연스러운 이름은 AI 단계에서 후보 이름을 보고 붙인다.
 */
public final class RegionLabeler {

	public static final String FALLBACK = "선택한 장소 주변";

	private RegionLabeler() {
	}

	public static String label(List<PlaceVote> members) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (PlaceVote member : members) {
			String area = areaOf(member.place().address());
			if (area != null) {
				counts.merge(area, member.votes(), Integer::sum);
			}
		}
		String best = null;
		int bestCount = 0;
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			if (entry.getValue() > bestCount) {
				best = entry.getKey();
				bestCount = entry.getValue();
			}
		}
		return best == null ? FALLBACK : best + " 일대";
	}

	static String areaOf(String address) {
		if (address == null || address.isBlank()) {
			return null;
		}
		String[] tokens = address.trim().split("\\s+");
		if (tokens.length < 2) {
			return tokens[0];
		}
		// 경기도 성남시 분당구 -> "성남시 분당구"
		if (tokens[1].endsWith("시") && tokens.length > 2 && tokens[2].endsWith("구")) {
			return tokens[1] + " " + tokens[2];
		}
		// 서울특별시 강남구, 경기도 가평군, 경기도 구리시 -> 두 번째 단어
		if (tokens[1].endsWith("구") || tokens[1].endsWith("군") || tokens[1].endsWith("시")) {
			return tokens[1];
		}
		// 세종특별자치시처럼 시·군·구가 따로 없으면 첫 단어
		return tokens[0];
	}
}
