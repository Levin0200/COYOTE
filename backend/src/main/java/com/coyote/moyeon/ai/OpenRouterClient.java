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
	private static final String EMPTY_RESPONSE = "AI 응답이 비어 있습니다.";
	private static final int RETRY_MIN_OUTPUT_TOKENS = 1024;

	private final RestClient client;
	private final AiProperties properties;

	public OpenRouterClient(@Qualifier("aiRestClient") RestClient aiRestClient, AiProperties properties) {
		this.client = aiRestClient;
		this.properties = properties;
		log.info("OpenRouter 장소 추천 설정: keyConfigured={}, model={}",
				properties.getApiKey() != null && !properties.getApiKey().isBlank(), properties.getModel());
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
		Map<String, Object> body = requestBody(system, user, maxOutputTokens, true);
		String response = post(body);
		try {
			return extractContent(response);
		} catch (LlmException e) {
			if (!EMPTY_RESPONSE.equals(e.getMessage())) {
				throw e;
			}
			// 일부 모델·제공자는 출력 한도를 소진하고 content를 비워 반환할 수 있다.
			// 이 경우 추론을 명시적으로 끄고 출력 여유를 늘려 한 번 재시도한다.
			log.warn("AI 응답이 비어 있어 추론을 끄고 한 번 재시도합니다.");
			Map<String, Object> retryBody = requestBody(system, user,
					Math.max(maxOutputTokens, RETRY_MIN_OUTPUT_TOKENS), false);
			return extractContent(post(retryBody));
		}
	}

	private Map<String, Object> requestBody(String system, String user, int maxOutputTokens,
			boolean includeReasoning) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", properties.getModel());
		body.put("messages", List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)));
		body.put("max_completion_tokens", maxOutputTokens);
		body.put("temperature", 0.2);
		body.put("response_format", Map.of("type", "json_object"));
		String effort = properties.getReasoningEffort();
		// reasoning 옵션을 생략하면 모델 기본 추론이 켜질 수 있으므로, 재시도에서는 none을 명시한다.
		// OpenRouter 공식 Chat Completions 규격의 reasoning_effort 단축 필드를 사용한다.
		String requestedEffort = includeReasoning && effort != null && !effort.isBlank() ? effort.trim() : "none";
		body.put("reasoning_effort", requestedEffort);
		return body;
	}

	private String post(Map<String, Object> body) {
		try {
			return client.post().uri("/chat/completions")
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
	}

	private static String extractContent(String response) {
		try {
			JsonNode content = JSON.readTree(response == null ? "" : response).path("choices").path(0).path("message")
					.path("content");
			String text = content.asString("");
			if (text.isBlank() && content.isArray()) {
				StringBuilder joined = new StringBuilder();
				for (JsonNode block : content) {
					String part = block.isString() ? block.asString("") : block.path("text").asString("");
					if (!part.isBlank()) {
						joined.append(part);
					}
				}
				text = joined.toString();
			}
			if (text.isBlank()) {
				throw new LlmException(EMPTY_RESPONSE);
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
