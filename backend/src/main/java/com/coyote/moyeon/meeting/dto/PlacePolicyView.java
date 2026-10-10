package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.Meeting;
import com.coyote.moyeon.meeting.PlacePolicyMode;
import java.util.List;

public record PlacePolicyView(
		PlacePolicyMode mode,
		String region,
		Integer radius,
		String keyword,
		String fixedPlace,
		List<PlaceOptionData> options) {

	public static PlacePolicyView from(Meeting meeting) {
		return new PlacePolicyView(meeting.getPlacePolicyMode(), meeting.getPolicyRegion(),
				meeting.getPolicyRadiusKm(), meeting.getPolicyKeyword(), meeting.getFixedPlace(),
				meeting.getPlaceOptions().stream().map(o -> new PlaceOptionData(o.name(), o.distance())).toList());
	}
}
