package com.coyote.moyeon.place.consensus;

import com.coyote.moyeon.place.Place;

/** 같은 장소에 모인 제안. votes는 그 장소를 제안한 사람 수, order는 가장 먼저 낸 제안의 순서다. */
public record PlaceVote(Place place, int votes, int order) {
}
