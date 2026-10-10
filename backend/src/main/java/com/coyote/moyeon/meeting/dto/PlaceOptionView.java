package com.coyote.moyeon.meeting.dto;

/** 응답용 추천 후보. id는 저장하지 않고 목록 순번(1부터)을 내려준다(프론트의 key 용도). */
public record PlaceOptionView(int id, String name, String distance) {
}
