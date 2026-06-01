package coyote.meeting_service.repository;

import coyote.meeting_service.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;


/***
 * 인터페이스 작성
 *
 * 원래는 save(), findById() 등을 직접 구현해야 한다.
 * 하지만 Spring Data JPA가 JpaRepository를 통해
 * 기본 CRUD 메서드를 자동 생성해준다.
 *
 * JPA = ORM 표준(규칙)
 * Spring Data JPA = CRUD 자동 생성기
 * 인터페이스 = "이 Repository를 만들어줘" 요청서
 */


/***
 * JpaRepository <Meeting, Long>이란 뭐냐
 * Meeting : 어떤 Entitiy를 관리할 거냐 -> Meeting.java 관리
 * Long : PK 타입이 뭐냐
 * Meeting Pk는 private Long meetingId였다.
 * 그래서 Meeting, Long 형태가 됨
 *
 * 이 한 줄로
 * meetingRepository.save(meeting);
 * meetingRepository.findById(1L);
 * meetingRepository.findAll();
 * meetingRepository.delete(meeting);
 * 위 모든게 가능해 진다. 즉, 기본 CRUD가 자동 생성된다.
 *
 *
 * Entity.Meeting (DB 테이블 - Java 로 표현한 클래스)
 * ↓
 * DB meetings 테이블을 Java 객체로 매핑
 *
 * MeetingRepository
 * ↓
 * Meeting 엔티티를 관리하는 저장소
 *
 * 인터페이스 선언만 하면
 * Spring Data JPA가 CRUD 구현체를 자동 생성
 */

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

}