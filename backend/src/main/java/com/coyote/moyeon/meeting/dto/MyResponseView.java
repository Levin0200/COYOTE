package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.MeetingResponse;
import com.coyote.moyeon.meeting.PlaceMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MyResponseView(
		String nickname,
		List<LocalDate> selectedDates,
		PlaceMode placeMode,
		String region,
		String placePreference,
		Integer recommendationRadius,
		String recommendationKeyword,
		PlaceDetailView place,
		Instant updatedAt) {

	public static MyResponseView from(MeetingResponse response) {
		return new MyResponseView(
				response.getParticipant().getNickname(),
				response.getAvailableDates().stream().sorted().toList(),
				response.getPlaceMode(),
				response.getRegion(),
				response.getPlacePreference(),
				response.getRecommendationRadius(),
				response.getRecommendationKeyword(),
				PlaceDetailView.from(response.toPlaceInfo()),
				response.getUpdatedAt());
	}
}
