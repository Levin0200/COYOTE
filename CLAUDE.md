# 모여ON (COYOTE) 백엔드

## 작업 규칙

- 항상 한국어로 답변해줘.
- 코드 수정 전에 계획을 먼저 말해줘.
- 파일을 삭제할 때는 반드시 먼저 확인해줘.
- 작업 시작 전에 내 요청에 불명확한 부분 있으면 먼저 질문해줘. 확인 없이 가정하지마.

## 프로젝트 개요

- 초대 링크 기반 모임 일정/장소 조율 서비스. 호스트가 모임을 만들고, 게스트는 링크 + 닉네임으로 날짜/장소를 응답하며, 마감 후 최종 날짜/장소를 산출한다.
- 백엔드: Spring Boot 4.1.1, Java 25, Gradle, JPA, Spring Security. 패키지 루트 `com.coyote.moyeon`.
- 프론트엔드: `D:\2학년\캡스톤\COYOTE\front\meeting-service` (React 19 + Vite, 개발 서버 `http://localhost:5173`). 현재는 localStorage(`src/services/meetingStorage.js`)가 DB 역할을 하며, 백엔드가 이를 대체한다.
- UserPreferences 페이지는 범위에서 제외했다. 취향/관심사 엔티티는 만들지 않는다.

## 확정된 결정

- DB: 개발은 H2(MariaDB 호환 모드, 파일 저장), 이후 MariaDB로 전환. 프로필(`dev`/`prod`)로 분리한다.
- DB 호환을 위해 네이티브 SQL, DB 전용 기능, JSON 컬럼을 쓰지 않는다. 날짜 선택 목록 같은 컬렉션은 별도 테이블로 정규화한다.
- 인증: JWT 액세스 토큰만 사용(리프레시 토큰은 필요 시 추후 추가). Spring Security 내장 Nimbus(HS256)를 우선 검토한다(Boot 4는 Jackson 3 기반이라 jjwt와 섞이는 것을 피함).
- 호스트는 회원가입 후 로그인 필수. 게스트는 로그인 없이 초대 토큰 + 닉네임 + guestId로 참여하며, 마감 전까지 응답을 수정(upsert)할 수 있다.
- 이메일 인증: 개발 중에는 서버 로그에 인증번호를 출력하고, 발송부는 인터페이스로 분리해 나중에 실제 메일 발송으로 교체한다.
- 장소 추천: 거리순의 단순한 방식(Kakao Local API를 서버에서 프록시).
- 소셜 로그인(네이버/카카오/구글/애플)은 범위에서 제외.
- 비밀번호는 BCrypt 해시로 저장한다.
- 패키지 구조는 도메인형: `common`(config, exception), `auth`, `user`, `meeting`.
- 성공 응답은 DTO를 그대로 반환하고, 에러는 `{code, message}` 형태로 통일한다.
- 비밀값(JWT 키, DB 계정 등)은 git에 올리지 않고 환경변수로 받는다. JWT 키는 `JWT_SECRET`(32바이트 이상)이며 설정 파일에 값을 쓰지 않는다. `dev`/`test` 프로필에서 비어 있으면 실행 시마다 무작위 키를 생성하고(재시작 시 토큰 무효), 그 외 프로필에서는 비어 있으면 서버가 시작되지 않는다.

## 도메인 (프론트 코드 기준)

- Meeting: id, inviteToken, title, description, responseDueDate(JSON에서는 `responseDeadline`, 모임 상세의 `id`는 `meetingId`로 내려감), linkExpirationDate, candidateStartDate/candidateEndDate(후보 날짜 범위, 최대 62일), 장소 정책(placePolicyMode `FIXED`/`RECOMMEND`/`OPEN`, 정책 지역·반경·키워드, fixedPlace, 추천 후보 목록은 별도 테이블 `meeting_place_option`), status(`COLLECTING` 등), manuallyClosed, 호스트 User 연결
- Participant: id, meetingId, nickname, role(`HOST`/`GUEST`), guestId
- Response: participantId, 선택한 날짜 목록(별도 테이블), placeMode(`FIXED`/`RECOMMEND`/`PROPOSE`/`OPEN`), region, placePreference, recommendationRadius/recommendationKeyword, latitude/longitude. 서버는 정책에 맞는 응답만 받는다(`FIXED`는 확정 장소, `RECOMMEND`는 후보 중 선택, `OPEN`은 게스트가 `PROPOSE`/`OPEN`, 호스트는 `OPEN`).
- FinalResult: meetingId, selectedDate, selectedPlace, decisionType(`MAJORITY`/`TIE_BREAK`), reason
- User: id, email(unique), password(BCrypt), name, address(nullable), termsAgreedAt, createdAt

## 단계별 작업 순서

