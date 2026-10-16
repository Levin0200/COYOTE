package com.coyote.moyeon.place;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class PlaceConfig {

	@Bean
	@ConditionalOnProperty(name = "app.place.provider", havingValue = "public-data", matchIfMissing = true)
	RestClient placeRestClient(PlaceProperties properties) {
		PlaceProperties.DataGoKr config = properties.getDataGoKr();
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(config.getConnectTimeoutMillis())).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofMillis(config.getReadTimeoutMillis()));
		return RestClient.builder().baseUrl(config.getBaseUrl()).requestFactory(requestFactory).build();
	}

	@Bean
	@ConditionalOnProperty(name = "app.place.provider", havingValue = "tour-api")
	RestClient tourApiRestClient(PlaceProperties properties) {
		PlaceProperties.TourApi config = properties.getTourApi();
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(config.getConnectTimeoutMillis())).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofMillis(config.getReadTimeoutMillis()));
		return RestClient.builder().baseUrl(config.getBaseUrl()).requestFactory(requestFactory).build();
	}

	@Bean
	RestClient moisRestClient(PlaceProperties properties) {
		PlaceProperties.Mois config = properties.getMois();
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(config.getConnectTimeoutMillis())).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofMillis(config.getReadTimeoutMillis()));
		return RestClient.builder().baseUrl(config.getBaseUrl()).requestFactory(requestFactory).build();
	}
}
