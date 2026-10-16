package com.coyote.moyeon.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** AI(OpenRouter) 설정. API 키는 환경변수(OPENROUTER_API_KEY)로만 받고 설정 파일에 값을 쓰지 않는다. */
@Component
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class AiProperties {

	/** openrouter(기본), fake(개발·테스트용 고정 응답), none(사용 안 함) */
	private String provider = "openrouter";
	private String apiKey = "";
	private String model = "deepseek/deepseek-v4.1-flash";
	/**
	 * 추론을 지원하는 모델이 답변 전에 추론 토큰을 쓰느라 출력 한도를 소진하지 않도록 추론 강도를 낮춘다(low/medium/high).
	 * 비워 두면 추론 옵션을 보내지 않는다. 추론 내용은 응답에서 제외한다.
	 */
	private String reasoningEffort = "low";
	private String baseUrl = "https://openrouter.ai/api/v1";
	private int connectTimeoutMillis = 3000;
	private int readTimeoutMillis = 12000;
	/** 한 번 호출에서 AI가 쓸 수 있는 최대 출력 토큰 */
	private int maxOutputTokens = 700;
	/** 사용자 한 명이 1시간 동안 AI 추천을 요청할 수 있는 횟수 */
	private int userHourlyLimit = 10;
	/** 서버 전체에서 하루에 AI를 호출할 수 있는 횟수(비용 상한) */
	private int dailyLimit = 300;
}
