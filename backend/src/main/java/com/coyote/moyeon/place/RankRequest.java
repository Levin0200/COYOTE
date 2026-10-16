package com.coyote.moyeon.place;

import java.util.List;

/**
 * 후보 중에서 장소를 고르는 요청. title·description은 사용자가 쓴 값이라 AI에 넘기기 전에 정리한다.
 * candidates는 가까운 순서로 정렬되어 있어야 한다.
 */
public record RankRequest(String title, String description, int participantCount, List<Place> candidates, int pick) {
}
