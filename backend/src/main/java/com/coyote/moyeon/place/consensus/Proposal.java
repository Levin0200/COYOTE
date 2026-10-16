package com.coyote.moyeon.place.consensus;

import com.coyote.moyeon.place.Place;

/** 게스트 한 명이 제안한 장소. order는 제안(응답 제출) 순서이고 작을수록 먼저 낸 것이다. */
public record Proposal(Place place, int order) {
}
