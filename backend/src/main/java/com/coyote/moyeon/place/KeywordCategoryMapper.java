package com.coyote.moyeon.place;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 사용자가 입력한 키워드("조용한 카페, 한식")를 상권업종 코드로 바꾼다. 공공데이터 상가정보에는 이름 검색이 없고
 * 업종 코드 필터만 있기 때문이다. 코드는 상가정보 API(largeUpjongList 등)의 공식 분류에서 확인한 값이다.
 * 사전에 없는 단어("조용한", "주차 가능")는 무시하고 ignoredKeywords로 돌려준다(상호명 우선 정렬과 AI 요청 사항에만 쓴다).
 */
@Component
public class KeywordCategoryMapper {

	public static final CategoryFilter DEFAULT_FILTER = CategoryFilter.large("I2", "음식");

	public record Mapping(List<CategoryFilter> filters, List<String> ignoredKeywords) {
	}

	/** 긴 단어부터 맞춰 보기 위해 길이 내림차순으로 정렬해 둔다("스터디카페"가 "카페"보다 먼저 맞는다). */
	private static final List<Map.Entry<String, List<CategoryFilter>>> WORDS = buildWords();

	public Mapping map(String keyword) {
		Map<String, CategoryFilter> filters = new LinkedHashMap<>();
		List<String> ignored = new ArrayList<>();
		if (keyword != null) {
			for (String token : keyword.toLowerCase(Locale.ROOT).split("[\\s,./·]+")) {
				if (token.isBlank()) {
					continue;
				}
				List<CategoryFilter> matched = lookup(token);
				if (matched == null) {
					ignored.add(token);
				} else {
					matched.forEach(filter -> filters.putIfAbsent(filter.level() + filter.code(), filter));
				}
			}
		}
		return new Mapping(List.copyOf(filters.values()), List.copyOf(ignored));
	}

