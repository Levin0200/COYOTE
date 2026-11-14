package com.coyote.moyeon.meeting.dto;

import com.coyote.moyeon.meeting.PlaceMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record GuestResponseRequest(
		@NotBlank @Size(max = 20) String nickname,
		@NotEmpty @Size(max = 62) List<@NotNull LocalDate> selectedDates,
		@NotNull PlaceMode placeMode,
		@Size(max = 100) String region,
		@Size(max = 100) String placePreference,
		@Min(1) @Max(100) Integer recommendationRadius,
		@Size(max = 100) String recommendationKeyword,
		/** 선택·제안한 장소의 ID(검색 결과). 있으면 서버가 공식 정보로 바꿔 저장한다. */
		@Size(max = 64) String placeId) {

	/** 요청에 들어 있는 장소 ID. 서버가 공식 정보로 확인할 대상이다. */
	public Set<String> placeIds() {
		return PlaceIds.collect(placeId, null);
	}

	public ResponseData toResponseData() {
		return new ResponseData(selectedDates, placeMode, region, placePreference, recommendationRadius,
				recommendationKeyword, placeId);
	}
}
