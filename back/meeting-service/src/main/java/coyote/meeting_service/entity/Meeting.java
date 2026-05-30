package coyote.meeting_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

//이 클래스는 DB의 meetings 테이블을 자바 객체로 표현한 클래스입니다.


//이 클래스는 DB 테이블이라는 어노테이션 표시
@Entity

// DB의 meetings 라는 테이블과 자바 객체를 연결 시키는 어노테이션
@Table(name = "meetings")

//private 접근 제어자 쓸때 lombok이 getter 자동 생성해줌
@Getter

//private 접근 제어자 쓸때 lombok이 setter 자동 생성해줌
@Setter


//Meeting 객체를 만들 클래스 선언
public class Meeting {

    //@Id는 PrimaryKey의 역할을 한다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //@Column은 기본적으로 DB 컬럼 설정이다.
    @Column(name = "meeting_id")
    private Long meetingId;

    //@Column 속성에 nullable이 false면 NOT NULL과 같다.
    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Column(name ="title", nullable = false, length = 100)
    private String title;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 20)
    private String status = "OPEN";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}