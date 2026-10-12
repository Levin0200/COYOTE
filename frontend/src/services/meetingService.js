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

// 마감된 모임의 날짜별 득표 수와 멤버별 투표(호스트만, 마감 후에만)
export const fetchMeetingVotes = (meetingId) =>
  api.get(`/api/meetings/${meetingId}/votes`).then((response) => response.data);

// ---- 게스트용 (로그인 불필요, guestId를 X-Guest-Id 헤더로 전달) ----

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
