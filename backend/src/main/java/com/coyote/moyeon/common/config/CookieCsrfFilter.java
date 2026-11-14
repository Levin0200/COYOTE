package com.coyote.moyeon.common.config;

import com.coyote.moyeon.auth.AuthCookieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/** HttpOnly 인증 쿠키가 자동 전송되는 변경 요청에 이중 제출 CSRF 토큰을 요구한다. */
public class CookieCsrfFilter extends OncePerRequestFilter {

	private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
	private final String authCookie;

	public CookieCsrfFilter(String authCookie) {
		this.authCookie = authCookie;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (SAFE.contains(request.getMethod()) || cookie(request, authCookie) == null) {
			chain.doFilter(request, response);
			return;
		}
		String expected = cookie(request, AuthCookieService.XSRF_COOKIE);
		String actual = request.getHeader(AuthCookieService.XSRF_HEADER);
		if (expected == null || actual == null || !MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write("{\"code\":\"CSRF_TOKEN_INVALID\",\"message\":\"보안 토큰이 없거나 올바르지 않습니다. 페이지를 새로고침해 주세요.\"}");
			return;
		}
		chain.doFilter(request, response);
	}

	private static String cookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) return null;
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) return cookie.getValue();
		}
		return null;
	}
}
