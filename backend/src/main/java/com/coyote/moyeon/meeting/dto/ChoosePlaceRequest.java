package com.coyote.moyeon.meeting.dto;

import jakarta.validation.constraints.Size;

/** 호스트가 후보 중에서 고른 장소. 장소 ID가 있으면 ID로, 이름만 있는 예전 후보는 이름으로 찾는다. */
public record ChoosePlaceRequest(
		@Size(max = 64) String placeId,
		@Size(max = 100) String name) {
}
