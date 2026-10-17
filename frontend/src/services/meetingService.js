import api from "./api";

const guestHeaders = (guestId) => ({ "X-Guest-Id": guestId });

// ---- 호스트용 (로그인 필요, JWT는 api 인스턴스가 자동 첨부) ----

export const createMeeting = (payload) =>
  api.post("/api/meetings", payload).then((response) => response.data);

export const fetchMyMeetings = () =>
  api.get("/api/meetings").then((response) => response.data);

export const fetchHostMeeting = (meetingId) =>
  api.get(`/api/meetings/${meetingId}`).then((response) => response.data);

export const updateHostResponse = (meetingId, payload) =>
  api
    .put(`/api/meetings/${meetingId}/response`, payload)
    .then((response) => response.data);

export const closeMeeting = (meetingId) =>
  api.post(`/api/meetings/${meetingId}/close`).then((response) => response.data);

export const fetchHostResult = (meetingId) =>
  api.get(`/api/meetings/${meetingId}/result`).then((response) => response.data);

// 호스트가 후보 중에서 최종 장소를 확정한다. payload: { placeId } (장소 ID가 없는 예전 후보는 { name })
export const choosePlace = (meetingId, payload) =>
  api.post(`/api/meetings/${meetingId}/result/place`, payload).then((response) => response.data);

// 마감된 모임의 날짜별 득표 수와 멤버별 투표(호스트만, 마감 후에만)
export const fetchMeetingVotes = (meetingId) =>
  api.get(`/api/meetings/${meetingId}/votes`).then((response) => response.data);

// ---- 장소 검색·추천 (호스트) ----

// 지도에서 고른 위치 주변의 장소를 가까운 순서로 찾는다. params: { latitude, longitude, radiusMeters, keyword, limit }
export const searchPlaces = (params) =>
  api.get("/api/places/search", { params }).then((response) => response.data);

// 근처 후보 중에서 AI가 모임에 맞는 6곳을 골라 준다(첫 화면 3곳 + 새로고침 3곳).
// payload: { latitude, longitude, radiusMeters, keyword, title, description, participantCount }
export const recommendPlaces = (payload) =>
  api.post("/api/places/recommend", payload).then((response) => response.data);

// ---- 게스트용 (로그인 불필요, guestId를 X-Guest-Id 헤더로 전달) ----

// 호스트가 장소를 게스트 제안에 열어 둔 모임에서 게스트가 장소를 검색한다.
export const searchPlacesAsGuest = (inviteToken, guestId, params) =>
  api
    .get(`/api/invites/${inviteToken}/places/search`, { params, headers: guestHeaders(guestId) })
    .then((response) => response.data);

export const fetchInviteInfo = (inviteToken) =>
  api.get(`/api/invites/${inviteToken}`).then((response) => response.data);

export const joinMeeting = (inviteToken, guestId, nickname) =>
  api
    .post(
      `/api/invites/${inviteToken}/join`,
      { nickname },
      { headers: guestHeaders(guestId) }
    )
    .then((response) => response.data);

export const submitGuestResponse = (inviteToken, guestId, payload) =>
  api
    .put(`/api/invites/${inviteToken}/response`, payload, {
      headers: guestHeaders(guestId),
    })
    .then((response) => response.data);

export const fetchMyGuestResponse = (inviteToken, guestId) =>
  api
    .get(`/api/invites/${inviteToken}/response/me`, {
      headers: guestHeaders(guestId),
    })
    .then((response) => response.data);

export const fetchGuestResult = (inviteToken) =>
  api.get(`/api/invites/${inviteToken}/result`).then((response) => response.data);
