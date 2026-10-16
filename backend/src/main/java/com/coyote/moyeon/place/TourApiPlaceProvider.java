package com.coyote.moyeon.place;

import com.coyote.moyeon.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 한국관광공사 국문 관광정보 TourAPI의 위치 기반 장소 공급자. */
@Component
@ConditionalOnProperty(name = "app.place.provider", havingValue = "tour-api")
public class TourApiPlaceProvider implements PlaceSearchProvider {

	private static final Logger log = LoggerFactory.getLogger(TourApiPlaceProvider.class);
	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final int MAX_RADIUS_METERS = 20000;
	private static final List<String> UNAVAILABLE_CODES = List.of("20", "22", "23", "30", "31");

	private final RestClient client;
	private final PlaceProperties.TourApi config;

	public TourApiPlaceProvider(RestClient tourApiRestClient, PlaceProperties properties) {
		this.client = tourApiRestClient;
		this.config = properties.getTourApi();
	}

	@Override
	public boolean isAvailable() {
		return config.getServiceKey() != null && !config.getServiceKey().isBlank();
	}

	@Override
	public List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter) {
		if (!isAvailable()) {
			throw unavailable();
		}
		Map<String, Object> params = commonParams();
		params.put("mapX", longitude);
		params.put("mapY", latitude);
		params.put("radius", Math.max(1, Math.min(MAX_RADIUS_METERS, radiusMeters)));
		params.put("arrange", "E");
		String contentType = contentType(filter);
		if (contentType != null) {
			params.put("contentTypeId", contentType);
		}
		return fetchPages("locationBasedList2", params, filter);
	}

	@Override
	public List<Place> findInBox(double minLongitude, double minLatitude, double maxLongitude, double maxLatitude,
			CategoryFilter filter) {
		double centerLat = (minLatitude + maxLatitude) / 2;
		double centerLon = (minLongitude + maxLongitude) / 2;
		int radius = (int) Math.ceil(GeoMath.distanceMeters(centerLat, centerLon, maxLatitude, maxLongitude));
		return findInRadius(centerLat, centerLon, radius, filter).stream()
				.filter(place -> place.longitude() >= minLongitude && place.longitude() <= maxLongitude
						&& place.latitude() >= minLatitude && place.latitude() <= maxLatitude)
				.toList();
	}

	@Override
	public Optional<Place> findById(String id) {
		if (!isAvailable()) {
			throw unavailable();
		}
		Map<String, Object> params = commonParams();
		params.put("contentId", id);
		return parse(call("detailCommon2", params), null).stream().filter(place -> id.equals(place.id())).findFirst();
	}

	private List<Place> fetchPages(String operation, Map<String, Object> base, CategoryFilter filter) {
		List<Place> places = new ArrayList<>();
		for (int page = 1; page <= config.getMaxPages(); page++) {
			Map<String, Object> params = new LinkedHashMap<>(base);
			params.put("numOfRows", config.getPageSize());
			params.put("pageNo", page);
			List<Place> received = parse(call(operation, params), filter);
			places.addAll(received);
			if (received.size() < config.getPageSize()) {
				break;
			}
		}
		return List.copyOf(places);
	}

	private Map<String, Object> commonParams() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("serviceKey", config.getServiceKey());
		params.put("MobileOS", config.getMobileOs());
		params.put("MobileApp", config.getMobileApp());
		params.put("_type", "json");
		return params;
	}

	private String call(String operation, Map<String, Object> params) {
		try {
			return client.get().uri(builder -> {
				builder.path("/" + operation);
				params.keySet().forEach(key -> builder.queryParam(key, "{" + key + "}"));
				return builder.build(params);
			}).retrieve().body(String.class);
		} catch (RestClientResponseException e) {
			log.warn("TourAPI HTTP 오류: operation={} status={}", operation, e.getStatusCode().value());
			throw e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403 ? unavailable() : failed();
		} catch (RestClientException e) {
			log.warn("TourAPI 호출 실패: operation={} cause={}", operation, e.getClass().getSimpleName());
			throw failed();
		}
	}

	private List<Place> parse(String body, CategoryFilter filter) {
		JsonNode root;
		try {
			root = JSON.readTree(body == null ? "" : body);
		} catch (RuntimeException e) {
			throw failed();
		}
		JsonNode response = root.path("response");
		String code = response.path("header").path("resultCode").asString("");
		if (!"0000".equals(code)) {
			String gatewayCode = root.path("resultCode").asString(code);
			log.warn("TourAPI 결과 코드: {}", gatewayCode);
			throw UNAVAILABLE_CODES.stream().anyMatch(gatewayCode::contains) ? unavailable() : failed();
		}
		JsonNode items = response.path("body").path("items").path("item");
		if (items.isObject()) {
			Place one = toPlace(items, filter);
			return one == null ? List.of() : List.of(one);
		}
		if (!items.isArray()) {
			return List.of();
		}
		List<Place> result = new ArrayList<>();
		for (JsonNode item : items) {
			Place place = toPlace(item, filter);
			if (place != null && matchesTourCategory(item, filter)) {
				result.add(place);
			}
		}
		return result;
	}

	private static Place toPlace(JsonNode item, CategoryFilter filter) {
		String id = item.path("contentid").asString("");
		String name = item.path("title").asString("").trim();
		JsonNode lat = item.path("mapy");
		JsonNode lon = item.path("mapx");
		if (id.isBlank() || name.isBlank() || !lat.isValueNode() || !lon.isValueNode()) {
			return null;
		}
		double latitude;
		double longitude;
		try {
			latitude = Double.parseDouble(lat.asString());
			longitude = Double.parseDouble(lon.asString());
		} catch (NumberFormatException e) {
			return null;
		}
		String type = item.path("contenttypeid").asString("");
		String large = "39".equals(type) ? "I2" : "R1";
		String medium = "28".equals(type) ? "R103" : large;
		String small = filter == null ? item.path("cat3").asString("") : filter.code();
		String category = filter == null ? tourTypeName(type) : filter.label();
		String address = item.path("addr1").asString("").trim();
		String detail = item.path("addr2").asString("").trim();
		if (!detail.isEmpty() && !address.contains(detail)) {
			address = (address + " " + detail).trim();
		}
		double distance = parseDouble(item.path("dist").asString("0"));
		return new Place(id, name, large, medium, small, category, address, latitude, longitude, distance);
	}

	private static boolean matchesTourCategory(JsonNode item, CategoryFilter filter) {
		if (filter == null || filter.level() == CategoryFilter.Level.LARGE) {
			return true;
		}
		String label = filter.label().replace(" ", "");
		String name = item.path("title").asString("").replace(" ", "");
		String cat3 = item.path("cat3").asString("");
		if (filter.code().equals("I21201")) {
			return cat3.equals("A05020900") || containsAny(name, "카페", "커피", "찻집", "다방");
		}
		if (filter.code().equals("I21001")) {
			return containsAny(name, "빵", "베이커리", "제과", "도넛");
		}
		return name.contains(label) || foodFamilyMatches(item.path("cat3").asString(""), filter.code());
	}

	private static boolean foodFamilyMatches(String cat3, String code) {
		return (code.startsWith("I201") && cat3.equals("A05020100"))
				|| (code.startsWith("I204") && cat3.equals("A05020200"))
				|| (code.startsWith("I203") && cat3.equals("A05020300"))
				|| (code.startsWith("I202") && cat3.equals("A05020400"));
	}

	private static boolean containsAny(String value, String... terms) {
		for (String term : terms) {
			if (value.contains(term)) return true;
		}
		return false;
	}

	private static String contentType(CategoryFilter filter) {
		if (filter == null) return null;
		return filter.code().startsWith("I") ? "39" : "28";
	}

	private static String tourTypeName(String type) {
		return switch (type) {
			case "12" -> "관광지";
			case "14" -> "문화시설";
			case "15" -> "행사";
			case "28" -> "레포츠";
			case "32" -> "숙박";
			case "38" -> "쇼핑";
			case "39" -> "음식점";
			default -> "관광정보";
		};
	}

	private static double parseDouble(String value) {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static BusinessException unavailable() {
		return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "PLACE_SEARCH_UNAVAILABLE",
				"장소 검색을 지금 사용할 수 없습니다.");
	}

	private static BusinessException failed() {
		return new BusinessException(HttpStatus.BAD_GATEWAY, "PLACE_SEARCH_FAILED",
				"장소 검색 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
	}
}
