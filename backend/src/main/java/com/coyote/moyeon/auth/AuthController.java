package com.coyote.moyeon.auth;

import com.coyote.moyeon.auth.dto.ConfirmCodeRequest;
import com.coyote.moyeon.auth.dto.LoginRequest;
import com.coyote.moyeon.auth.dto.SendCodeRequest;
import com.coyote.moyeon.auth.dto.SignupRequest;
import com.coyote.moyeon.auth.dto.TokenResponse;
import com.coyote.moyeon.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/email-verification")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void sendCode(@Valid @RequestBody SendCodeRequest request) {
		authService.sendVerificationCode(request);
	}

	@PostMapping("/email-verification/confirm")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void confirmCode(@Valid @RequestBody ConfirmCodeRequest request) {
		authService.confirmVerificationCode(request);
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse signup(@Valid @RequestBody SignupRequest request) {
		return authService.signup(request);
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}
}
