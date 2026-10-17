# 프론트엔드 수정 항목 (백엔드 연동용)

| 항목 | 내용 |
|---|---|
| 작성일 | 2026-10-10 |
| 프론트 기준 폴더 | `D:\2학년\캡스톤\COYOTE-frontend\frontend` (git 저장소 아님, 인증 연동 포함 최종본) |
| 백엔드 기준 | 이 워크트리 (브랜치 `claude/backend-frontend-changes-review-33908a`), A1(`265ccff`) 이후 A2·추천 후보 순번 `id`·필드 이름 변경(`meetingId`, `responseDeadline`) 커밋 |
| 서버 준비 상태 | 인증, A1(후보 날짜 범위·장소 정책), A2(호스트 응답·정책 수정), A3(`join`), A4(마감·결과), A5(내 모임 목록) 완료. 4단계(마이페이지)와 5단계(Kakao 장소 추천)는 아직 |
| 이 문서의 원칙 | 프론트 수정은 사용자가 직접 한다. 백엔드 쪽에서 프론트 파일을 바꾸지 않는다 |
| 함께 보는 문서 | `FRONTEND_AUTH_CHANGES.md`(이전 인증 연동 내역, `mystifying-merkle` 워크트리), `PROJECT_STATUS_AND_ROADMAP.md`(같은 워크트리) |

---

## A. 서버가 준비된 부분 (바로 수정 가능)

> 연동 진행 상황(2026-10-10): 1단계(공통 기반) 완료 ✅ — 1(`meetingService.js`), 2(`dateTime.js`), 3(로그인 가드), 12(`meetingErrors.js`). 2단계(모임 생성) 완료 ✅ — 4(62일 검증), 5·6(생성 제출, 호스트 수정·게스트 분기는 3·4단계), 13(입력 길이). 생성 후 이동하는 상태 화면은 4단계 전까지 "모임을 찾을 수 없습니다"로 보임. 3단계(게스트 참여) 완료 ✅ — 7(게스트 정책 읽기), 10(후보 범위·초대 정보), 11(기존 응답 복원), 14(`join`), 게스트 제출. 게스트 상태 화면은 4단계 전까지 "모임을 찾을 수 없습니다"로 보임. 4단계(호스트 현황·수정·마감) 완료 ✅ — 8(정책 잠금, 범위 (a): 장소 정책만 잠금), 9(호스트 수정 데이터), 17(마감·현황). 마감 후 "분석 및 결과 확인" 화면(`MeetingResultPage`)은 5단계 전까지 "모임을 찾을 수 없습니다"로 보임. 5단계(결과) 완료 ✅ — 18(서버 결과 조회, MOCK 문구 제거, 결과 링크는 호스트도 게스트용 주소 복사), 19(다시 추천 버튼은 클릭 시 "준비 중" 안내). `meetingStorage.js`를 쓰는 곳이 없어짐(7단계에서 삭제 여부 확인). 6단계(MainPage) 완료 ✅ — 15(목록·응답 N/M명·진행률·마감 표시), 16(리마인더 기능 삭제, 카드에 "현황 보기"/"결과 보기" 이동 버튼). 7단계(정리·최종 검증) 완료 ✅ — `meetingStorage.js` 삭제, 회원가입부터 로그아웃까지 전체 흐름을 화면에서 확인. 아래 "프론트 변경 파일 목록" 참고.
> 마이페이지 연동(2026-10-12) 완료 ✅ — `src/services/meetingService.js`에 `fetchMeetingVotes` 추가, `src/pages/MyPage/MyPage.jsx`를 서버 데이터로 교체(확정된 내 모임만 캘린더에 표시, 같은 날 모임은 칩 2개 + "+N개"로 펼치기, 지역은 `placeRegion`, 참여 인원은 `participantCount`, 시각 삭제, 상세정보 팝업은 `GET /api/meetings/{id}/votes`). 서버 변경은 커밋 `0eeeeea`.

