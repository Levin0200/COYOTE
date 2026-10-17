package com.coyote.moyeon.place;

/** 고른 장소와 짧은 이유. 규칙 기반으로 고른 경우 reason은 null이다. */
public record RankedPlace(Place place, String reason) {
}
