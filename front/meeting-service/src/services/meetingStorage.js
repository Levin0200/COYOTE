const KEYS = {
  meetings: "coyoteMeetings",
  participants: "coyoteParticipants",
  responses: "coyoteMeetingResponses",
  results: "coyoteFinalResults",
};

export const readList = (key) => {
  try {
    const value = JSON.parse(localStorage.getItem(key) || "[]");
    return Array.isArray(value) ? value : [];
  } catch {
    return [];
  }
};

const saveList = (key, value) => localStorage.setItem(key, JSON.stringify(value));

export const getMeetings = () => readList(KEYS.meetings);
export const getMeetingById = (meetingId) => getMeetings().find((item) => item.meetingId === meetingId);
export const getMeetingByInviteToken = (inviteToken) => getMeetings().find((item) => item.inviteToken === inviteToken);
export const saveMeeting = (meeting) => saveList(KEYS.meetings, [...getMeetings().filter((item) => item.meetingId !== meeting.meetingId), meeting]);
export const updateMeeting = (meetingId, changes) => {
  const meeting = getMeetingById(meetingId);
  if (!meeting) return null;
  const updated = { ...meeting, ...changes };
  saveMeeting(updated);
  return updated;
};

export const getParticipants = (meetingId) => readList(KEYS.participants).filter((item) => item.meetingId === meetingId);
export const getParticipantByGuestId = (meetingId, guestId) => getParticipants(meetingId).find((item) => item.guestId === guestId);
export const saveParticipant = (participant) => saveList(KEYS.participants, [...readList(KEYS.participants).filter((item) => item.participantId !== participant.participantId), participant]);

export const getResponses = (meetingId) => readList(KEYS.responses).filter((item) => item.meetingId === meetingId);
export const getResponseByParticipant = (meetingId, participantId) => getResponses(meetingId).find((item) => item.participantId === participantId);
export const saveResponse = (response) => saveList(KEYS.responses, [...readList(KEYS.responses).filter((item) => item.responseId !== response.responseId), response]);

export const getFinalResult = (meetingId) => readList(KEYS.results).find((item) => item.meetingId === meetingId);
export const saveFinalResult = (result) => saveList(KEYS.results, [...readList(KEYS.results).filter((item) => item.meetingId !== result.meetingId), result]);

export const isMeetingClosed = (meeting) => meeting?.status !== "COLLECTING" || new Date(meeting.responseDueDate).getTime() <= Date.now();

export const createMockFinalResult = (meetingId) => {
  const responses = getResponses(meetingId);
  const dateCounts = {};
  const placeCounts = {};
  responses.forEach((response) => {
    response.selectedDates?.forEach((date) => { dateCounts[date] = (dateCounts[date] || 0) + 1; });
    if (response.placePreference) placeCounts[response.placePreference] = (placeCounts[response.placePreference] || 0) + 1;
  });
  const choose = (counts) => {
    const entries = Object.entries(counts).sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]));
    const tied = entries.length > 1 && entries[0][1] === entries[1][1];
    return { value: entries[0]?.[0] || "추후 협의", count: entries[0]?.[1] || 0, tied };
  };
  const date = choose(dateCounts);
  const place = choose(placeCounts);
  const tieBreak = date.tied || place.tied;
  return {
    meetingId,
    selectedDate: date.value,
    selectedPlace: place.value,
    decisionType: tieBreak ? "TIE_BREAK" : "MAJORITY",
    reason: tieBreak
      ? "최다 응답 후보가 동률이어서 전체 응답에서 반복적으로 선택된 후보와 정렬 기준을 적용해 공정하게 결정한 Mock 결과입니다."
      : "가장 많은 참여자가 선택한 날짜와 장소를 기준으로 결정했습니다.",
    createdAt: new Date().toISOString(),
  };
};

export const createId = () => globalThis.crypto.randomUUID();