| # | 파일 | 수정 내용 |
|---|---|---|
| 1 ✅ | 새 파일 `src/services/meetingService.js` | `api`(axios)로 모임 API 함수 작성. 호스트용: `POST /api/meetings`, `GET /api/meetings/{id}`, `PUT /api/meetings/{id}/response`. 게스트용: `GET /api/invites/{token}`, `PUT /api/invites/{token}/response`, `GET /api/invites/{token}/response/me` (헤더 `X-Guest-Id`) |
| 2 ✅ | 새 유틸 (시각 변환) | `responseDeadline`(`2026-10-08T21:30`)을 `new Date(값).toISOString()`으로 바꿔 같은 이름의 필드로 전송. 링크 만료일이 비어 있으면 빈 문자열이 아니라 `null` (빈 문자열은 400) |
| 3 ✅ | `App.jsx`, `AppHeader.jsx` | 로그인 가드 추가: `/create-meeting`, `/meeting/setup/availability`, `/meeting/setup/place`, `/meeting/setup/review`, `/meeting/:meetingId/status`, `/meeting/:meetingId/result`. `/invite/*`는 게스트용이라 가드하지 않음 |
| 4 ✅ | `CreateMeetingPage.jsx` | 후보 날짜 범위가 시작·종료 포함 62일을 넘으면 안내하고 막기 (서버도 400으로 거절) |
| 5 ✅(생성 분기) | `MeetingVotePage.jsx` | `submit()`의 세 갈래를 API로 교체. 모임 생성은 `POST /api/meetings` 후 응답의 모임 번호로 `/meeting/{번호}/status` 이동, 호스트 수정은 `PUT /api/meetings/{id}/response`, 게스트는 `PUT /api/invites/{token}/response`. 로컬 `createId`, `saveParticipant`, `saveResponse` 호출 제거 |
| 6 ✅(생성 분기) | `MeetingVotePage.jsx` | 요청 필드를 서버와 맞춤. 정책은 `placePolicy`(`mode, region, radius, keyword, fixedPlace, options`), 응답은 `selectedDates, placeMode, region, placePreference, recommendationRadius, recommendationKeyword`. `latitude/longitude`는 보내지 않음. 호스트 닉네임 "호스트" 하드코딩은 불필요(서버가 로그인 사용자 이름 사용) |
| 7 ✅(게스트) | `MeetingPlacesPage.jsx` | 게스트는 장소 정책을 `meetingStorage` 대신 초대 정보(`GET /api/invites/{token}`)의 `placePolicy`에서 읽기. 정책 필드 이름이 서버와 같아 구조 변경은 없음. 추천 후보의 `id`는 서버가 순번(1, 2, 3…)으로 내려주므로 `key={place.id}`는 그대로 사용 가능 |
| 8 ✅ | `MeetingPlacesPage.jsx`, `MeetingStatusPage.jsx` | 호스트 수정 화면에서 게스트 응답이 생기면(`responseCount > 1`) 장소 정책 선택을 막고 **"게스트 응답이 있어 장소 선택은 수정 불가합니다."** 안내. 안 막으면 `POLICY_LOCKED` 409. 호스트 본인의 날짜·장소 선택은 가능(정책이 같으면) |
| 9 ✅ | `MeetingStatusPage.jsx` | 호스트 수정 시작(`editHostResponse`) 데이터를 `GET /api/meetings/{id}`의 `myResponse`와 `placePolicy`로 구성 |
| 10 ✅ | `MeetingTimeOptionsPage.jsx`, `NicknamePage.jsx` | 후보 범위, 모임 제목, 마감 여부를 초대 정보(`candidateStartDate`, `candidateEndDate`, `title`, `closed`)에서 읽기. 마감 판단은 로컬 시계 대신 서버의 `closed` 사용. **서버 JSON 필드 이름은 프론트에 맞춰 변경 완료**: 모임 번호는 `meetingId`(호스트 모임 조회·생성 응답), 마감일은 `responseDeadline`(요청·응답 모두). 초대 정보 응답에는 모임 번호가 없고 `responseDeadline`만 있음 |
| 11 ✅ | `NicknamePage.jsx` | 기존 참여 정보 확인을 `GET /api/invites/{token}/response/me`로 대체. 404 `RESPONSE_NOT_FOUND`는 신규 참여, 410 `INVITE_EXPIRED`는 만료 안내, 404 `MEETING_NOT_FOUND`는 잘못된 링크 안내 |
| 12 ✅ | 공통 에러 처리 | 코드별 문구: `INVALID_REQUEST`(서버 `message` 표시), `MEETING_CLOSED`(409), `POLICY_LOCKED`(409), `INVITE_EXPIRED`(410), `FORBIDDEN`(403), `INVALID_GUEST_ID`(400). 기존 `getErrorMessage`가 서버 `message`를 이미 사용 |
| 13 ✅ | 입력 길이 제한 (선택) | 서버 한도: 제목 100자, 설명 1000자, 지역·키워드·확정 장소·장소 이름 100자, 닉네임 20자(프론트에 이미 있음). 나머지는 `maxLength`가 없어 초과하면 400 |

