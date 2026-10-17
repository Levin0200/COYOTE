package com.coyote.moyeon.common.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtKeyConfigTest {

	private static final String ORIGINS = "http://localhost:5173";
	private static final String SECRET = "a-secret-with-at-least-32-bytes-0123456789";

	private static SecurityConfig config(String secret, String profile) {
		MockEnvironment env = new MockEnvironment();
		env.setActiveProfiles(profile);
		return new SecurityConfig(secret, ORIGINS, env);
	}

	private static String issue(SecurityConfig config) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject("1").issuedAt(now).expiresAt(now.plusSeconds(60)).build();
		return config.jwtEncoder()
				.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}

	@Test
	void missingSecretOutsideDevAndTest_failsStartup() {
		assertThrows(IllegalStateException.class, () -> config("", "prod"));
		assertThrows(IllegalStateException.class, () -> config("   ", "staging"));
	}

	@Test
	void missingSecretInDevAndTest_usesWorkingRandomKey() {
		for (String profile : new String[] { "dev", "test" }) {
			SecurityConfig config = config("", profile);
			assertEquals("1", config.jwtDecoder().decode(issue(config)).getSubject());
		}
	}

	@Test
	void randomKeysDifferBetweenRuns() {
		SecurityConfig first = config("", "dev");
		SecurityConfig second = config("", "dev");
		assertThrows(JwtException.class, () -> second.jwtDecoder().decode(issue(first)));
	}

	@Test
	void shortSecret_isRejectedInEveryProfile() {
		assertThrows(IllegalStateException.class, () -> config("too-short", "dev"));
		assertThrows(IllegalStateException.class, () -> config("too-short", "prod"));
	}

	@Test
	void providedSecret_isUsedAndStableAcrossInstances() {
		SecurityConfig first = config(SECRET, "prod");
		SecurityConfig second = config(SECRET, "prod");
		assertEquals("1", second.jwtDecoder().decode(issue(first)).getSubject());
		assertDoesNotThrow(() -> config(SECRET, "dev"));
	}

	@Test
	void differentSecrets_doNotAcceptEachOthersTokens() {
		SecurityConfig first = config(SECRET, "prod");
		SecurityConfig other = config(SECRET + "-other", "prod");
		assertThrows(JwtException.class, () -> other.jwtDecoder().decode(issue(first)));
	}
}
