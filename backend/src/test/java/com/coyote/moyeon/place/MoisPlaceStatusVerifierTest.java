package com.coyote.moyeon.place;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MoisPlaceStatusVerifierTest {

	@Test
	void callsTheMoisApiAtSearchTime() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://apis.data.go.kr/1741000");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(request -> {
			assertThat(request.getURI().getPath()).isEqualTo("/1741000/general_restaurants/info");
			assertThat(request.getURI().getQuery()).contains("serviceKey=test-key", "BPLC_NM");
		}).andRespond(withSuccess(response("""
				[{"BPLC_NM":"폐업 식당","ROAD_NM_ADDR":"서울특별시 강남구 테헤란로 2",\
				"SALS_STTS_NM":"폐업","DTL_SALS_STTS_NM":"폐업"}]
				"""), MediaType.APPLICATION_JSON));
		PlaceProperties properties = new PlaceProperties();
		properties.getMois().setServiceKey("test-key");
		MoisPlaceStatusVerifier verifier = new MoisPlaceStatusVerifier(builder.build(), properties);

		try {
			assertThat(verifier.statusOf(place("폐업 식당", "서울특별시 강남구 테헤란로 2")))
					.isEqualTo(PlaceStatusVerifier.Status.CLOSED);
			server.verify();
		} finally {
			verifier.closeExecutor();
		}
	}

	@Test
	void exactOpenAddressWinsOverClosedHistory() {
		String response = response("""
				[{"BPLC_NM":"다시 연 가게","ROAD_NM_ADDR":"인천광역시 제물포구 능해길 76",\
				"SALS_STTS_NM":"폐업","DTL_SALS_STTS_NM":"폐업"},
				{"BPLC_NM":"다시 연 가게","ROAD_NM_ADDR":"인천광역시 제물포구 능해길 76 (1층)",\
				"SALS_STTS_NM":"영업/정상","DTL_SALS_STTS_NM":"영업"}]
				""");

		MoisPlaceStatusVerifier.Evaluation result = MoisPlaceStatusVerifier.evaluateResponse(response,
				place("다시연가게", "인천광역시 제물포구 능해길 76"));

		assertThat(result.exactNameFound()).isTrue();
		assertThat(result.status()).isEqualTo(PlaceStatusVerifier.Status.OPEN);
	}

	@Test
	void exactClosedAddressIsRejected() {
		String response = response("""
				[{"BPLC_NM":"폐업 식당","ROAD_NM_ADDR":"서울특별시 강남구 테헤란로 2",\
				"SALS_STTS_NM":"폐업","DTL_SALS_STTS_NM":"폐업"}]
				""");

		assertThat(MoisPlaceStatusVerifier.evaluateResponse(response,
				place("폐업식당", "서울 강남구 테헤란로 2 (역삼동)")).status())
				.isEqualTo(PlaceStatusVerifier.Status.CLOSED);
	}

	@Test
	void nameWithOnlyClosedRecordsRejectsAStaleAddress() {
		String response = response("""
				[{"BPLC_NM":"코리아숯불닭바베큐","ROAD_NM_ADDR":"인천광역시 미추홀구 한나루로 583",\
				"SALS_STTS_NM":"폐업","DTL_SALS_STTS_NM":"폐업"}]
				""");

		assertThat(MoisPlaceStatusVerifier.evaluateResponse(response,
				place("코리아숯불닭바베큐", "인천광역시 제물포구 등산로 76")).status())
				.isEqualTo(PlaceStatusVerifier.Status.CLOSED);
	}

	@Test
	void mixedNameHistoryUsesTheAddressToRejectAReplacedBusiness() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://apis.data.go.kr/1741000");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(request -> assertThat(request.getURI().getQuery()).contains("BPLC_NM"))
				.andRespond(withSuccess(response("""
						[{"BPLC_NM":"코리안숯불닭바베큐","ROAD_NM_ADDR":"인천광역시 남동구 독곡로48번길 42",\
						"SALS_STTS_NM":"영업/정상","DTL_SALS_STTS_NM":"영업"},
						{"BPLC_NM":"코리안숯불닭바베큐","LOTNO_ADDR":"인천광역시 제물포구 송현동 73-10",\
						"SALS_STTS_NM":"폐업","DTL_SALS_STTS_NM":"폐업"}]
						"""), MediaType.APPLICATION_JSON));
		server.expect(request -> assertThat(request.getURI().getQuery()).contains("ROAD_NM_ADDR"))
				.andRespond(withSuccess(response("""
						[{"BPLC_NM":"처갓집양념치킨 송림3점","ROAD_NM_ADDR":"인천광역시 제물포구 동산로 76-1, 1층",\
						"SALS_STTS_NM":"폐업","DTL_SALS_STTS_NM":"폐업"},
						{"BPLC_NM":"모미락(송림점)","ROAD_NM_ADDR":"인천광역시 제물포구 동산로 76, 1층 (송림동)",\
						"SALS_STTS_NM":"영업/정상","DTL_SALS_STTS_NM":"영업"}]
						"""), MediaType.APPLICATION_JSON));
		PlaceProperties properties = new PlaceProperties();
		properties.getMois().setServiceKey("test-key");
		MoisPlaceStatusVerifier verifier = new MoisPlaceStatusVerifier(builder.build(), properties);

		try {
			assertThat(verifier.statusOf(place("코리안숯불닭바베큐", "인천광역시 제물포구 동산로 76")))
					.isEqualTo(PlaceStatusVerifier.Status.CLOSED);
			server.verify();
		} finally {
			verifier.closeExecutor();
		}
	}

	@Test
	void multipleOtherBusinessesAtOneAddressRemainUnknown() {
		String response = response("""
				[{"BPLC_NM":"다른 식당 1","ROAD_NM_ADDR":"서울특별시 강남구 테헤란로 2, 1층",\
				"SALS_STTS_NM":"영업/정상","DTL_SALS_STTS_NM":"영업"},
				{"BPLC_NM":"다른 식당 2","ROAD_NM_ADDR":"서울특별시 강남구 테헤란로 2, 2층",\
				"SALS_STTS_NM":"영업/정상","DTL_SALS_STTS_NM":"영업"}]
				""");

		assertThat(MoisPlaceStatusVerifier.evaluateAddressResponse(response,
				place("후보 식당", "서울 강남구 테헤란로 2")).status())
				.isEqualTo(PlaceStatusVerifier.Status.UNKNOWN);
	}

	@Test
	void neighboringBuildingNumberIsNotTreatedAsTheSameAddress() {
		String response = response("""
				[{"BPLC_NM":"옆 건물 식당","ROAD_NM_ADDR":"인천광역시 제물포구 동산로 76-1, 1층",\
				"SALS_STTS_NM":"영업/정상","DTL_SALS_STTS_NM":"영업"}]
				""");

		assertThat(MoisPlaceStatusVerifier.evaluateAddressResponse(response,
				place("후보 식당", "인천광역시 제물포구 동산로 76")).status())
				.isEqualTo(PlaceStatusVerifier.Status.UNKNOWN);
	}

	@Test
	void unknownNameFailsOpen() {
		MoisPlaceStatusVerifier.Evaluation result = MoisPlaceStatusVerifier.evaluateResponse(response("[]"),
				place("새 가게", "서울특별시 강남구 테헤란로 3"));

		assertThat(result.exactNameFound()).isFalse();
		assertThat(result.status()).isEqualTo(PlaceStatusVerifier.Status.UNKNOWN);
	}

	private static String response(String items) {
		return "{\"response\":{\"header\":{\"resultCode\":\"0\",\"resultMsg\":\"정상\"},"
				+ "\"body\":{\"items\":{\"item\":" + items + "}}}}";
	}

	private static Place place(String name, String address) {
		return new Place("id", name, "I2", "I201", "I20101", "음식 > 한식", address, 37.5, 127.0, 0);
	}
}