## B. 서버 단계가 끝나야 수정 가능한 부분

| # | 파일 | 의존하는 서버 단계 | 수정 내용 |
|---|---|---|---|
| 14 ✅ | `NicknamePage.jsx` | A3 (`join`, **서버 구현 완료**) | "참여하기" 클릭 시 `POST /api/invites/{token}/join` 호출 (헤더 `X-Guest-Id`, 본문 `{nickname}`). 응답 `{nickname, responded}`: 처음 등록은 201, 같은 `guestId` 재호출은 200(닉네임만 갱신), `responded`는 이미 응답을 제출했는지. 마감 후 409 `MEETING_CLOSED`, 링크 만료 410 `INVITE_EXPIRED`. 이후 가능한 날짜 선택 화면으로 이동하고, 응답 제출은 기존 `PUT /api/invites/{token}/response`를 그대로 사용 |
| 15 ✅ | `MainPage.jsx` | A3, A5 (**서버 구현 완료**) | 목업 제거, `GET /api/meetings`(JWT)로 교체. 항목 필드: `meetingId, inviteToken, title, description, responseDeadline, status, closed, participantCount, responseCount, createdAt, result`. "진행 중" = `closed: false`, "완료" = `closed: true`(필터와 개수는 프론트에서 계산). 응답 N/M명 = `responseCount`/`participantCount`, 진행률 = `responseCount / participantCount`. 완료 모임의 `result`는 `{selectedDate, selectedPlaces, selectedPlace, decisionType}`이고, 마감 시각이 지났지만 서버가 아직 처리하지 않은 최대 1분 동안은 `closed: true`이면서 `result: null`이므로 "결과 집계 중"으로 표시 |
| 16 ✅ | `MainPage.jsx` | 서버 불필요 | **확정(변경, 2026-10-11)**: 팀 논의로 리마인더(미응답자 알림) 기능 자체를 없앰. "미응답자에게 알림 보내기" 버튼, 알림 요청 상태(`remindedMeetings`), 안내 문구를 삭제하고 "초대 링크 복사" 대체 버튼도 두지 않음. 대신 모임 카드에 진행 중은 "현황 보기"(`/meeting/{meetingId}/status`), 완료는 "결과 보기"(`/meeting/{meetingId}/result`) 이동 버튼을 둠(목록에서 현황·응답 수정·마감 화면으로 다시 들어갈 수 있게 하기 위함) |
| 17 ✅ | `MeetingStatusPage.jsx` | A4 (마감, **서버 구현 완료**) | 로컬 상태 변경 제거: 마감 시각 경과 시 자동 `CLOSED`(34~40행), 수동 마감 `updateMeeting`(60행). 수동 마감은 `POST /api/meetings/{id}/close`(응답은 모임 상세, `status`가 `COMPLETED`), 자동 마감은 서버 스케줄러. 서버는 마감 시각이 지나면 `closed: true`를 내려주므로 화면은 `closed`를 기준으로 표시 |
| 18 ✅ | `MeetingResultPage.jsx` | A4 (결과, **서버 구현 완료**) | `ANALYZING`/`COMPLETED` 로컬 상태 변경(26, 38행)과 `createMockFinalResult` 제거. 결과는 서버 조회(호스트 `GET /api/meetings/{id}/result`, 게스트 `GET /api/invites/{token}/result`; 마감 전 409 `MEETING_NOT_CLOSED`, 게스트 링크 만료 410). 응답 필드: `selectedDate`("YYYY-MM-DD", 항상 존재), `selectedPlaces`(목록, 없으면 빈 배열), `selectedPlace`(" · "로 합친 문자열, 없으면 "추후 협의"), `decisionType`, `reason`, `dateVoteCount`, `placeVoteCount`, `totalResponses`, `createdAt`. 서버에는 `generation`/`history`가 없음. 진행바 연출은 유지. "MOCK" 문구와 "현재는 프론트 Mock 집계 결과입니다" 문구 제거. 날짜는 하나, 장소 동률은 목록 표시 유지 |
| 19 ✅(준비 중 안내) | `MeetingResultPage.jsx` | 다시 추천(보류) | "이전 응답으로 다시 추천" 버튼은 서버 지원이 AI 단계로 미뤄졌으니 그때까지 숨기거나 비활성화 |
| 20 | `MeetingPlacesPage.jsx` | 공동 제안 공개 API(보류) | `sharedProposals`(다른 게스트의 제안 장소)가 지금은 로컬 응답 전체를 읽음. 서버 연동 후에는 비어 있게 되므로, API가 생기기 전까지 목록은 숨기고 새 장소 제안만 가능하게 함 |
| 21 | `MeetingPlacesPage.jsx` | 5단계 (Kakao) | `makeOptions`의 목업 후보 3개를 서버 추천으로 교체. 그 전까지는 프론트가 만든 후보를 정책으로 보냄 |
| 22 ✅(MyPage) | `MeetingDetailPage.jsx`, `MyPage.jsx` | A5, 4단계 | 정적/목업 데이터를 서버 확정 모임으로 교체. `time` 표시는 제외하기로 했으니 삭제, `votes`(멤버별 투표)는 마감 후에만 공개 |  <!-- 2026-10-12: MyPage는 서버 연동 완료(확정 모임 캘린더, 상세 팝업의 투표 현황, 시각 표시 삭제). MeetingDetailPage는 팀 논의 후 결정하기로 하고 그대로 둠 -->

