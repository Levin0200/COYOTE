package com.coyote.moyeon.place;

import jakarta.annotation.PreDestroy;
import java.text.Normalizer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 행정안전부 일반음식점·휴게음식점 API를 검색 요청 시 실시간 조회해 폐업 장소를 제외한다.
 *
 * <p>후보를 병렬 조회하고 아주 짧은 메모리 캐시만 사용한다. 파일이나 DB에는 API 응답을 저장하지 않는다.
 * 외부 API 장애나 매칭 실패는 UNKNOWN으로 처리해 장소 검색 자체가 중단되지 않게 한다.</p>
 */
@Component
public class MoisPlaceStatusVerifier implements PlaceStatusVerifier {

	private static final Logger log = LoggerFactory.getLogger(MoisPlaceStatusVerifier.class);
	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final Pattern PARENTHESIZED = Pattern.compile("\\([^)]*\\)|（[^）]*）");
	private static final Pattern ROAD = Pattern.compile("([가-힣A-Za-z0-9·.]+(?:대로|로|길))\\s*(\\d+(?:-\\d+)?)");
	private static final List<String> CLOSED_WORDS = List.of("폐업", "휴업", "취소", "말소", "정지", "종료");
	private static final List<String> OPEN_WORDS = List.of("영업", "정상", "재개");

	private final RestClient client;
	private final PlaceProperties.Mois config;
	private final String serviceKey;
	private final ExecutorService executor;
	private final Clock clock;
	private final ConcurrentHashMap<String, CachedStatus> cache = new ConcurrentHashMap<>();

	@Autowired
	public MoisPlaceStatusVerifier(@Qualifier("moisRestClient") RestClient client, PlaceProperties properties) {
		this(client, properties, Clock.systemUTC());
	}

	MoisPlaceStatusVerifier(RestClient client, PlaceProperties properties, Clock clock) {
		this.client = client;
		this.config = properties.getMois();
		this.serviceKey = firstNonBlank(config.getServiceKey(), properties.getDataGoKr().getServiceKey());
		this.clock = clock;
		this.executor = Executors.newFixedThreadPool(Math.max(1, config.getParallelism()),
				Thread.ofVirtual().name("mois-place-status-", 0).factory());
	}

