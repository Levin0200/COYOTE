package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.Meeting;
import com.coyote.moyeon.meeting.PlaceOption;
import com.coyote.moyeon.meeting.PlacePolicyMode;
import java.util.List;
import java.util.stream.IntStream;

public record PlacePolicyView(
		PlacePolicyMode mode,
		String region,
		Integer radius,
		String keyword,
		String fixedPlace,
		List<PlaceOptionView> options) {

	public static PlacePolicyView from(Meeting meeting) {
		List<PlaceOption> options = meeting.getPlaceOptions();
		return new PlacePolicyView(meeting.getPlacePolicyMode(), meeting.getPolicyRegion(),
				meeting.getPolicyRadiusKm(), meeting.getPolicyKeyword(), meeting.getFixedPlace(),
				IntStream.range(0, options.size())
						.mapToObj(i -> {
							PlaceOption option = options.get(i);
							return new PlaceOptionView(i + 1, option.name(), option.distance(), option.placeId(),
									option.address(), option.category(), option.latitude(), option.longitude());
						})
						.toList());
	}
}
