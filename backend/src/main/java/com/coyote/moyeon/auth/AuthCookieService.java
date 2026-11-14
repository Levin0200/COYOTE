package com.coyote.moyeon.auth;

import com.coyote.moyeon.auth.dto.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** JWT는 HttpOnly 쿠키로, CSRF 토큰은 이중 제출용 일반 쿠키로 전달한다. */
@Component
public class AuthCookieService {

	public static final String XSRF_COOKIE = "XSRF-TOKEN";
	public static final String XSRF_HEADER = "X-XSRF-TOKEN";
	private final SecureRandom random = new SecureRandom();
	private final String authCookie;
	private final boolean secure;

	public AuthCookieService(@Value("${app.auth.cookie-name:moyeon_auth}") String authCookie,
			@Value("${app.auth.cookie-secure:false}") boolean secure) {
		this.authCookie = authCookie;
		this.secure = secure;
	}

	public String authCookieName() {
		return authCookie;
	}

	public void login(HttpServletResponse response, TokenResponse token) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie(authCookie, token.accessToken(), true,
				Duration.ofSeconds(token.expiresIn())).toString());
		byte[] csrf = new byte[32];
		random.nextBytes(csrf);
		response.addHeader(HttpHeaders.SET_COOKIE, cookie(XSRF_COOKIE,
				Base64.getUrlEncoder().withoutPadding().encodeToString(csrf), false,
				Duration.ofSeconds(token.expiresIn())).toString());
	}

	public void logout(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie(authCookie, "", true, Duration.ZERO).toString());
		response.addHeader(HttpHeaders.SET_COOKIE, cookie(XSRF_COOKIE, "", false, Duration.ZERO).toString());
		response.setHeader("Clear-Site-Data", "\"cache\", \"cookies\", \"storage\"");
	}

	private ResponseCookie cookie(String name, String value, boolean httpOnly, Duration maxAge) {
		return ResponseCookie.from(name, value).httpOnly(httpOnly).secure(secure).sameSite("Lax")
				.path("/").maxAge(maxAge).build();
	}
}
