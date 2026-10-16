package com.coyote.moyeon.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TourApiPlaceProviderTest {

	private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2";
	private MockRestServiceServer server;
	private TourApiPlaceProvider provider;

	@BeforeEach
	void setUp() {
		PlaceProperties properties = new PlaceProperties();
		properties.getTourApi().setServiceKey("test+key/with=chars");
		properties.getTourApi().setBaseUrl(BASE_URL);
		properties.getTourApi().setPageSize(30);
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		provider = new TourApiPlaceProvider(builder.build(), properties);
	}

	@Test
	void searchesNearbyFoodByDistanceAndMapsResponse() {
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/locationBasedList2")))
				.andExpect(queryParam("mapX", "126.6462"))
				.andExpect(queryParam("mapY", "37.4743"))
				.andExpect(queryParam("radius", "2000"))
				.andExpect(queryParam("arrange", "E"))
				.andExpect(queryParam("contentTypeId", "39"))
				.andExpect(queryParam("MobileOS", "ETC"))
				.andExpect(queryParam("MobileApp", "COYOTE"))
				.andExpect(queryParam("_type", "json"))
				.andRespond(withSuccess(response(item("100", "개항로 카페", "39", "A05020900",
						"인천광역시 제물포구 개항로 1", "37.4744", "126.6463", "15")),
						MediaType.APPLICATION_JSON));

		List<Place> places = provider.findInRadius(37.4743, 126.6462, 2000,
				CategoryFilter.small("I21201", "카페"));

		assertThat(places).singleElement().satisfies(place -> {
			assertThat(place.id()).isEqualTo("100");
			assertThat(place.name()).isEqualTo("개항로 카페");
			assertThat(place.address()).isEqualTo("인천광역시 제물포구 개항로 1");
			assertThat(place.latitude()).isEqualTo(37.4744);
			assertThat(place.longitude()).isEqualTo(126.6463);
			assertThat(place.distanceMeters()).isEqualTo(15);
		});
		server.verify();
	}

	@Test
	void filtersOutNonCafeFoodForACafeSearch() {
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/locationBasedList2")))
				.andRespond(withSuccess(response(
						item("1", "관광공사 커피집", "39", "A05020900", "서울 1", "37.5", "127.0", "10"),
						item("2", "관광공사 한식당", "39", "A05020100", "서울 2", "37.5", "127.0", "20")),
						MediaType.APPLICATION_JSON));

		assertThat(provider.findInRadius(37.5, 127.0, 1000, CategoryFilter.small("I21201", "카페")))
				.extracting(Place::id).containsExactly("1");
	}

	@Test
	void loadsOnePlaceByContentId() {
		server.expect(requestTo(org.hamcrest.Matchers.containsString("/detailCommon2")))
				.andExpect(queryParam("contentId", "100"))
				.andRespond(withSuccess(response(item("100", "개항로 카페", "39", "A05020900",
						"인천광역시 제물포구 개항로 1", "37.4744", "126.6463", "")),
						MediaType.APPLICATION_JSON));

		assertThat(provider.findById("100")).hasValueSatisfying(place -> assertThat(place.name()).isEqualTo("개항로 카페"));
	}

	private static String response(String... items) {
		return """
				{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
				 "body":{"items":{"item":[%s]},"numOfRows":30,"pageNo":1,"totalCount":%d}}}
				""".formatted(String.join(",", items), items.length);
	}

	private static String item(String id, String title, String type, String cat3, String address,
			String latitude, String longitude, String distance) {
		return """
				{"contentid":"%s","title":"%s","contenttypeid":"%s","cat3":"%s",
				 "addr1":"%s","addr2":"","mapy":"%s","mapx":"%s","dist":"%s"}
				""".formatted(id, title, type, cat3, address, latitude, longitude, distance);
	}
}
