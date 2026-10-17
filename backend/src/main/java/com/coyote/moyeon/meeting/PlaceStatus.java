package com.coyote.moyeon.meeting;

/** 최종 장소가 어디까지 정해졌는지. 날짜는 마감 즉시 정해지고 장소만 이 상태를 따른다. */
public enum PlaceStatus {
	/** 참여자 제안을 분석해 후보를 만드는 중(마감 직후). */
	PENDING,
	/** 후보가 정해졌고 호스트가 하나를 고르기를 기다린다. 시한이 지나면 자동으로 정한다. */
	AWAITING_HOST,
	/** 장소가 정해졌다. */
	DECIDED,
	/** 정할 장소가 없어 추후 협의로 남겼다. */
	NONE
}
