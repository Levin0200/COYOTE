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

export const getResponseDeadline = (meeting) => meeting?.responseDeadline || meeting?.responseDueDate;
export const isMeetingClosed = (meeting) => {
  const deadline = new Date(getResponseDeadline(meeting)).getTime();
  return meeting?.status !== "COLLECTING" || (Number.isFinite(deadline) && deadline <= Date.now());
};

export const chooseClosestDate = (dates, referenceDate = new Date()) => {
  if (!dates.length) return "추후 협의";
  const reference = new Date(referenceDate);
  reference.setHours(0, 0, 0, 0);
  return [...dates].sort((a, b) => {
    const distanceA = Math.abs(new Date(`${a}T00:00:00`).getTime() - reference.getTime());
    const distanceB = Math.abs(new Date(`${b}T00:00:00`).getTime() - reference.getTime());
    return distanceA - distanceB || a.localeCompare(b);
  })[0];
};

export const createMockFinalResult = (meetingId, generation = 1) => {
  const responses = getResponses(meetingId);
  const dateCounts = {};
  const placeCounts = {};
  responses.forEach((response) => {
    response.selectedDates?.forEach((date) => { dateCounts[date] = (dateCounts[date] || 0) + 1; });
    if (response.placePreference && response.placeMode !== "OPEN" && response.placeMode !== "SKIP") placeCounts[response.placePreference] = (placeCounts[response.placePreference] || 0) + 1;
  });
  const rankedDates = Object.entries(dateCounts).sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]));
  const bestDateCount = rankedDates[0]?.[1] || 0;
  const tiedDates = rankedDates.filter(([, count]) => count === bestDateCount).map(([value]) => value).sort();
  const selectedDate = chooseClosestDate(tiedDates);
  const rankedPlaces = Object.entries(placeCounts).sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]));
  const bestPlaceCount = rankedPlaces[0]?.[1] || 0;
  const selectedPlaces = rankedPlaces.filter(([, count]) => count === bestPlaceCount).map(([value]) => value);
  const tieBreak = tiedDates.length > 1 || selectedPlaces.length > 1;
  const placeReason = selectedPlaces.length > 1 ? "장소 공동 1위 선택지는 모두 표시합니다." : selectedPlaces.length === 1 ? "가장 많은 참여자가 선택한 장소를 반영했습니다." : "유효한 장소 응답이 없어 장소는 추후 협의로 남겼습니다.";
  return {
    meetingId,
    selectedDate,
    selectedPlaces: selectedPlaces.length ? selectedPlaces : ["추후 협의"],
    selectedPlace: selectedPlaces.length ? selectedPlaces.join(" · ") : "추후 협의",
    decisionType: tieBreak ? "TIE_BREAK" : "MAJORITY",
    reason: `${tiedDates.length > 1 ? "날짜 공동 1위 중 오늘과 가장 가까운 날짜를 우선했습니다. " : "가장 많은 참여자가 선택한 날짜를 반영했습니다. "}${placeReason}`,
    generation,
    createdAt: new Date().toISOString(),
  };
};

export const createId = () => globalThis.crypto.randomUUID();