한 단계씩 진행하고, 각 단계가 끝나면 결과를 보고한 뒤 다음 단계로 넘어간다.

### 0단계: 기반 설정
- `build.gradle`에 `spring-boot-starter-validation`, `spring-boot-starter-oauth2-resource-server`, H2(`runtimeOnly`) 추가
- `application.properties`(공통, 기본 프로필 dev), `application-dev.properties`(H2, ddl-auto=update, SQL 로그, 개발용 JWT 키), `application-prod.properties`(MariaDB, 환경변수)
- 도메인형 패키지 구조 생성
- Security 설정: `/api/auth/**`, `/api/invites/**` 공개, 나머지 인증 필요, 세션 미사용, CSRF 비활성화, CORS(`http://localhost:5173`) 허용
- `@RestControllerAdvice` 공통 예외 처리
- 헬스체크 엔드포인트
- `.gitignore`에 H2 데이터 파일(`data/`)과 비밀값 제외 추가
- 확인: `./gradlew build`, 앱 기동, 헬스체크 호출

### 1단계: 인증
- User 엔티티와 Repository
- 이메일 인증: `POST /api/auth/email-verification`(발송), `POST /api/auth/email-verification/confirm`(확인). 발송부 인터페이스 + 개발용 로그 출력 구현체, 인증 상태 임시 저장 테이블
- `POST /api/auth/signup`(이메일 인증 완료 확인, 약관 동의 확인, BCrypt), `POST /api/auth/login`(JWT 발급)
- JWT 검증 설정, `GET /api/users/me`, `PATCH /api/users/me`

### 2단계: 모임 코어
- Meeting, Participant, Response(+ 날짜 선택 테이블) 엔티티와 Repository
- 모임 생성(로그인 필요, 호스트 첫 응답 포함), 모임 조회, 초대 토큰 조회(공개), 게스트 응답 제출/수정(guestId 기준 upsert, 마감 후 거부), 응답 현황 조회
- 검증: 응답 마감일/링크 만료일 규칙은 프론트(`CreateMeetingPage`)와 동일하게 서버에서도 검증
- 확정 사항: 응답 마감일은 서버에서 10분 단위(KST 기준, 초 단위 0)를 강제한다. 호스트 닉네임은 로그인한 사용자 이름(최대 20자). `guestId`는 클라이언트가 만든 UUID를 `X-Guest-Id` 헤더로 받고(나중에 서버 발급 방식으로 교체 가능하도록 문자열 컬럼으로 저장), 닉네임 중복은 허용한다. 시간 값은 UTC ISO(`Instant`)로 주고받는다.
- 확정 사항(최종 프론트 연동, 2026-10-10): 후보 날짜 범위는 오늘(KST) 이후, 시작≤종료, 시작·종료 포함 최대 62일이며 응답 날짜는 범위 안이어야 한다. 날짜 동률은 마감 처리 시점의 오늘과 가장 가까운 하나, 장소 동률은 전부 표시한다(3단계). `join`(참여 등록)과 미응답 인원수 집계를 구현한다(MainPage 응답 N/M명용, 인원수만 제공). 추천 후보는 프론트가 보낸 목록을 그대로 저장하고 Kakao 연동(5단계) 때 서버 생성으로 교체한다. 호스트 응답 수정은 `PUT /api/meetings/{id}/response`(호스트만, 마감 전까지)이며 장소 정책도 함께 보낸다. 게스트 응답이 하나라도 있으면 정책 변경은 409 `POLICY_LOCKED`로 막고(정책이 같으면 호스트 본인 응답 수정은 가능), 호스트 수정과 게스트 제출은 모임 행 쓰기 잠금으로 순서를 보장한다. 보류: "다시 추천"(회차/이력)은 AI 단계에서, PROPOSE 공동 제안 장소 이름 공개 API는 구현 직전에 다시 확인한다.

### 3단계: 마감과 결과 산출
- 호스트 수동 마감, 마감 시간 자동 처리(스케줄러)
- 날짜/장소 최다 득표 계산, 동률 처리(`TIE_BREAK`), FinalResult 저장
- 결과 조회 API (프론트 `createMockFinalResult` 로직을 서버로 이전)

### 4단계: 마이페이지
- 프로필 조회/수정, 확정된 내 모임 목록(캘린더 이벤트) API

### 5단계: 장소 검색/추천
- Kakao Local API 서버 프록시(키는 환경변수), 기준 지역 검색, 거리순 추천

### 6단계: 프론트 연동과 테스트
- 프론트 axios 서비스 레이어로 `meetingStorage` 호출부를 페이지별로 교체 (프론트 수정 전에 사용자 확인)
- 서비스/컨트롤러 테스트, MariaDB 전환 검증(로컬 MariaDB에서 전체 플로우 실행)
