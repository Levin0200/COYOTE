package coyote.meeting_service.dto;

import lombok.Getter;
import lombok.Setter;

//DTO : 프론트에서 보내는 JSON을 담는 상자 data-transfer-object
// 이 클래스는 요청 데이터를 전달용 객체를 표현한다.

/**
 *
 * DTO가 받을 데이터 JSON 형식
 *
 * {
 *   "hostId": 1,
 *   "title": "강남 점심 모임",
 *   "category": "식사",
 *   "description": "6월 20일 점심 가능하신 분"
 * }
 *
 * React 요청 JSON
 * ↓
 * MeetingRequest에 담김
 * ↓
 * Service에서 Meeting Entity로 변환
 * ↓
 * Repository로 DB 저장
 *
 */

@Getter
@Setter
public class MeetingCreateRequest {

    private Long hostId;

    private String title;

    private String category;

    private String description;
}