package com.coyote.moyeon.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.coyote.moyeon.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class StorePublicDataProviderTest {

	private static final String BASE_URL = "https://apis.data.go.kr/B553077/api/open/sdsc2";
	private static final String KEY = "test+key/with=chars";

	private PlaceProperties properties;
	private MockRestServiceServer server;
	private StorePublicDataProvider provider;

	@BeforeEach
	void setUp() {
		properties = new PlaceProperties();
		properties.getDataGoKr().setServiceKey(KEY);
		properties.getDataGoKr().setBaseUrl(BASE_URL);
		properties.getDataGoKr().setPageSize(5);
		properties.getDataGoKr().setMaxPages(3);
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		provider = new StorePublicDataProvider(builder.build(), properties);
	}

	private static String item(String id, String name, String branch, double lat, double lon) {
		return """
				{"bizesId":"%s","bizesNm":"%s","brchNm":"%s","indsLclsCd":"I2","indsLclsNm":"음식",
				 "indsMclsCd":"I212","indsMclsNm":"비알코올 ","indsSclsCd":"I21201","indsSclsNm":"카페",
				 "rdnmAdr":"서울특별시 강남구 테헤란로 1","lnoAdr":"서울특별시 강남구 역삼동 1","lat":%s,"lon":%s}
				""".formatted(id, name, branch, lat, lon);
	}

	private static String page(String resultCode, int totalCount, String... items) {
		return """
				{"header":{"description":"설명","stdrYm":"202606","resultCode":"%s","resultMsg":"NORMAL SERVICE"},
				 "body":{"items":[%s],"numOfRows":2,"pageNo":1,"totalCount":%d}}
				""".formatted(resultCode, String.join(",", items), totalCount);
	}

	private void respondWith(String json) {
		server.expect(requestTo(containsString("/storeListInRadius"))).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
	}

	@Test
	void sendsTheExpectedRadiusQueryWithAnEncodedKey() {
		server.expect(requestTo(containsString("/storeListInRadius")))
				.andExpect(requestTo(containsString("serviceKey=test%2Bkey%2Fwith%3Dchars")))
				.andExpect(queryParam("radius", "500"))
				.andExpect(queryParam("cx", "127.0276"))
				.andExpect(queryParam("cy", "37.4979"))
				.andExpect(queryParam("indsSclsCd", "I21201"))
				.andExpect(queryParam("numOfRows", "5"))
				.andExpect(queryParam("pageNo", "1"))
				.andExpect(queryParam("type", "json"))
				.andRespond(withSuccess(page("00", 0), MediaType.APPLICATION_JSON));

		provider.findInRadius(37.4979, 127.0276, 500, CategoryFilter.small("I21201", "카페"));

		server.verify();
	}

	@Test
	void usesTheMatchingParameterForEachCategoryLevel() {
		for (String[] row : new String[][] { { "indsLclsCd", "I2" }, { "indsMclsCd", "I201" }, { "indsSclsCd", "I20101" } }) {
			respondWith(page("00", 0));
		}
		provider.findInRadius(37.5, 127.0, 500, CategoryFilter.large("I2", "음식"));
		provider.findInRadius(37.5, 127.0, 500, CategoryFilter.medium("I201", "한식"));
		provider.findInRadius(37.5, 127.0, 500, CategoryFilter.small("I20101", "백반"));
		server.verify();
	}

	@Test
	void omitsTheCategoryParameterWithoutAFilter() {
		server.expect(requestTo(containsString("/storeListInRadius")))
				.andExpect(requestTo(not(containsString("inds"))))
				.andRespond(withSuccess(page("00", 0), MediaType.APPLICATION_JSON));

		provider.findInRadius(37.5, 127.0, 500, null);

		server.verify();
	}

	@Test
	void sendsTheRectangleQuery() {
		server.expect(requestTo(containsString("/storeListInRectangle")))
				.andExpect(queryParam("minx", "126.9"))
				.andExpect(queryParam("miny", "37.4"))
				.andExpect(queryParam("maxx", "127.1"))
				.andExpect(queryParam("maxy", "37.6"))
				.andRespond(withSuccess(page("00", 0), MediaType.APPLICATION_JSON));

		provider.findInBox(126.9, 37.4, 127.1, 37.6, null);

		server.verify();
	}

	@Test
	void mapsTheFieldsOfAPlace() {
		respondWith(page("00", 2, item("A1", "벤티프레소", "강남점", 37.5, 127.1), item("A2", "바오밥나무커피", "", 37.6, 127.2)));

		List<Place> places = provider.findInRadius(37.5, 127.0, 500, null);

		assertThat(places).hasSize(2);
		Place first = places.get(0);
		assertThat(first.id()).isEqualTo("A1");
		assertThat(first.name()).isEqualTo("벤티프레소 강남점");
		assertThat(first.largeCategoryCode()).isEqualTo("I2");
		assertThat(first.mediumCategoryCode()).isEqualTo("I212");
		assertThat(first.smallCategoryCode()).isEqualTo("I21201");
		assertThat(first.categoryName()).isEqualTo("음식 > 비알코올 > 카페");
		assertThat(first.address()).isEqualTo("서울특별시 강남구 테헤란로 1");
		assertThat(first.latitude()).isEqualTo(37.5);
		assertThat(first.longitude()).isEqualTo(127.1);
		assertThat(places.get(1).name()).isEqualTo("바오밥나무커피");
	}

	@Test
	void doesNotRepeatTheBranchNameWhenItIsAlreadyInTheName() {
		respondWith(page("00", 1, item("A1", "벤티프레소강남점", "강남점", 37.5, 127.1)));

		assertThat(provider.findInRadius(37.5, 127.0, 500, null)).extracting(Place::name).containsExactly("벤티프레소강남점");
	}

	@Test
	void skipsItemsWithoutIdNameOrCoordinates() {
		String noCoordinates = "{\"bizesId\":\"X\",\"bizesNm\":\"좌표없음\"}";
		String noId = "{\"bizesNm\":\"번호없음\",\"lat\":37.5,\"lon\":127.0}";
		respondWith(page("00", 3, noCoordinates, noId, item("OK", "정상", "", 37.5, 127.0)));

		assertThat(provider.findInRadius(37.5, 127.0, 500, null)).extracting(Place::id).containsExactly("OK");
	}

	@Test
	void fetchesFollowingPagesUntilAShortPage() {
		properties.getDataGoKr().setPageSize(2);
		respondWith(page("00", 3, item("1", "가", "", 37.5, 127.0), item("2", "나", "", 37.5, 127.0)));
		respondWith(page("00", 3, item("3", "다", "", 37.5, 127.0)));

		assertThat(provider.findInRadius(37.5, 127.0, 500, null)).extracting(Place::id).containsExactly("1", "2", "3");
		server.verify();
	}

	@Test
	void stopsAtTheConfiguredMaximumNumberOfPages() {
		properties.getDataGoKr().setPageSize(2);
		for (int i = 0; i < 3; i++) {
			respondWith(page("00", 100, item("a" + i, "가" + i, "", 37.5, 127.0), item("b" + i, "나" + i, "", 37.5, 127.0)));
		}

		assertThat(provider.findInRadius(37.5, 127.0, 500, null)).hasSize(6);
		server.verify();
	}

	@Test
	void anEmptyItemListIsNotAnError() {
		respondWith("{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":\"\",\"totalCount\":0}}");

		assertThat(provider.findInRadius(37.5, 127.0, 500, null)).isEmpty();
	}

	@Test
	void quotaAndKeyProblemsAreReportedAsUnavailable() {
		String[] codes = { "20", "22", "30", "31" };
		for (String code : codes) {
			respondWith(page(code, 0));
		}
		for (int i = 0; i < codes.length; i++) {
			assertThatThrownBy(() -> provider.findInRadius(37.5, 127.0, 500, null))
					.isInstanceOfSatisfying(BusinessException.class, e -> {
						assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
						assertThat(e.getCode()).isEqualTo("PLACE_SEARCH_UNAVAILABLE");
					});
		}
	}

	@Test
	void otherResultCodesAndBrokenResponsesAreReportedAsBadGateway() {
		respondWith(page("10", 0));
		respondWith("이건 JSON이 아닙니다");
		server.expect(requestTo(containsString("/storeListInRadius"))).andRespond(withServerError());

		for (int i = 0; i < 3; i++) {
			assertThatThrownBy(() -> provider.findInRadius(37.5, 127.0, 500, null))
					.isInstanceOfSatisfying(BusinessException.class, e -> {
						assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
						assertThat(e.getCode()).isEqualTo("PLACE_SEARCH_FAILED");
						assertThat(e.getMessage()).doesNotContain(KEY).doesNotContain("serviceKey");
					});
		}
	}

	@Test
	void withoutAKeyTheProviderIsUnavailableAndMakesNoCall() {
		properties.getDataGoKr().setServiceKey("  ");

		assertThat(provider.isAvailable()).isFalse();
		assertThatThrownBy(() -> provider.findInRadius(37.5, 127.0, 500, null))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
		server.verify();
	}
}
