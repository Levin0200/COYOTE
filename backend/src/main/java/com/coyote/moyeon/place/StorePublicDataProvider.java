package com.coyote.moyeon.place;

import com.coyote.moyeon.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

/**
 * 소상공인시장진흥공단 상가(상권)정보 API(공공데이터포털) 구현체.
 * 인증키가 URL에 들어가므로 예외 메시지와 로그에는 요청 주소를 절대 남기지 않는다.
 */
@Component
@ConditionalOnProperty(name = "app.place.provider", havingValue = "public-data", matchIfMissing = true)
public class StorePublicDataProvider implements PlaceSearchProvider {

	private static final Logger log = LoggerFactory.getLogger(StorePublicDataProvider.class);
	private static final JsonMapper JSON = JsonMapper.builder().build();
	/** 22: 일일 호출 한도 초과, 20·30·31: 키 오류·미등록·만료. 서비스 쪽 문제라 "사용 불가"로 알린다. */
	private static final List<String> UNAVAILABLE_CODES = List.of("20", "22", "30", "31");

	private final RestClient client;
	private final PlaceProperties.DataGoKr config;

	public StorePublicDataProvider(RestClient placeRestClient, PlaceProperties properties) {
		this.client = placeRestClient;
		this.config = properties.getDataGoKr();
	}

	@Override
	public boolean isAvailable() {
		return config.getServiceKey() != null && !config.getServiceKey().isBlank();
	}

	@Override
	public List<Place> findInRadius(double latitude, double longitude, int radiusMeters, CategoryFilter filter) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("radius", radiusMeters);
		params.put("cx", longitude);
		params.put("cy", latitude);
		return fetchAll("storeListInRadius", params, filter);
	}

	@Override
	public List<Place> findInBox(double minLongitude, double minLatitude, double maxLongitude, double maxLatitude,
			CategoryFilter filter) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("minx", minLongitude);
		params.put("miny", minLatitude);
		params.put("maxx", maxLongitude);
		params.put("maxy", maxLatitude);
		return fetchAll("storeListInRectangle", params, filter);
	}

	private List<Place> fetchAll(String operation, Map<String, Object> base, CategoryFilter filter) {
		if (!isAvailable()) {
			throw unavailable();
		}
		List<Place> places = new ArrayList<>();
		for (int page = 1; page <= config.getMaxPages(); page++) {
			Map<String, Object> params = new LinkedHashMap<>(base);
			if (filter != null) {
				params.put(filterParameter(filter), filter.code());
			}
			params.put("numOfRows", config.getPageSize());
			params.put("pageNo", page);
			params.put("type", "json");
			params.put("serviceKey", config.getServiceKey());

			int received = parsePage(call(operation, params), places);
			if (received < config.getPageSize()) {
				break;
			}
		}
		return places;
	}

	private static String filterParameter(CategoryFilter filter) {
		return switch (filter.level()) {
			case LARGE -> "indsLclsCd";
			case MEDIUM -> "indsMclsCd";
			case SMALL -> "indsSclsCd";
		};
	}

	private String call(String operation, Map<String, Object> params) {
		try {
			// 값은 URI 변수로 넘겨 인증키의 + / = 같은 문자도 안전하게 인코딩한다.
			return client.get().uri(builder -> {
				builder.path("/" + operation);
				params.keySet().forEach(key -> builder.queryParam(key, "{" + key + "}"));
				return builder.build(params);
			}).retrieve().body(String.class);
		} catch (RestClientResponseException e) {
			log.warn("공공데이터 장소 검색 HTTP 오류: operation={} status={}", operation, e.getStatusCode().value());
			throw failed();
		} catch (RestClientException e) {
			// e.getMessage()에는 인증키가 든 요청 주소가 들어갈 수 있어 클래스 이름만 남긴다.
			log.warn("공공데이터 장소 검색 호출 실패: operation={} cause={}", operation, e.getClass().getSimpleName());
			throw failed();
		}
	}

	/** 한 페이지를 해석해 places에 더하고, 이번 페이지에서 받은 건수를 돌려준다. */
	private int parsePage(String body, List<Place> places) {
		JsonNode root;
		try {
			root = JSON.readTree(body == null ? "" : body);
		} catch (RuntimeException e) {
			log.warn("공공데이터 장소 검색 응답을 해석하지 못했습니다: {}", e.getClass().getSimpleName());
			throw failed();
		}
		String resultCode = root.path("header").path("resultCode").asString("");
		if (!"00".equals(resultCode)) {
			log.warn("공공데이터 장소 검색 결과 코드: {} {}", resultCode, root.path("header").path("resultMsg").asString(""));
			throw UNAVAILABLE_CODES.contains(resultCode) ? unavailable() : failed();
		}
		JsonNode items = root.path("body").path("items");
		if (!items.isArray()) {
			return 0;
		}
		int received = 0;
		for (JsonNode item : items) {
			received++;
			Place place = toPlace(item);
			if (place != null) {
				places.add(place);
			}
		}
		return received;
	}

	private static Place toPlace(JsonNode item) {
		JsonNode lat = item.path("lat");
		JsonNode lon = item.path("lon");
		String id = item.path("bizesId").asString("");
		String name = item.path("bizesNm").asString("").trim();
		if (id.isEmpty() || name.isEmpty() || !lat.isNumber() || !lon.isNumber()) {
			return null;
		}
		String branch = item.path("brchNm").asString("").trim();
		String displayName = branch.isEmpty() || name.contains(branch) ? name : name + " " + branch;
		String category = String.join(" > ", item.path("indsLclsNm").asString(""),
				item.path("indsMclsNm").asString("").trim(), item.path("indsSclsNm").asString(""));
		String address = item.path("rdnmAdr").asString("");
		if (address.isEmpty()) {
			address = item.path("lnoAdr").asString("");
		}
		return new Place(id, displayName, item.path("indsLclsCd").asString(""), item.path("indsMclsCd").asString(""),
				item.path("indsSclsCd").asString(""), category, address, lat.asDouble(), lon.asDouble(), 0);
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
