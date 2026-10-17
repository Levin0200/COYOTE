package com.coyote.moyeon.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		@NotBlank @Size(max = 50) String name,
		@Size(max = 255) String address) {
}
