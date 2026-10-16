package com.coyote.moyeon.place;

import java.util.List;

/** 고르기 결과. aiUsed는 AI가 실제로 골랐는지(false면 규칙 기반 대체)를 알려 준다. */
public record Ranking(List<RankedPlace> places, boolean aiUsed) {
}
