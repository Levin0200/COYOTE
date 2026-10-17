package com.coyote.moyeon.user.dto;

import com.coyote.moyeon.user.User;

public record UserResponse(Long id, String email, String name, String address) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getAddress());
	}
}