## C. 인증 단계에서 남은 항목

| # | 항목 | 현재 상태 | 비고 |
|---|---|---|---|
| 23 | `MyPage.jsx` "회원정보 수정" 버튼 | 버튼(868행)이 `/mypage`(자기 자신)로 이동할 뿐이고, 수정 화면/라우트가 없음 | 서버에는 `PATCH /api/users/me`(이름·주소 수정)가 있음. 화면을 만들지 결정 필요 |
| 24 | 비밀번호 재설정 | 로그인 화면의 버튼이 "준비 중입니다" 알림만 띄움. 프론트 화면·서비스 함수 없음 | 서버에도 비밀번호 재설정/변경 API가 없음. 범위 포함 여부 결정 필요 |
| 25 | 로그인 후 원래 페이지로 복귀 (선택) | 가드에 걸려 `/login`으로 간 뒤 로그인하면 항상 `/mypage`로 이동 | 예: 비로그인 상태에서 "모임 생성하기" 클릭 → 로그인 → 모임 생성 페이지로 돌아가는 기능 |
| 26 | 토큰 만료 (**팀 논의 후 결정, 나중에 다시 언급**) | 토큰이 120분 후 만료되어 `/login`으로 이동 | 작성 중이던 모임 생성 내용은 `sessionStorage`에 남음. 안내 여부는 팀과 논의 |
| 27 | `.gitignore` `.env` 규칙 (**필요할 때 다시 알려주기**) | `*.local`만 무시. `.env.development`는 localhost 주소만 있어 안전 | 비밀값을 넣게 되면 `.env*.local` 규칙 사용 |
| 28 | 개발 서버 포트 | 서버 CORS는 `http://localhost:5173`만 허용 | Vite를 다른 포트로 띄우면 요청이 막힘 |
| 29 | 호스트가 자기 초대 링크로 접속 (선택) | 호스트가 게스트처럼 `join`/응답하면 중복 참여자가 생길 수 있음. 게스트 API는 로그인 없이 쓰므로 서버는 막을 수 없음 | 프론트에서 로그인한 사용자의 `GET /api/meetings` 목록에 같은 `inviteToken`이 있으면 호스트 화면(`/meeting/{meetingId}/status`)으로 보내는 방식을 권장 |

