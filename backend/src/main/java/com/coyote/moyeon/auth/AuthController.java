package com.coyote.moyeon.auth;

import com.coyote.moyeon.auth.dto.ConfirmCodeRequest;
import com.coyote.moyeon.auth.dto.LoginRequest;
import com.coyote.moyeon.auth.dto.SendCodeRequest;
import com.coyote.moyeon.auth.dto.SignupRequest;
import com.coyote.moyeon.auth.dto.SessionResponse;
import com.coyote.moyeon.auth.dto.TokenResponse;
import com.coyote.moyeon.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final AuthCookieService cookies;

	public AuthController(AuthService authService, AuthCookieService cookies) {
		this.authService = authService;
		this.cookies = cookies;
	}

	@PostMapping("/email-verification")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void sendCode(@Valid @RequestBody SendCodeRequest request, HttpServletRequest http) {
		authService.sendVerificationCode(request, http.getRemoteAddr());
	}

	@PostMapping("/email-verification/confirm")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void confirmCode(@Valid @RequestBody ConfirmCodeRequest request) {
		authService.confirmVerificationCode(request);
	}

	/**
	 * 회원가입
	 * 요청 예시 POST/api/auth/signup
	 * 회원가입 성공 시 생성된 회원 정보를 UserResponse 형태로 반환
	 * HTTP 상태코드는 201 Created
	 */
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse signup(@Valid @RequestBody SignupRequest request) {
		// 회원 정보 검증 및 db 저장 로직을 Service에서 처리 (여기서 요청 받고->authService에서 로직처리)
		return authService.signup(request);
	}

	/**
	 *
	 * 로그인
	 * 요청예시: POST / api/auth/login
	 * 이메일, 비밀번호 검증 후 JWT 토큰 반환
	 */
	@PostMapping("/login")
	public SessionResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http,
			HttpServletResponse response) {
		TokenResponse token = authService.login(request, http.getRemoteAddr());
		cookies.login(response, token);
		return new SessionResponse(true, token.expiresIn());
	}

	@GetMapping("/session")
	public SessionResponse session(@AuthenticationPrincipal Jwt jwt) {
		return new SessionResponse(jwt != null, 0);
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletResponse response) {
		cookies.logout(response);
	}
}
