package com.coyote.moyeon.place;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 장소 검색 설정. 인증키는 환경변수(DATA_GO_KR_SERVICE_KEY)로만 받고 설정 파일에 값을 쓰지 않는다. */
@Component
@ConfigurationProperties(prefix = "app.place")
@Getter
public class PlaceProperties {

	/** public-data(공공데이터 상가정보, 기본) 또는 fake(개발·테스트용 고정 데이터) */
	@Setter
	private String provider = "public-data";

	private final DataGoKr dataGoKr = new DataGoKr();
	private final TourApi tourApi = new TourApi();
	private final Mois mois = new Mois();

	@Getter
	@Setter
	public static class DataGoKr {
		private String serviceKey = "";
		private String baseUrl = "https://apis.data.go.kr/B553077/api/open/sdsc2";
		private int connectTimeoutMillis = 3000;
		private int readTimeoutMillis = 8000;
		/** 한 번에 받는 건수(API 최대 1000) */
		private int pageSize = 1000;
		/** 한 조회에서 이어서 받는 최대 페이지 수 */
		private int maxPages = 5;
	}

	@Getter
	@Setter
	public static class TourApi {
		private String serviceKey = "";
		private String baseUrl = "https://apis.data.go.kr/B551011/KorService2";
		private String mobileOs = "ETC";
		private String mobileApp = "COYOTE";
		private int connectTimeoutMillis = 3000;
		private int readTimeoutMillis = 8000;
		private int pageSize = 100;
		private int maxPages = 3;
	}

	@Getter
	@Setter
	public static class Mois {
		private boolean enabled = true;
		private String serviceKey = "";
		private String baseUrl = "https://apis.data.go.kr/1741000";
		private int connectTimeoutMillis = 2500;
		private int readTimeoutMillis = 7000;
		private int parallelism = 8;
		private long cacheTtlMillis = 60_000;
	}
}