## 결정 대기

1. 회원정보 수정 화면 (23번), 비밀번호 재설정 범위 (24번), 로그인 후 원래 페이지 복귀 (25번).

(10번 서버 필드 이름 변경은 2026-10-10에 완료되어 결정 대기에서 제외)

## 나중에 다시 언급할 항목 (잊지 않기)

- 26번: 토큰 만료 시 처리 — 팀 논의 후 결정.
- 27번: `.gitignore`의 `.env` 규칙 — 비밀값이 필요해질 때.
- 다시 추천(회차/이력): AI 결과 생성기 단계에서 서버 범위 포함 여부 재확인.
- 공동 제안 장소 이름 공개 API: 구현 직전에 재확인 (추천은 이름만 공개).

## 참고: 필드 매핑 (프론트 → 서버)

| 프론트 | 서버 (현재) |
|---|---|
| `meetingId` (UUID) | `meetingId` (숫자, 이름은 같고 값 형식만 다름) |
| `responseDeadline` (`datetime-local`, 시간대 없음) | `responseDeadline` (UTC ISO, 예: `2026-10-12T09:20:00Z`, 이름은 같고 형식만 다름) |
| `isMeetingClosed(meeting)` | `closed` (boolean, 서버 시각 기준) |
| `participants.length` | `participantCount` |
| 응답 수 | `responseCount` |
| 호스트 응답 | `myResponse` (`GET /api/meetings/{id}`) |
| 게스트 응답 | `GET /api/invites/{token}/response/me` |
| 후보 `id` | 목록 순번(1부터)을 서버가 내려줌 |
| 장소 응답 모드 | `FIXED / RECOMMEND / PROPOSE / OPEN` (서버와 동일) |
| 장소 정책 | `placePolicy` (`mode, region, radius, keyword, fixedPlace, options`, 서버와 동일) |

---

## 프론트 변경 파일 목록 (팀원본 `COYOTE-frontend (1)` 대비, 2026-10-11 기준)

최종 폴더: `D:\2학년\캡스톤\COYOTE-frontend\frontend`. 팀 저장소에 올릴 때 아래 파일을 반영하면 됩니다(`node_modules`, `dist`는 제외).

### 신규 (9)
| 파일 | 내용 |
|---|---|
| `.env.development` | `VITE_API_BASE_URL=http://localhost:8080` |
| `src/services/api.js` | axios 인스턴스(토큰 자동 첨부, 401 처리, 에러 코드·메시지 추출) |
| `src/services/authService.js` | 인증 API |
| `src/services/meetingService.js` | 모임 API (호스트용 6개, 게스트용 5개) |
| `src/utils/authToken.js` | 로그인 토큰 보관·만료 확인 |
| `src/utils/dateTime.js` | `datetime-local` → 서버용 UTC 시각 변환 |
| `src/utils/guestId.js` | 게스트 식별자(UUID) 보관 |
| `src/utils/meetingErrors.js` | 서버 에러 코드별 안내 문구 |
| `src/components/RequireAuth.jsx` | 로그인 가드 |

