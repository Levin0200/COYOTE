package com.coyote.moyeon.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenRouterClientTest {

	private static final String KEY = "test-only-not-an-api-key";
	private static final String BASE_URL = "https://openrouter.ai/api/v1";

	private AiProperties properties;
	private MockRestServiceServer server;
	private OpenRouterClient client;

	@BeforeEach
	void setUp() {
		properties = new AiProperties();
		properties.setApiKey(KEY);
		properties.setModel("deepseek/deepseek-v4-flash-0731");
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		client = new OpenRouterClient(builder.build(), properties);
	}

	private static String completion(String content) {
		return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":" + quote(content) + "}}]}";
	}

	private static String quote(String text) {
		return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
	}

	@Test
	void sendsAChatCompletionWithTheKeyOnlyInTheAuthorizationHeader() {
		server.expect(requestTo(BASE_URL + "/chat/completions"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(requestTo(not(containsString(KEY))))
				.andExpect(header("Authorization", "Bearer " + KEY))
				.andExpect(jsonPath("$.model").value("deepseek/deepseek-v4-flash-0731"))
				.andExpect(jsonPath("$.max_completion_tokens").value(400))
				.andExpect(jsonPath("$.reasoning_effort").value("none"))
				.andExpect(jsonPath("$.messages[0].role").value("system"))
				.andExpect(jsonPath("$.messages[0].content").value("시스템 지시"))
				.andExpect(jsonPath("$.messages[1].role").value("user"))
				.andExpect(jsonPath("$.messages[1].content").value("사용자 입력"))
				.andExpect(jsonPath("$.response_format.type").value("json_object"))
				.andRespond(withSuccess(completion("{\"picks\":[]}"), MediaType.APPLICATION_JSON));

		assertThat(client.complete("시스템 지시", "사용자 입력", 400)).isEqualTo("{\"picks\":[]}");
		server.verify();
	}

	@Test
	void theReasoningOptionIsOmittedWhenNotConfigured() {
		properties.setReasoningEffort("");
		server.expect(requestTo(BASE_URL + "/chat/completions"))
				.andExpect(jsonPath("$.reasoning_effort").value("none"))
				.andRespond(withSuccess(completion("{}"), MediaType.APPLICATION_JSON));

		assertThat(client.complete("s", "u", 100)).isEqualTo("{}");
		server.verify();
	}

	@Test
	void readsTextFromContentBlocksUsedBySomeProviders() {
		server.expect(requestTo(BASE_URL + "/chat/completions"))
				.andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":[{\"type\":\"text\",\"text\":\"{\\\"picks\\\":[]}\"}]}}]}", MediaType.APPLICATION_JSON));

		assertThat(client.complete("s", "u", 100)).isEqualTo("{\"picks\":[]}");
		server.verify();
	}

	@Test
	void withoutAKeyItIsUnavailableAndMakesNoCall() {
		properties.setApiKey("  ");

		assertThat(client.isAvailable()).isFalse();
		assertThatThrownBy(() -> client.complete("s", "u", 100)).isInstanceOf(LlmException.class);
		server.verify();
	}

	@Test
	void httpErrorsBecomeLlmExceptionsWithoutLeakingTheKeyOrTheBody() {
		server.expect(requestTo(BASE_URL + "/chat/completions")).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\":\"bad key " + KEY + "\"}"));
		server.expect(requestTo(BASE_URL + "/chat/completions")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		server.expect(requestTo(BASE_URL + "/chat/completions")).andRespond(withServerError());

		for (int i = 0; i < 3; i++) {
			assertThatThrownBy(() -> client.complete("s", "u", 100)).isInstanceOf(LlmException.class)
					.hasMessageContaining("HTTP").message().doesNotContain(KEY).doesNotContain("bad key");
		}
	}

	@Test
	void malformedResponsesBecomeLlmExceptions() {
		server.expect(requestTo(BASE_URL + "/chat/completions")).andRespond(withSuccess("이건 JSON이 아닙니다", MediaType.TEXT_PLAIN));

		assertThatThrownBy(() -> client.complete("s", "u", 100)).isInstanceOf(LlmException.class);
	}

	@Test
	void emptyResponseIsRetriedWithoutReasoningAndWithMoreOutputTokens() {
		server.expect(requestTo(BASE_URL + "/chat/completions"))
				.andExpect(jsonPath("$.reasoning_effort").value("none"))
				.andExpect(jsonPath("$.max_completion_tokens").value(100))
				.andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));
		server.expect(requestTo(BASE_URL + "/chat/completions"))
				.andExpect(jsonPath("$.reasoning_effort").value("none"))
				.andExpect(jsonPath("$.max_completion_tokens").value(1024))
				.andRespond(withSuccess(completion("{\"picks\":[{\"n\":1}]}"), MediaType.APPLICATION_JSON));

		assertThat(client.complete("s", "u", 100)).isEqualTo("{\"picks\":[{\"n\":1}]}");
		server.verify();
	}

	@Test
	void twoEmptyResponsesBecomeAnLlmException() {
		server.expect(requestTo(BASE_URL + "/chat/completions"))
				.andRespond(withSuccess(completion("   "), MediaType.APPLICATION_JSON));
		server.expect(requestTo(BASE_URL + "/chat/completions"))
				.andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.complete("s", "u", 100)).isInstanceOf(LlmException.class);
		server.verify();
	}
}
