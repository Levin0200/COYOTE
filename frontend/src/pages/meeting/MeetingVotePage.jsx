import { useLocation, useNavigate, useParams } from "react-router-dom";
import { formatSelectedDate } from "../../utils/dateFormat";
import { createId, getMeetingById, getMeetingByInviteToken, getParticipantByGuestId, getParticipants, getResponseByParticipant, isMeetingClosed, saveMeeting, saveParticipant, saveResponse, updateMeeting } from "../../services/meetingStorage";

const readDraft = (key) => { try { return JSON.parse(sessionStorage.getItem(key) || "{}"); } catch { return {}; } };
const placeLabel = (draft) => draft.placeMode === "OPEN" ? "내 장소 의견 건너뛰기" : draft.placeMode === "RECOMMEND" ? `${draft.placePreference} · 공통 범위 추천` : draft.placeMode === "PROPOSE" ? `${draft.placePreference} · 게스트 공동 제안` : draft.placePreference;
const responsePlaceFields = (draft) => ({ placeMode: draft.placeMode, region: ["RECOMMEND", "PROPOSE"].includes(draft.placeMode) ? draft.region || "" : "", placePreference: ["FIXED", "RECOMMEND", "PROPOSE"].includes(draft.placeMode) ? draft.placePreference || "" : "", recommendationRadius: ["RECOMMEND", "PROPOSE"].includes(draft.placeMode) ? draft.recommendationRadius || null : null, recommendationKeyword: ["RECOMMEND", "PROPOSE"].includes(draft.placeMode) ? draft.recommendationKeyword || "" : "" });
const placePolicyFields = (draft) => ({ mode: draft.placeMode, region: draft.placeMode === "RECOMMEND" ? draft.region || "" : "", radius: draft.placeMode === "RECOMMEND" ? draft.recommendationRadius || null : null, keyword: draft.placeMode === "RECOMMEND" ? draft.recommendationKeyword || "" : "", fixedPlace: draft.placeMode === "FIXED" ? draft.fixedPlace || "" : "", options: draft.placeMode === "RECOMMEND" ? draft.recommendationOptions || [] : [] });

function MeetingVotePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { inviteToken } = useParams();
  const isGuest = location.pathname.startsWith("/invite/");
  const draftKey = isGuest ? `coyoteGuestDraft_${inviteToken}` : "coyoteMeetingDraft";
  const draft = readDraft(draftKey);
  const isHostEditing = !isGuest && Boolean(draft.editingMeetingId);

  const submit = () => {
    if (!draft.selectedDates?.length || !draft.placeMode) return alert("날짜와 장소 응답을 먼저 입력해주세요.");
    if (["FIXED", "RECOMMEND", "PROPOSE"].includes(draft.placeMode) && !draft.placePreference) return alert("선택한 장소 정보가 없습니다. 장소 응답 단계에서 다시 선택해주세요.");
    if (isGuest) {
      const meeting = getMeetingByInviteToken(inviteToken);
      if (!meeting) return alert("초대 정보를 찾을 수 없습니다.");
      if (isMeetingClosed(meeting)) return alert("응답이 마감되어 수정할 수 없습니다.");
      const existingParticipant = getParticipantByGuestId(meeting.meetingId, draft.guestId);
      const participant = existingParticipant || { participantId: createId(), meetingId: meeting.meetingId, nickname: draft.nickname, role: "GUEST", guestId: draft.guestId, createdAt: new Date().toISOString() };
      saveParticipant({ ...participant, nickname: draft.nickname });
      const existingResponse = getResponseByParticipant(meeting.meetingId, participant.participantId);
      saveResponse({ responseId: existingResponse?.responseId || createId(), meetingId: meeting.meetingId, participantId: participant.participantId, selectedDates: draft.selectedDates, ...responsePlaceFields(draft), latitude: null, longitude: null, createdAt: existingResponse?.createdAt || new Date().toISOString(), updatedAt: new Date().toISOString() });
      sessionStorage.removeItem(draftKey);
      navigate(`/invite/${inviteToken}/status`);
      return;
    }

    if (draft.editingMeetingId) {
      const meeting = getMeetingById(draft.editingMeetingId);
      if (!meeting) return alert("수정할 모임을 찾을 수 없습니다.");
      if (isMeetingClosed(meeting)) return alert("응답이 마감되어 수정할 수 없습니다.");
      const hostParticipant = getParticipants(meeting.meetingId).find((participant) => participant.role === "HOST");
      if (!hostParticipant) return alert("호스트 참여 정보를 찾을 수 없습니다.");
      const existingResponse = getResponseByParticipant(meeting.meetingId, hostParticipant.participantId);
      if (!existingResponse) return alert("수정할 호스트 응답을 찾을 수 없습니다.");
      saveResponse({ ...existingResponse, selectedDates: draft.selectedDates, ...responsePlaceFields(draft), updatedAt: new Date().toISOString() });
      updateMeeting(meeting.meetingId, { placePolicy: placePolicyFields(draft) });
      sessionStorage.removeItem(draftKey);
      navigate(`/meeting/${meeting.meetingId}/status`);
      return;
    }

    const meetingId = createId();
    const token = createId();
    const participantId = createId();
    saveMeeting({ meetingId, inviteToken: token, title: draft.title, description: draft.description, candidateStartDate: draft.candidateStartDate, candidateEndDate: draft.candidateEndDate, responseDeadline: draft.responseDeadline || draft.responseDueDate, linkExpirationDate: draft.linkExpirationDate, placePolicy: placePolicyFields(draft), status: "COLLECTING", manuallyClosed: false, createdAt: new Date().toISOString() });
    saveParticipant({ participantId, meetingId, nickname: "호스트", role: "HOST", guestId: null, createdAt: new Date().toISOString() });
    saveResponse({ responseId: createId(), meetingId, participantId, selectedDates: draft.selectedDates, ...responsePlaceFields(draft), latitude: null, longitude: null, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() });
    sessionStorage.removeItem(draftKey);
    navigate(`/meeting/${meetingId}/status`);
  };

  if (!draft.selectedDates) return <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6"><div className="rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">확인할 응답이 없습니다.</h1><button onClick={() => navigate(isGuest ? `/invite/${inviteToken}/availability` : "/create-meeting")} className="mt-5 rounded-2xl bg-blue-600 px-6 py-3 font-bold text-white">처음부터 입력하기</button></div></div>;

  return (
    <div className="min-h-screen bg-slate-50 px-6 py-10 text-slate-800"><div className="mx-auto max-w-4xl"><header className="mb-10"><p className="mb-3 text-sm font-semibold text-blue-600">Response review</p><h1 className="mb-3 text-4xl font-black text-slate-950">{isGuest || isHostEditing ? "내 응답 확인" : "모임 생성 확인"}</h1><p className="text-slate-500">제출하기 전 선택한 내용을 확인해 주세요.</p></header>
      {!isGuest && <section className="mb-6 rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm"><p className="text-sm font-bold text-slate-400">모임 정보</p><h2 className="mt-2 text-2xl font-black text-slate-900">{draft.title}</h2><p className="mt-2 text-slate-500">{draft.description || "작성된 설명이 없습니다."}</p><p className="mt-4 text-sm font-semibold text-blue-600">응답 마감 · {new Date(draft.responseDeadline || draft.responseDueDate).toLocaleString("ko-KR")}</p></section>}
      <div className="grid gap-6 md:grid-cols-2"><section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm"><div className="flex items-center justify-between"><h2 className="text-xl font-black text-slate-900">내가 선택한 날짜</h2><span className="rounded-full bg-blue-50 px-3 py-1 text-sm font-bold text-blue-600">{draft.selectedDates.length}일</span></div><div className="mt-5 space-y-3">{draft.selectedDates.map((date) => <div key={date} className="rounded-2xl bg-slate-50 p-4 font-bold">📅 {formatSelectedDate(date)}</div>)}</div></section><section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm"><h2 className="text-xl font-black text-slate-900">장소 응답</h2><div className={`mt-5 rounded-2xl p-5 ${["FIXED", "RECOMMEND", "PROPOSE"].includes(draft.placeMode) && !draft.placePreference ? "bg-red-50" : "bg-blue-50"}`}><p className="text-xs font-bold text-blue-500">{draft.placeMode === "PROPOSE" ? "게스트 장소 제안" : draft.placeMode}</p><p className="mt-2 text-lg font-black text-blue-900">📍 {placeLabel(draft) || "장소가 선택되지 않았습니다"}</p>{draft.region && <p className="mt-2 text-sm text-blue-600">기준 지역: {draft.region}</p>}</div></section></div>
      <div className="mt-8 flex gap-4"><button onClick={() => navigate(-1)} className="w-1/3 rounded-2xl border border-slate-200 bg-white py-4 font-bold">이전으로</button><button onClick={submit} className="w-2/3 rounded-2xl bg-blue-600 py-4 font-black text-white shadow-md hover:bg-blue-700">{isGuest || isHostEditing ? "응답 수정 완료" : "모임 생성하기"}</button></div>
    </div></div>
  );
}

export default MeetingVotePage;