### 수정 (13)
| 파일 | 내용 |
|---|---|
| `src/App.jsx` | 호스트 전용 라우트에 로그인 가드, 온보딩·취향 라우트 제거 |
| `src/pages/auth/LoginPage.jsx`, `SignupPage.jsx` | 서버 로그인·이메일 인증·회원가입 |
| `src/pages/MyPage/MyPage.jsx` | 서버의 이름·주소 표시, 로그아웃 |
| `src/pages/home/MainPage.jsx` | 서버 모임 목록, 리마인더 삭제, 현황·결과 보기 버튼 |
| `src/pages/meeting/CreateMeetingPage.jsx` | 62일 검증, 입력 길이 제한 |
| `src/pages/meeting/MeetingTimeOptionsPage.jsx` | 서버 후보 범위 사용, 초안 없으면 닉네임 화면으로 이동 |
| `src/pages/meeting/MeetingPlacesPage.jsx` | 서버 정책 사용, 게스트 응답이 있으면 정책 잠금, 입력 길이 제한 |
| `src/pages/meeting/MeetingVotePage.jsx` | 모임 생성·게스트 제출·호스트 수정을 서버로 전송 |
| `src/pages/meeting/NicknamePage.jsx` | 초대 정보 조회, 참여 등록(`join`), 기존 응답 복원 |
| `src/pages/meeting/MeetingStatusPage.jsx` | 서버 조회·갱신, 수동 마감, 호스트 수정 시작 |
| `src/pages/meeting/MeetingResultPage.jsx` | 서버 결과 조회, MOCK 문구 제거 |

### 삭제 (2)
| 파일 | 사유 |
|---|---|
| `src/services/meetingStorage.js` | 로컬 저장소 DB 역할 종료, 참조 0건 |
| `src/pages/auth/OnboardingPage.jsx` | 온보딩 화면 제거(인증 연동 단계에서 반영) |

### 변경 없음
`package.json`, `vite.config.js`, `index.html`, `eslint.config.js`, `.gitignore`는 팀원본과 동일합니다.


---

## 장소 검색·구조화 저장 (백엔드 3단계 완료, 프론트 연동 전)

프론트 화면은 아직 이전 방식(장소 이름만 보내기)으로도 그대로 동작한다. 아래는 지도 연동을 할 때 쓸 새 API와 새 필드다. 새 필드는 모두 선택 항목이라 기존 요청과 응답은 깨지지 않는다.

### 1. 장소 검색 API (새로)
- 호스트: `GET /api/places/search` (로그인 필요)
- 게스트: `GET /api/invites/{inviteToken}/places/search` (헤더 `X-Guest-Id` 필요, 호스트가 건너뛰기를 고른 모임에서 마감 전까지만)
- 요청 변수: `latitude`, `longitude`(필수, 지도에서 고른 핀 좌표), `radiusMeters`(기본 1000, 100~4500), `keyword`(선택, "카페", "한식", "고기" 등), `limit`(기본 20, 최대 30)
- 응답: `places[]`(`id`, `name`, `category`, `address`, `latitude`, `longitude`, `distanceMeters`), `appliedCategories`, `ignoredKeywords`(업종으로 못 바꾼 단어, 예 "조용한"), `searchedRadiusMeters`
- 오류: 400 `INVALID_REQUEST`(좌표 누락·한국 밖), 409 `PLACE_SEARCH_NOT_ALLOWED`(게스트가 검색할 수 없는 모임), 409 `MEETING_CLOSED`, 429 `TOO_MANY_REQUESTS`(10분에 30회), 503 `PLACE_SEARCH_UNAVAILABLE`
- 지역 이름 입력은 서버가 좌표로 바꾸지 않는다. 카카오 지도 SDK로 사용자가 핀을 고르면 그 좌표를 보낸다. 화면의 반경 선택지는 1·2·3km 권장(서버 최대 4.5km).

