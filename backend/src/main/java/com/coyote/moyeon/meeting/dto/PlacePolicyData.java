package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.PlacePolicyMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PlacePolicyData(
		@NotNull PlacePolicyMode mode,
		@Size(max = 100) String region,
		@Min(1) @Max(100) Integer radius,
		@Size(max = 100) String keyword,
		@Size(max = 100) String fixedPlace,
		@Size(max = 10) List<@NotNull @Valid PlaceOptionData> options) {
}
