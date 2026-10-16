package com.coyote.moyeon.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * OpenRouter(OpenAI 호환 chat completions) 구현체. 인증키는 Authorization 헤더에만 넣고,
 * 로그와 예외 메시지에는 요청 내용·응답 본문·키를 남기지 않는다(사용자가 쓴 모임 설명이 들어 있을 수 있다).
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "openrouter", matchIfMissing = true)
public class OpenRouterClient implements LlmClient {

	private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final RestClient client;
	private final AiProperties properties;

	public OpenRouterClient(@Qualifier("aiRestClient") RestClient aiRestClient, AiProperties properties) {
		this.client = aiRestClient;
		this.properties = properties;
	}

	@Override
	public boolean isAvailable() {
		return properties.getApiKey() != null && !properties.getApiKey().isBlank();
	}

	@Override
	public String complete(String system, String user, int maxOutputTokens) {
		if (!isAvailable()) {
			throw new LlmException("AI 키가 설정되지 않았습니다.");
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", properties.getModel());
		body.put("messages", List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)));
		body.put("max_tokens", maxOutputTokens);
		body.put("temperature", 0.2);
		body.put("response_format", Map.of("type", "json_object"));
		String effort = properties.getReasoningEffort();
		if (effort != null && !effort.isBlank()) {
			// 추론을 지원하지 않는 모델에서는 OpenRouter가 이 값을 무시한다.
			body.put("reasoning", Map.of("effort", effort.trim(), "exclude", true));
		}

		String response;
		try {
			response = client.post().uri("/chat/completions")
					.header("Authorization", "Bearer " + properties.getApiKey())
					.header("X-Title", "COYOTE")
					.contentType(MediaType.APPLICATION_JSON)
					.body(body).retrieve().body(String.class);
		} catch (RestClientResponseException e) {
			log.warn("AI 호출 HTTP 오류: status={}", e.getStatusCode().value());
			throw new LlmException("AI 호출이 실패했습니다(HTTP " + e.getStatusCode().value() + ").");
		} catch (RestClientException e) {
			log.warn("AI 호출 실패: cause={}", e.getClass().getSimpleName());
			throw new LlmException("AI 호출이 실패했습니다.");
		}
		return extractContent(response);
	}

	private static String extractContent(String response) {
		try {
			JsonNode content = JSON.readTree(response == null ? "" : response).path("choices").path(0).path("message")
					.path("content");
			String text = content.asString("");
			if (text.isBlank()) {
				throw new LlmException("AI 응답이 비어 있습니다.");
			}
			return text;
		} catch (LlmException e) {
			throw e;
		} catch (RuntimeException e) {
			log.warn("AI 응답을 해석하지 못했습니다: {}", e.getClass().getSimpleName());
			throw new LlmException("AI 응답을 해석하지 못했습니다.");
		}
	}
}
