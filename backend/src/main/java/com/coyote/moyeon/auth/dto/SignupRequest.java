package com.coyote.moyeon.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(max = 50) String name,
		@NotBlank @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.") String password,
		@Size(max = 255) String address,
		@AssertTrue(message = "서비스 이용약관에 동의해야 합니다.") boolean serviceAgree,
		@AssertTrue(message = "개인정보 수집 및 이용에 동의해야 합니다.") boolean privacyAgree) {
}
