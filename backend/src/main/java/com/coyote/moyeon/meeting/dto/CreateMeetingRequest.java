package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.PlaceMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CreateMeetingRequest(
		@NotBlank @Size(max = 100) String title,
		@Size(max = 1000) String description,
		@NotNull Instant responseDeadline,
		Instant linkExpirationDate,
		@NotNull LocalDate candidateStartDate,
		@NotNull LocalDate candidateEndDate,
		@NotNull @Valid PlacePolicyData placePolicy,
		@NotEmpty @Size(max = 62) List<@NotNull LocalDate> selectedDates,
		@NotNull PlaceMode placeMode,
		@Size(max = 100) String region,
		@Size(max = 100) String placePreference,
		@Min(1) @Max(100) Integer recommendationRadius,
		@Size(max = 100) String recommendationKeyword) {

	public ResponseData toResponseData() {
		return new ResponseData(selectedDates, placeMode, region, placePreference, recommendationRadius,
				recommendationKeyword);
	}
}
