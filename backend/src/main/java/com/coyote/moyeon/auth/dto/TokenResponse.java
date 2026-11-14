package com.coyote.moyeon.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
