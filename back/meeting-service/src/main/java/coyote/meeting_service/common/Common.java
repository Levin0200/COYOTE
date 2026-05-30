package coyote.meeting_service.common;


// 여러곳에서 공통으로 쓰는 것들 & 내 파트 큰 그림으로 그리고 쪼개기

/***
 *
 * 1. 모임 생성 페이지
 * 2. 모임 생성 API
 * 3. AI 처리 요청 API
 * 4. AI 추천 결과 저장/조회
 *
 *  *
 *  * controller  → 모임 생성 API 주소 (주소 담당)
 *  * dto         → 모임 생성 요청/응답 데이터 JSON 형식 데이터를 Java 객체로 담기
 *  * entity      → meetings, ai_requests, ai_recommendations 테이블 매핑 (Java - DB) (DB 테이블을 자바 객체로 표현)
 *  * repository  → DB 저장/조회
 *  * service     → 모임 생성 로직, AI 처리 로직
 *  * config      → 나중에 CORS/Swagger  프로젝트 설정
 *  * common      → 나중에 공통 응답/예외처리  공통 기능
 *
 *
 *
 * 전반적인 전체 그림
 *
 React 모임 생성 페이지

 ↓
 MeetingController

 ↓
 MeetingRequest

 ↓
 MeetingService

 ↓
 Meeting Entity 생성

 ↓
 MeetingRepository

 ↓
 MariaDB meetings 테이블 저장

 ↓
 MeetingCreateResponse

 ↓
 React
 *
 *
 *
 *
 *메인 페이지
 * ↓
 * 모임 생성하기 버튼
 * ↓
 * 모임 생성 페이지
 * ↓
 * 제목, 목적, 지역, 최소 후보날짜 입력
 * ↓
 * POST /api/meetings
 * ↓
 * meetings 테이블 저장
 *
 *

 *
 */






public class Common {
    
    
}
