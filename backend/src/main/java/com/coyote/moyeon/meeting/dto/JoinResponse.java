package com.coyote.moyeon.meeting.dto;

/** 참여 등록 결과. responded는 이 참여자가 이미 응답을 제출했는지 여부다. */
public record JoinResponse(String nickname, boolean responded) {
}
