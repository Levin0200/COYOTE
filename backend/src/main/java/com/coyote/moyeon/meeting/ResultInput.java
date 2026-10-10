package com.coyote.moyeon.meeting;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** 결과 생성기에 넘기는 입력. 닉네임 같은 식별 정보는 넘기지 않는다. */
public record ResultInput(LocalDate today, List<ResponseSnapshot> responses) {

	public record ResponseSnapshot(Set<LocalDate> dates, PlaceMode placeMode, String placePreference) {
	}
}
