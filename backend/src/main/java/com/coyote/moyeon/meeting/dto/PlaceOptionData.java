package com.coyote.moyeon.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaceOptionData(
		@NotBlank @Size(max = 100) String name,
		@Size(max = 20) String distance,
		/** 검색에서 받은 장소 ID. 있으면 서버가 공식 이름·주소·좌표로 바꿔 저장한다. */
		@Size(max = 64) String placeId) {
}
