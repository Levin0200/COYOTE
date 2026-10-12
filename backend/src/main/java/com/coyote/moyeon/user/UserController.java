package com.coyote.moyeon.user;

import com.coyote.moyeon.user.dto.UpdateUserRequest;
import com.coyote.moyeon.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

	//userService 변수가 가르키는 객체를 교체할 수 없게 상수처리 / 처음 받은 객체를 이후 교체하지 않는다.
	//즉, 유저의 정보를 수정할 수 있지만 유저 자체가 다른 유저 객체를 가르키게 해서는 안되니까
	//ex 짱구라는 객체가 훈이라는 객체를 가르키게 되면 짱구를 찾아온 누군가는 매우 당혹스럽게 되므로 이를 방지하기 위해
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

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMe(@AuthenticationPrincipal Jwt jwt) {
		userService.deleteMe(Long.valueOf(jwt.getSubject()));
	}
}
