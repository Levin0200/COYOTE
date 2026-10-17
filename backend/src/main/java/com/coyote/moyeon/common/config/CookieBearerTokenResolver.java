package com.coyote.moyeon.common.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;

/** 브라우저의 HttpOnly 인증 쿠키를 우선 사용하고, 테스트·비브라우저 API용 Authorization도 유지한다. */
@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

	private final DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
	private final String cookieName;

	public CookieBearerTokenResolver(@Value("${app.auth.cookie-name:moyeon_auth}") String cookieName) {
		this.cookieName = cookieName;
	}

	@Override
	public String resolve(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookieName.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
					return cookie.getValue();
				}
			}
		}
		return headerResolver.resolve(request);
	}
}