	private static List<CategoryFilter> lookup(String token) {
		for (Map.Entry<String, List<CategoryFilter>> entry : WORDS) {
			if (token.contains(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}

	private static List<Map.Entry<String, List<CategoryFilter>>> buildWords() {
		Map<String, List<CategoryFilter>> words = new LinkedHashMap<>();
		CategoryFilter cafe = CategoryFilter.small("I21201", "카페");
		CategoryFilter bakery = CategoryFilter.small("I21001", "빵/도넛");
		CategoryFilter iceCream = CategoryFilter.small("I21008", "아이스크림/빙수");
		CategoryFilter pork = CategoryFilter.small("I20107", "돼지고기 구이/찜");
		CategoryFilter beef = CategoryFilter.small("I20108", "소고기 구이/찜");
		CategoryFilter duck = CategoryFilter.small("I20110", "닭/오리고기 구이/찜");
		CategoryFilter offal = CategoryFilter.small("I20109", "곱창 전골/구이");
		CategoryFilter sashimi = CategoryFilter.small("I20111", "횟집");
		CategoryFilter seafood = CategoryFilter.small("I20112", "해산물 구이/찜");
		CategoryFilter sushi = CategoryFilter.small("I20301", "일식 회/초밥");
		CategoryFilter beer = CategoryFilter.small("I21103", "생맥주 전문");
		CategoryFilter pub = CategoryFilter.small("I21104", "요리 주점");

		put(words, List.of("음식", "식당", "맛집", "식사"), CategoryFilter.large("I2", "음식"));
		put(words, List.of("카페", "커피", "커피숍"), cafe);
		put(words, List.of("디저트"), cafe, bakery, iceCream);
		put(words, List.of("빵", "베이커리", "제과", "도넛"), bakery);
		put(words, List.of("빙수", "아이스크림"), iceCream);

		put(words, List.of("한식", "한국음식"), CategoryFilter.medium("I201", "한식"));
		put(words, List.of("백반", "한정식", "밥집"), CategoryFilter.small("I20101", "백반/한정식"));
		put(words, List.of("국밥", "찌개", "해장국", "탕"), CategoryFilter.small("I20102", "국/탕/찌개류"));
		put(words, List.of("칼국수", "국수"), CategoryFilter.small("I20105", "국수/칼국수"));
		put(words, List.of("냉면", "밀면"), CategoryFilter.small("I20106", "냉면/밀면"));
		put(words, List.of("부침개", "파전", "전집"), CategoryFilter.small("I20104", "전/부침개"));
		put(words, List.of("족발", "보쌈"), CategoryFilter.small("I20103", "족발/보쌈"));
		put(words, List.of("곱창", "막창"), offal);
		put(words, List.of("삼겹살", "돼지고기", "돼지갈비"), pork);
		put(words, List.of("소고기", "한우", "갈비", "불고기"), beef);
		put(words, List.of("고기", "고깃집", "구이", "바베큐", "바비큐"), pork, beef);
		put(words, List.of("닭갈비", "오리", "닭구이"), duck);
		put(words, List.of("횟집", "생선회"), sashimi, sushi);
		put(words, List.of("해산물", "조개"), seafood, sashimi);

		put(words, List.of("일식", "일본음식"), CategoryFilter.medium("I203", "일식"));
		put(words, List.of("초밥", "스시", "사시미"), sushi);
		put(words, List.of("돈가스", "돈까스", "카레", "덮밥"), CategoryFilter.small("I20302", "일식 카레/돈가스/덮밥"));
		put(words, List.of("라멘", "우동", "소바", "라면"), CategoryFilter.small("I20303", "일식 면 요리"));

		put(words, List.of("중식", "중국집", "중국음식", "짜장", "짬뽕", "탕수육"), CategoryFilter.medium("I202", "중식"));
		put(words, List.of("마라", "훠궈"), CategoryFilter.small("I20202", "마라탕/훠궈"));

		put(words, List.of("양식", "레스토랑", "서양식"), CategoryFilter.medium("I204", "서양식"));
		put(words, List.of("파스타", "스테이크"), CategoryFilter.small("I20402", "파스타/스테이크"));
		put(words, List.of("패밀리레스토랑"), CategoryFilter.small("I20403", "패밀리레스토랑"));
		put(words, List.of("동남아", "태국", "팟타이"), CategoryFilter.medium("I205", "동남아시아"));
		put(words, List.of("베트남", "쌀국수"), CategoryFilter.small("I20501", "베트남식 전문"));

		put(words, List.of("피자"), CategoryFilter.small("I21003", "피자"));
		put(words, List.of("치킨", "통닭"), CategoryFilter.small("I21006", "치킨"));
		put(words, List.of("버거", "햄버거"), CategoryFilter.small("I21004", "버거"));
		put(words, List.of("분식", "김밥", "떡볶이", "만두"), CategoryFilter.small("I21007", "김밥/만두/분식"));
		put(words, List.of("샌드위치", "샐러드", "토스트"), CategoryFilter.small("I21005", "토스트/샌드위치/샐러드"));
		put(words, List.of("뷔페"), CategoryFilter.small("I20702", "뷔페"));

		put(words, List.of("술집", "주점", "호프", "맥주", "이자카야", "포차", "펍", "와인바", "요리주점", "한잔"), beer, pub);

		put(words, List.of("스터디", "독서실", "스터디카페"), CategoryFilter.small("R10202", "독서실/스터디 카페"));
		put(words, List.of("노래방", "코인노래방"), CategoryFilter.small("R10407", "노래방"));
		put(words, List.of("볼링"), CategoryFilter.small("R10309", "볼링장"));
		put(words, List.of("당구"), CategoryFilter.small("R10310", "당구장"));
		put(words, List.of("탁구"), CategoryFilter.small("R10313", "탁구장"));
		put(words, List.of("골프"), CategoryFilter.small("R10311", "골프 연습장"));
		put(words, List.of("헬스", "피트니스"), CategoryFilter.small("R10307", "헬스장"));
		put(words, List.of("피시방", "pc방"), CategoryFilter.small("R10406", "PC방"));
		put(words, List.of("스포츠", "운동"), CategoryFilter.medium("R103", "스포츠 서비스"));
		put(words, List.of("오락", "게임", "놀거리"), CategoryFilter.medium("R104", "유원지·오락"));

		List<Map.Entry<String, List<CategoryFilter>>> sorted = new ArrayList<>(words.entrySet());
		sorted.sort(Comparator.comparingInt((Map.Entry<String, List<CategoryFilter>> e) -> e.getKey().length()).reversed());
		return List.copyOf(sorted);
	}

	private static void put(Map<String, List<CategoryFilter>> words, List<String> keys, CategoryFilter... filters) {
		keys.forEach(key -> words.put(key, List.of(filters)));
	}
}
