package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.PlaceMode;
import java.time.LocalDate;
import java.util.List;

public record ResponseData(List<LocalDate> selectedDates, PlaceMode placeMode, String region, String placePreference,
		Integer recommendationRadius, String recommendationKeyword, String placeId) {
}
