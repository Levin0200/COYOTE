package com.coyote.moyeon.auth.dto;

/** 브라우저에는 JWT 원문을 노출하지 않고 로그인 상태와 만료 시간만 알린다. */
public record SessionResponse(boolean authenticated, long expiresIn) {
}
