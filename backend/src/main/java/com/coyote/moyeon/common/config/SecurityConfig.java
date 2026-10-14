package com.coyote.moyeon.common.config;

import com.coyote.moyeon.common.ratelimit.IpRateLimitFilter;
import com.coyote.moyeon.common.ratelimit.RateLimitProperties;
import com.coyote.moyeon.common.ratelimit.RateLimiter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class SecurityConfig {

	private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

	private final SecretKey jwtKey;
	private final List<String> allowedOrigins;

	public SecurityConfig(
			@Value("${app.jwt.secret:}") String jwtSecret,
			@Value("${app.cors.allowed-origins}") String allowedOrigins,
			Environment environment) {
		byte[] keyBytes;
		if (jwtSecret.isBlank()) {
			if (!environment.acceptsProfiles(Profiles.of("dev", "test"))) {
				throw new IllegalStateException("JWT_SECRET must be set outside the dev/test profiles");
			}
			keyBytes = new byte[32];
			new SecureRandom().nextBytes(keyBytes);
			log.warn("JWT_SECRET is not set; using a random key for this run. Issued tokens become invalid on restart.");
		} else {
			keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
			if (keyBytes.length < 32) {
				throw new IllegalStateException("JWT_SECRET must be at least 32 bytes for HS256");
			}
		}
		this.jwtKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim)
				.filter(origin -> !origin.isEmpty()).toList();
		boolean devOrTest = environment.acceptsProfiles(Profiles.of("dev", "test"));
		if (!devOrTest && (this.allowedOrigins.isEmpty() || this.allowedOrigins.contains("*"))) {
			throw new IllegalStateException(
					"CORS_ALLOWED_ORIGINS must list the frontend origins (no wildcard) outside the dev/test profiles");
		}
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimiter rateLimiter,
			RateLimitProperties rateLimitProperties) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				// JSON API라 브라우저가 응답을 문서로 렌더링할 일이 없다. 어떤 리소스도 불러오지 못하게 막는다.
				.headers(headers -> headers
						.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
						.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
				.addFilterAfter(new IpRateLimitFilter(rateLimiter, rateLimitProperties), CorsFilter.class)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/health", "/api/auth/**", "/api/invites/**").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(allowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		// 프론트가 실제로 보내는 헤더만 허용한다(Authorization: 호스트 JWT, X-Guest-Id: 게스트 식별자).
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Guest-Id"));
		// 429 응답의 Retry-After 를 프론트가 읽을 수 있게 노출한다.
		config.setExposedHeaders(List.of("Retry-After"));
		config.setMaxAge(Duration.ofHours(1));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
	}
}
