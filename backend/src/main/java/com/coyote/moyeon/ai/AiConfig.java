package com.coyote.moyeon.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class AiConfig {

	@Bean
	RestClient aiRestClient(AiProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMillis())).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMillis()));
		return RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(requestFactory).build();
	}
}
