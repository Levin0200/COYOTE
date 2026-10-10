package com.coyote.moyeon.meeting;

/** 모임별 개수 집계용 값 객체(JPQL 생성자 표현식에서 사용). */
public record MeetingCount(Long meetingId, Long count) {
}
