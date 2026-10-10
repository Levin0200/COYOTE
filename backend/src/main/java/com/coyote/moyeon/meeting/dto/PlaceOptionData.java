package com.coyote.moyeon.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceOptionData(
		@NotBlank @Size(max = 100) String name,
		@Size(max = 20) String distance) {
}
