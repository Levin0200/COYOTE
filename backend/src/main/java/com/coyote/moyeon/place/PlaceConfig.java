package com.coyote.moyeon.place;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class PlaceConfig {

	@Bean
	RestClient placeRestClient(PlaceProperties properties) {
		PlaceProperties.DataGoKr config = properties.getDataGoKr();
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(config.getConnectTimeoutMillis())).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofMillis(config.getReadTimeoutMillis()));
		return RestClient.builder().baseUrl(config.getBaseUrl()).requestFactory(requestFactory).build();
	}
}
