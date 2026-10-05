package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.PlaceMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record GuestResponseRequest(
		@NotBlank @Size(max = 20) String nickname,
		@NotEmpty @Size(max = 62) List<@NotNull LocalDate> selectedDates,
		@NotNull PlaceMode placeMode,
		@Size(max = 100) String region,
		@Size(max = 100) String placePreference) {

	public ResponseData toResponseData() {
		return new ResponseData(selectedDates, placeMode, region, placePreference);
	}
}
