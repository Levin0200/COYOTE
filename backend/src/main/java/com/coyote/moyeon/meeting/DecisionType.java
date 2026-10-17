package com.coyote.moyeon.meeting;

public enum DecisionType {
	MAJORITY,
	TIE_BREAK,
	/** 제안이 흩어져 가까운 장소끼리 묶어 합의한 지역의 후보에서 정했다. */
	SPATIAL,
	/** 호스트가 후보 중에서 골랐다. */
	HOST_CHOICE,
	/** 호스트가 시한 안에 고르지 않아 자동으로 정했다. */
	AUTO_PICK
}
