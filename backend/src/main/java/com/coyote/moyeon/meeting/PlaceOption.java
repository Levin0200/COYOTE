package com.coyote.moyeon.meeting;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record PlaceOption(
		@Column(name = "option_name", nullable = false, length = 100) String name,
		@Column(name = "distance", length = 20) String distance) {
}
