package com.coyote.moyeon.meeting;

/** 마감 시 최종 날짜/장소를 정하는 전략. 나중에 AI 구현으로 교체하거나 실패 시 기본 구현으로 대체할 수 있다. */
public interface FinalResultGenerator {

	GeneratedResult generate(ResultInput input);
}