### 2. 후보·응답에 장소 ID 보내기 (선택 항목 추가)
- 추천 후보(`placePolicy.options[]`)에 `placeId` 추가 → 서버가 공식 이름·주소·좌표·업종으로 바꿔 저장한다(`name`은 계속 필수지만 서버가 공식 이름으로 덮어쓴다).
- 모임 생성(`POST /api/meetings`), 호스트 응답 수정(`PUT /api/meetings/{id}/response`), 게스트 응답(`PUT /api/invites/{token}/response`)에 최상위 `placeId` 추가:
  - 장소 제안(`PROPOSE`): 검색 결과의 `id`를 보내면 `placePreference` 없이도 된다.
  - 추천 후보 선택(`RECOMMEND`): 후보의 `placeId`를 보내거나, 예전처럼 `placePreference`에 이름을 보낸다.
- 없는 장소 ID는 400 `PLACE_NOT_FOUND`, 형식이 틀리면 400 `INVALID_REQUEST`, 장소 확인이 불가능하면 503 `PLACE_SEARCH_UNAVAILABLE`.

### 3. 응답에 추가된 필드
- 추천 후보(`placePolicy.options[]`, 모임 상세·초대 정보): `placeId`, `address`, `category`, `latitude`, `longitude` 추가(이름만 받은 예전 후보는 null)
- 내 응답(`myResponse`, `GET .../response/me`, 응답 제출 결과): `place { id, name, address, category, latitude, longitude }` 추가(없으면 null)
- 결과(`GET /api/meetings/{id}/result`, `GET /api/invites/{token}/result`): `selectedPlaceDetails[]`(`id`, `name`, `address`, `latitude`, `longitude`) 추가. 기존 `selectedPlaces`(이름 목록)와 `selectedPlace`는 그대로.

### 4. 아직 안 된 것(백엔드 4단계 이후)
- 단독 1위가 없을 때 군집화·AI로 후보를 만들고 호스트가 고르는 흐름, 후보 새로고침(3개 + 3개), 마감 비동기 처리, 결과 화면의 "호스트가 장소를 고르는 중" 표시


---

## 장소 추천 API (백엔드 4-1 완료, 프론트 연동 전)

호스트가 장소 설정 화면에서 "이 조건으로 장소 추천받기"를 누를 때 쓴다. AI가 근처 후보 20곳 중에서 모임 성격에 맞는 6곳을 순서대로 골라 준다. 첫 화면에는 앞의 3곳을 보여 주고 **새로고침을 누르면 나머지 3곳**을 보여 준다(서버 추가 호출 없음, 새로고침은 한 번만 허용하는 것은 화면에서 처리).

- `POST /api/places/recommend` (로그인 필요)
- 요청(JSON): `latitude`, `longitude`(필수, 지도에서 고른 핀 좌표), `radiusMeters`(기본 1000, 100~4500), `keyword`(선택), `title`, `description`(모임 제목·설명, AI가 모임 성격을 이해하는 데 쓴다. 선택), `participantCount`(선택)
- 응답: `places[]`(`id`, `name`, `category`, `address`, `latitude`, `longitude`, `distanceMeters`, `reason`(AI가 쓴 20자 안팎의 이유, 없으면 null)), `pageSize`(3), `aiUsed`(false면 AI 없이 가까운 순서·업종 다양성으로 고른 것), `appliedCategories`, `ignoredKeywords`, `searchedRadiusMeters`
- 후보가 6곳 이하이면 AI 없이 그대로 내려준다(`aiUsed=false`). AI 키가 없거나 호출이 실패해도 같은 형식으로 규칙 기반 결과가 온다.
- 오류: 400 `INVALID_REQUEST`, 429 `TOO_MANY_REQUESTS`(사용자당 1시간에 10회), 503 `PLACE_SEARCH_UNAVAILABLE`
- 호스트가 고른 장소는 모임 생성 요청의 `placePolicy.options[]`에 `placeId`로 보내면 된다(위 "장소 검색·구조화 저장" 항목 참고). 호스트가 후보를 새로고침해서 6곳을 모두 후보로 보낼 수도 있다(후보는 최대 10곳).

## 마감 후 장소 결정 (호스트 선택, 자동 선정) — 2026-10-17

