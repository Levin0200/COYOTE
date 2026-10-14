package com.coyote.moyeon.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 로그인 없이 호출할 수 있는 공개 API(/api/auth/**, /api/invites/**)에 IP별 분당 요청 수를 제한한다.
 * 스프링 빈으로 등록하지 않고 SecurityConfig에서 CORS 필터 뒤에 끼워 넣는다
 * (429 응답에도 CORS 헤더가 붙어 프론트가 오류 내용을 읽을 수 있다).
 */
public class IpRateLimitFilter extends OncePerRequestFilter {

	private static final Duration WINDOW = Duration.ofMinutes(1);

	private final RateLimiter rateLimiter;
	private final RateLimitProperties properties;

	public IpRateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties) {
		this.rateLimiter = rateLimiter;
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String bucket = null;
		int limit = 0;
		String path = request.getRequestURI();
		if (!HttpMethod.OPTIONS.matches(request.getMethod())) {
			if (path.startsWith("/api/auth/")) {
				bucket = "auth";
				limit = properties.getIp().getAuthPerMinute();
			} else if (path.startsWith("/api/invites/")) {
				bucket = "invites";
				limit = properties.getIp().getInvitePerMinute();
			}
		}
		if (bucket == null) {
			chain.doFilter(request, response);
			return;
		}
		RateLimiter.Decision decision = rateLimiter.tryAcquire("ip:" + bucket + ":" + request.getRemoteAddr(), limit,
				WINDOW);
		if (decision.allowed()) {
			chain.doFilter(request, response);
			return;
		}
		response.setStatus(429);
		response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"요청이 너무 많습니다. "
				+ decision.retryAfterSeconds() + "초 후 다시 시도해 주세요.\"}");
	}
}
