package coyote.meeting_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
/**
 * Getter가
 * getMeetingId()
 * getTitle()
 * getMessage() 이것들 메서드를 자동 생성
 * 
 */

//얘는 Lombok 어노테이션이다.
//모든 필드를 매개변수로 받는 생성자(Constructor)를 자동 생성 해준다.
// @Getter, @Setter @All~~ 등... 이게 Lombok이라는 애가 제공해주는 어노테이션 기능들이다.

/**
 * public MeetingCreateResponse(
 *         Long meetingId,
 *         String title,
 *         String message
 * ) {
 *     this.meetingId = meetingId;
 *     this.title = title;
 *     this.message = message;
 * }
 * 이걸 자동 생성해준다.
 */

//이 클래스가 해주는 일은 JSON 데이터를 MeetingRequset가 받고, 서비스에서 객체를 생성 후 프론트에게 반환해주는 값을 얘가 담당함
//처리 결과 데이터를 보내줄때 얘를 쓴다. / 모임 생성 후 프론트에게 반환할 응답 데이터를 담는 DTO다.

//Create 전용 dto
@AllArgsConstructor
public class MeetingCreateResponse {

    private Long meetingId;

    private String title;

    private String message;
}