### 결과 응답에 추가된 필드
`GET /api/meetings/{id}/result`(호스트), `GET /api/invites/{token}/result`(게스트) 공통. 기존 필드는 그대로다.

| 필드 | 설명 |
|---|---|
| `placeStatus` | `PENDING`(후보 분석 중) / `AWAITING_HOST`(호스트 선택 대기) / `DECIDED`(확정) / `NONE`(추후 협의) |
| `hostDecisionDeadline` | 호스트가 고를 수 있는 마감 시각(UTC ISO). `AWAITING_HOST`일 때만 있고 아니면 없음. 이때까지 안 고르면 서버가 첫 번째 후보로 자동 확정한다 |
| `placeCandidates[]` | 호스트가 고를 후보(`id`, `name`, `address`, `category`, `latitude`, `longitude`, `votes`, `reason`). **호스트 조회에서 `AWAITING_HOST`일 때만** 채워지고 게스트 조회는 항상 빈 배열 |

- `selectedPlaces`, `selectedPlaceDetails`, `selectedPlace`는 **`DECIDED`일 때만** 장소가 들어간다. 그 외에는 빈 배열이고 `selectedPlace`는 "추후 협의"다. 화면에서 `placeStatus`로 분기해야 "추후 협의"와 "호스트가 고르는 중"을 구분할 수 있다.
- `decisionType`에 `SPATIAL`(흩어진 제안을 합의 지역으로 묶음), `HOST_CHOICE`(호스트가 선택), `AUTO_PICK`(시한 지나 자동 선정)이 추가됐다. 기존 `MAJORITY`, `TIE_BREAK`도 그대로다.
- 날짜(`selectedDate`)는 마감 즉시 정해지므로 `PENDING`이어도 값이 있다.
- 내 모임 목록(`GET /api/meetings`)의 `result`에도 `placeStatus`가 추가됐다. 확정 전에는 `selectedPlace`가 "추후 협의"이므로 캘린더에는 `DECIDED`일 때만 장소를 보여 주는 편이 좋다.

### 화면 흐름 제안
| `placeStatus` | 호스트 화면 | 게스트 화면 |
|---|---|---|
| `PENDING` | "후보를 분석하고 있어요" (몇 초 뒤 다시 조회) | 같음 |
| `AWAITING_HOST` | 후보 목록(지도 마커 + 카드)에서 하나 선택, 남은 시간 표시 | "호스트가 장소를 정하고 있어요" + 남은 시간(후보는 안 보임) |
| `DECIDED` | 확정 장소(주소·지도) | 같음 |
| `NONE` | "장소는 추후 협의" | 같음 |

- 마감 직후 `PENDING`일 수 있으므로, `PENDING`이면 2~3초 간격으로 다시 조회한다(보통 수 초 안에 끝난다).
- 모임 상세(`GET /api/meetings/{id}`)의 `status`는 `PENDING` 동안 `CLOSED`, 끝나면 `COMPLETED`다.

### 새 API: 호스트가 최종 장소 확정
`POST /api/meetings/{id}/result/place` (JWT, 호스트만)

요청 본문: `{ "placeId": "후보의 id" }` — 장소 ID가 없는 예전 후보는 `{ "name": "후보 이름" }`.
성공(200): 호스트용 결과 응답(`placeStatus`가 `DECIDED`, `decisionType`이 `HOST_CHOICE`).

| 상태 | `code` | 의미 |
|---|---|---|
| 400 | `INVALID_CANDIDATE` | 후보에 없는 장소 |
| 400 | `INVALID_REQUEST` | `placeId`와 `name`이 모두 비어 있음 |
| 403 | `FORBIDDEN` | 내 모임이 아님 |
| 409 | `MEETING_NOT_CLOSED` | 아직 마감 전 |
| 409 | `PLACE_NOT_AWAITING` | 이미 확정됐거나 선택을 기다리는 상태가 아님 |

프론트 `meetingErrors.js`에 `INVALID_CANDIDATE`, `PLACE_NOT_AWAITING` 안내 문구를 추가하면 좋다.
