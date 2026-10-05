package com.coyote.moyeon.user;

import com.coyote.moyeon.user.dto.UpdateUserRequest;
import com.coyote.moyeon.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public UserResponse getMe(@AuthenticationPrincipal Jwt jwt) {
		return userService.getMe(Long.valueOf(jwt.getSubject()));
	}

	@PatchMapping
	public UserResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateUserRequest request) {
		return userService.updateMe(Long.valueOf(jwt.getSubject()), request);
	}
}
