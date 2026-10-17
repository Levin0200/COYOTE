package com.coyote.moyeon.auth;

import com.coyote.moyeon.auth.dto.TokenResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final Duration expiration;

	public JwtTokenService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
		this.jwtEncoder = jwtEncoder;
		this.expiration = Duration.ofMinutes(expirationMinutes);
	}

	public TokenResponse issue(Long userId) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(String.valueOf(userId))
				.issuedAt(now)
				.expiresAt(now.plus(expiration))
				.build();
		String token = jwtEncoder.encode(
				JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
		return new TokenResponse(token, "Bearer", expiration.toSeconds());
	}
}