	@Override
	public Status statusOf(Place place) {
		if (!isAvailable() || !isFood(place)) {
			return Status.UNKNOWN;
		}
		Industry primary = primaryIndustry(place);
		String cacheKey = primary.name() + '|' + normalize(place.name()) + '|' + normalizeRegionalAddress(place.address());
		long now = clock.millis();
		CachedStatus cached = cache.get(cacheKey);
		if (cached != null && cached.expiresAtMillis() > now) {
			return cached.status();
		}

		Status status = query(primary, place);
		cache.put(cacheKey, new CachedStatus(status, now + Math.max(1, config.getCacheTtlMillis())));
		if (cache.size() > 5_000) {
			cache.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() <= now);
		}
		return status;
	}

	@Override
	public Map<Place, Status> statusesOf(Collection<Place> places) {
		Map<Place, Status> result = new ConcurrentHashMap<>();
		List<CompletableFuture<Void>> tasks = new ArrayList<>();
		for (Place place : places) {
			if (!isFood(place) || !isAvailable()) {
				result.put(place, Status.UNKNOWN);
				continue;
			}
			tasks.add(CompletableFuture.runAsync(() -> result.put(place, statusOf(place)), executor));
		}
		CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
		return Map.copyOf(result);
	}

	private Status query(Industry primary, Place place) {
		try {
			Industry matchedIndustry = primary;
			Evaluation nameEvaluation = evaluateResponse(callByName(primary, place.name()), place);
			if (!nameEvaluation.exactNameFound()) {
				matchedIndustry = primary.other();
				nameEvaluation = evaluateResponse(callByName(matchedIndustry, place.name()), place);
			}
			if (nameEvaluation.status() != Status.UNKNOWN || !nameEvaluation.exactNameFound()
					|| place.address() == null || place.address().isBlank()) {
				return nameEvaluation.status();
			}

			// 같은 상호의 영업점과 폐업점이 전국에 함께 있으면 이름만으로는 현재 지점을 판별할 수 없다.
			// 이때 현재 주소를 한 번 더 조회해, 그 자리를 다른 단일 업소가 사용 중이면 오래된 후보로 판단한다.
			return evaluateAddressResponse(callByAddress(matchedIndustry, place.address()), place).status();
		} catch (RestClientResponseException e) {
			log.warn("행안부 영업상태 API HTTP 오류: status={}", e.getStatusCode().value());
			return Status.UNKNOWN;
		} catch (RuntimeException e) {
			log.warn("행안부 영업상태 API 호출 실패: cause={}", e.getClass().getSimpleName());
			return Status.UNKNOWN;
		}
	}

	private String callByName(Industry industry, String businessName) {
		return client.get().uri(builder -> builder.path('/' + industry.path + "/info")
				.queryParam("serviceKey", "{serviceKey}")
				.queryParam("pageNo", "1")
				.queryParam("numOfRows", "100")
				.queryParam("returnType", "JSON")
				.queryParam("cond[BPLC_NM::LIKE]", "{businessName}")
				.build(Map.of("serviceKey", serviceKey, "businessName", businessName)))
				.retrieve().body(String.class);
	}

	private String callByAddress(Industry industry, String address) {
		return client.get().uri(builder -> builder.path('/' + industry.path + "/info")
				.queryParam("serviceKey", "{serviceKey}")
				.queryParam("pageNo", "1")
				.queryParam("numOfRows", "100")
				.queryParam("returnType", "JSON")
				.queryParam("cond[ROAD_NM_ADDR::LIKE]", "{address}")
				.build(Map.of("serviceKey", serviceKey, "address", address)))
				.retrieve().body(String.class);
	}

	static Evaluation evaluateResponse(String body, Place place) {
		List<JsonNode> items = parseItems(body);
		String expectedName = normalize(place.name());
		String expectedAddress = normalizeRegionalAddress(place.address());
		boolean exactNameFound = false;
		boolean openByName = false;
		boolean closedByName = false;
		boolean openByAddress = false;
		boolean closedByAddress = false;
		for (JsonNode item : items) {
			if (!expectedName.equals(normalize(item.path("BPLC_NM").asString("")))) {
				continue;
			}
			exactNameFound = true;
			Status status = classify(item.path("SALS_STTS_NM").asString("") + ' '
					+ item.path("DTL_SALS_STTS_NM").asString(""));
			openByName |= status == Status.OPEN;
			closedByName |= status == Status.CLOSED;
			String address = item.path("ROAD_NM_ADDR").asString("");
			if (address.isBlank()) {
				address = item.path("LOTNO_ADDR").asString("");
			}
			if (!expectedAddress.isEmpty() && expectedAddress.equals(normalizeRegionalAddress(address))) {
				openByAddress |= status == Status.OPEN;
				closedByAddress |= status == Status.CLOSED;
			}
		}

		if (openByAddress) {
			return new Evaluation(true, Status.OPEN);
		}
		if (closedByAddress) {
			return new Evaluation(true, Status.CLOSED);
		}
		// 같은 상호가 전국 어디에서도 영업 중이지 않고 폐업 기록만 있으면 오래된 후보로 본다.
		if (exactNameFound && closedByName && !openByName) {
			return new Evaluation(true, Status.CLOSED);
		}
		return new Evaluation(exactNameFound, Status.UNKNOWN);
	}

	/**
	 * 같은 상호가 다른 지역에서 영업 중이라 이름 조회만으로 결론을 못 낸 경우의 보조 판정이다.
	 * 정확히 같은 도로명 주소에 다른 영업 중 업소가 하나만 있으면 해당 후보는 교체된 오래된 상가 데이터다.
	 * 한 건물에 여러 업소가 영업 중이면 복합 건물일 수 있으므로 UNKNOWN으로 열어 둔다.
	 */
	static AddressEvaluation evaluateAddressResponse(String body, Place place) {
		List<JsonNode> items = parseItems(body);
		String expectedName = normalize(place.name());
		String expectedAddress = normalizeRegionalAddress(place.address());
		boolean exactNameOpen = false;
		boolean exactNameClosed = false;
		Set<String> otherOpenBusinesses = new HashSet<>();
		for (JsonNode item : items) {
			String address = item.path("ROAD_NM_ADDR").asString("");
			if (address.isBlank() || !expectedAddress.equals(normalizeRegionalAddress(address))) {
				continue;
			}
			Status status = classify(item.path("SALS_STTS_NM").asString("") + ' '
					+ item.path("DTL_SALS_STTS_NM").asString(""));
			if (expectedName.equals(normalize(item.path("BPLC_NM").asString("")))) {
				exactNameOpen |= status == Status.OPEN;
				exactNameClosed |= status == Status.CLOSED;
			} else if (status == Status.OPEN) {
				otherOpenBusinesses.add(normalize(item.path("BPLC_NM").asString("")) + '|' + expectedAddress);
			}
		}
		int otherOpenCount = otherOpenBusinesses.size();
		if (exactNameOpen) {
			return new AddressEvaluation(Status.OPEN, otherOpenCount);
		}
		if (exactNameClosed || otherOpenCount == 1) {
			return new AddressEvaluation(Status.CLOSED, otherOpenCount);
		}
		return new AddressEvaluation(Status.UNKNOWN, otherOpenCount);
	}

	private static List<JsonNode> parseItems(String body) {
		JsonNode root = JSON.readTree(body == null ? "" : body);
		String resultCode = root.path("response").path("header").path("resultCode").asString("");
		if (!"0".equals(resultCode) && !"00".equals(resultCode)) {
			throw new IllegalStateException("MOIS result code: " + resultCode);
		}
		JsonNode itemNode = root.path("response").path("body").path("items").path("item");
		List<JsonNode> items = new ArrayList<>();
		if (itemNode.isArray()) {
			itemNode.forEach(items::add);
		} else if (!itemNode.isMissingNode() && !itemNode.isNull()) {
			items.add(itemNode);
		}
		return items;
	}

	private boolean isAvailable() {
		return config.isEnabled() && serviceKey != null && !serviceKey.isBlank();
	}

	private static boolean isFood(Place place) {
		return place != null && "I2".equalsIgnoreCase(place.largeCategoryCode());
	}

	private static Industry primaryIndustry(Place place) {
		String category = normalize(place.categoryName());
		boolean cafe = "I212".equalsIgnoreCase(place.mediumCategoryCode()) || category.contains("카페")
				|| category.contains("커피") || category.contains("음료") || category.contains("제과")
				|| category.contains("빵");
		return cafe ? Industry.REST_CAFES : Industry.GENERAL_RESTAURANTS;
	}

	private static Status classify(String value) {
		String normalized = normalize(value);
		if (CLOSED_WORDS.stream().anyMatch(normalized::contains)) {
			return Status.CLOSED;
		}
		return OPEN_WORDS.stream().anyMatch(normalized::contains) ? Status.OPEN : Status.UNKNOWN;
	}

	/** 도로명과 건물 번호뿐 아니라 시·군·구도 포함해 서로 다른 지역의 동명 도로를 구분한다. */
	private static String normalizeRegionalAddress(String address) {
		if (address == null || address.isBlank()) {
			return "";
		}
		String withoutDetails = PARENTHESIZED.matcher(address).replaceAll(" ");
		Matcher road = ROAD.matcher(withoutDetails);
		if (!road.find()) {
			return normalize(withoutDetails);
		}
		String administrativeArea = normalize(withoutDetails.substring(0, road.start()))
				.replace("특별자치도", "").replace("특별자치시", "")
				.replace("특별시", "").replace("광역시", "")
				.replace("도", "").replace("시", "").replace("군", "").replace("구", "");
		return administrativeArea + normalize(road.group(1) + road.group(2));
	}

	private static String normalize(String value) {
		if (value == null) {
			return "";
		}
		return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
				.replaceAll("[^가-힣a-z0-9]", "");
	}

	private static String firstNonBlank(String first, String second) {
		return first != null && !first.isBlank() ? first : second;
	}

	@PreDestroy
	void closeExecutor() {
		executor.shutdown();
	}

	record Evaluation(boolean exactNameFound, Status status) {
	}

	record AddressEvaluation(Status status, int otherOpenCount) {
	}

	private record CachedStatus(Status status, long expiresAtMillis) {
	}

	private enum Industry {
		GENERAL_RESTAURANTS("general_restaurants"),
		REST_CAFES("rest_cafes");

		private final String path;

		Industry(String path) {
			this.path = path;
		}

		Industry other() {
			return this == GENERAL_RESTAURANTS ? REST_CAFES : GENERAL_RESTAURANTS;
		}
	}
}
