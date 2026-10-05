import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { formatSelectedDate } from "../../utils/dateFormat";
import { getMeetingById, getMeetingByInviteToken, getParticipantByGuestId, getParticipants, getResponseByParticipant, getResponseDeadline, getResponses, updateMeeting } from "../../services/meetingStorage";

const remainingText = (dueDate, now) => {
  const dueTime = new Date(dueDate).getTime();
  if (!Number.isFinite(dueTime)) return "마감일 미설정";
  const ms = dueTime - now;
  if (ms <= 0) return "응답 마감";
  const hours = Math.floor(ms / 3600000);
  const days = Math.floor(hours / 24);
  return `${days ? `${days}일 ` : ""}${hours % 24}시간 ${Math.floor((ms % 3600000) / 60000)}분`;
};

function MeetingStatusPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { meetingId, inviteToken } = useParams();
  const isGuest = location.pathname.startsWith("/invite/");
  const [meeting, setMeeting] = useState(() => isGuest ? getMeetingByInviteToken(inviteToken) : getMeetingById(meetingId));
  const [now, setNow] = useState(0);
  const [confirmClose, setConfirmClose] = useState(false);
  const responseDeadline = getResponseDeadline(meeting);
  const responseDeadlineTime = new Date(responseDeadline).getTime();

  useEffect(() => {
    const refresh = () => setNow(Date.now());
    refresh();
    const id = window.setInterval(refresh, 60000);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    if (meeting && now && meeting.status === "COLLECTING" && Number.isFinite(responseDeadlineTime) && now >= responseDeadlineTime) {
      const id = window.setTimeout(() => setMeeting(updateMeeting(meeting.meetingId, { status: "CLOSED" })), 0);
      return () => window.clearTimeout(id);
    }
    return undefined;
  }, [meeting, now, responseDeadlineTime]);

  if (!meeting) return <div className="flex min-h-screen items-center justify-center bg-slate-50"><div className="rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">모임을 찾을 수 없습니다.</h1><button onClick={() => navigate("/")} className="mt-5 rounded-xl bg-blue-600 px-5 py-3 font-bold text-white">홈으로</button></div></div>;

  const participants = getParticipants(meeting.meetingId);
  const responses = getResponses(meeting.meetingId);
  const guestId = isGuest ? localStorage.getItem(`coyote_guest_${inviteToken}`) : null;
  const participant = isGuest ? getParticipantByGuestId(meeting.meetingId, guestId) : null;
  const myResponse = participant ? getResponseByParticipant(meeting.meetingId, participant.participantId) : null;
  const hostParticipant = !isGuest ? participants.find((item) => item.role === "HOST") : null;
  const hostResponse = hostParticipant ? getResponseByParticipant(meeting.meetingId, hostParticipant.participantId) : null;
  const closed = meeting.status !== "COLLECTING";
  const expired = isGuest && meeting.linkExpirationDate && now >= new Date(meeting.linkExpirationDate).getTime();
  const invitePath = `/invite/${meeting.inviteToken}`;

  const copyInviteLink = async () => {
    const url = `${window.location.origin}${invitePath}`;
    try { await navigator.clipboard.writeText(url); alert("초대 링크를 복사했습니다."); } catch { window.prompt("초대 링크를 복사해 주세요.", url); }
  };
  const closeMeeting = () => {
    setMeeting(updateMeeting(meeting.meetingId, { status: "CLOSED", manuallyClosed: true }));
    setConfirmClose(false);
  };
  const editHostResponse = () => {
    if (!hostResponse) return alert("수정할 호스트 응답을 찾을 수 없습니다.");
    sessionStorage.setItem("coyoteMeetingDraft", JSON.stringify({
      ...hostResponse,
      editingMeetingId: meeting.meetingId,
      title: meeting.title,
      description: meeting.description,
      candidateStartDate: meeting.candidateStartDate,
      candidateEndDate: meeting.candidateEndDate,
      responseDeadline: getResponseDeadline(meeting),
      linkExpirationDate: meeting.linkExpirationDate,
    }));
    navigate("/meeting/setup/availability");
  };

  if (expired) return <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6"><div className="max-w-md rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">초대 링크가 만료되었습니다.</h1><p className="mt-3 text-slate-500">게스트는 더 이상 이 모임에 접근할 수 없습니다.</p></div></div>;

  return (
    <div className="min-h-screen bg-slate-50 px-6 py-10 text-slate-800"><div className="mx-auto max-w-5xl"><header className="mb-8"><div className="mb-4 flex items-center gap-3"><span className={`rounded-full px-4 py-2 text-sm font-bold ${closed ? "bg-amber-100 text-amber-700" : "bg-blue-100 text-blue-700"}`}>{closed ? "응답 마감" : "투표 진행 중"}</span><span className="text-sm text-slate-400">{isGuest ? "GUEST VIEW" : "HOST VIEW"}</span></div><h1 className="text-4xl font-black text-slate-950">{meeting.title}</h1><p className="mt-3 text-slate-500">{closed ? "응답이 마감되었습니다. 결과 분석 단계로 이동할 수 있어요." : "참여자들의 응답을 기다리고 있어요."}</p></header>
      <div className="mb-6 grid gap-4 sm:grid-cols-2"><div className="rounded-3xl bg-blue-600 p-7 text-white shadow-md"><p className="text-sm font-semibold text-blue-100">현재 응답</p><p className="mt-2 text-4xl font-black">{responses.length}명</p></div><div className="rounded-3xl border border-slate-200 bg-white p-7 shadow-sm"><p className="text-sm font-semibold text-slate-400">응답 마감까지</p><p className="mt-2 text-3xl font-black text-slate-900">{closed ? "응답 마감" : remainingText(responseDeadline, now)}</p><p className="mt-2 text-xs text-slate-400">{Number.isFinite(responseDeadlineTime) ? new Date(responseDeadlineTime).toLocaleString("ko-KR") : "마감일 정보가 없습니다."}</p></div></div>
      {!isGuest && !closed && <div className="mb-6 grid gap-4 md:grid-cols-[1fr_auto]"><div className="rounded-3xl border border-blue-100 bg-blue-50 p-6"><p className="text-sm font-bold text-blue-600">게스트 초대 링크</p><div className="mt-3 flex gap-3"><code className="min-w-0 flex-1 overflow-hidden text-ellipsis whitespace-nowrap rounded-xl bg-white px-4 py-3 text-sm text-slate-600">{invitePath}</code><button onClick={copyInviteLink} className="rounded-xl bg-blue-600 px-5 py-3 text-sm font-bold text-white">링크 복사</button></div></div><button onClick={() => setConfirmClose(true)} className="rounded-3xl border border-red-200 bg-white px-7 py-5 font-bold text-red-600 hover:bg-red-50">응답 조기 마감</button></div>}
      {isGuest ? <section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm"><h2 className="text-xl font-black text-slate-900">내 응답</h2>{myResponse ? <><p className="mt-5 text-sm font-bold text-slate-400">가능한 날짜</p><div className="mt-2 flex flex-wrap gap-2">{myResponse.selectedDates.map((date) => <span key={date} className="rounded-xl bg-blue-50 px-3 py-2 text-sm font-bold text-blue-700">{formatSelectedDate(date)}</span>)}</div><p className="mt-5 text-sm font-bold text-slate-400">장소</p><p className="mt-2 font-black text-slate-800">{myResponse.placeMode === "SKIP" ? "PASS · 나중에 정하기" : myResponse.placePreference}</p>{!closed && <button onClick={() => { sessionStorage.setItem(`coyoteGuestDraft_${inviteToken}`, JSON.stringify({ ...myResponse, guestId, nickname: participant.nickname, inviteToken, candidateStartDate: meeting.candidateStartDate, candidateEndDate: meeting.candidateEndDate })); navigate(`/invite/${inviteToken}/availability`); }} className="mt-6 rounded-2xl bg-blue-600 px-6 py-3 font-bold text-white">내 응답 수정</button>}</> : <p className="mt-4 text-slate-500">아직 저장된 응답이 없습니다.</p>}</section> : <section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm"><h2 className="text-xl font-black text-slate-900">모임 상태</h2><p className="mt-3 text-slate-500">개별 참여자와 세부 선택은 응답 기간 중 공개하지 않습니다.</p><div className="mt-5 flex items-center justify-between rounded-2xl bg-slate-50 p-4"><span className="font-bold">참여 등록</span><span className="font-black text-blue-600">{participants.length}명</span></div>{!closed && hostResponse && <button onClick={editHostResponse} className="mt-5 rounded-2xl bg-blue-600 px-6 py-3 font-bold text-white hover:bg-blue-700">내 응답 수정</button>}</section>}
      {closed && <button onClick={() => navigate(isGuest ? `/invite/${inviteToken}/result` : `/meeting/${meeting.meetingId}/result`)} className="mt-6 w-full rounded-2xl bg-purple-600 py-4 font-black text-white shadow-md hover:bg-purple-700">분석 및 결과 확인</button>}
      {confirmClose && <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-6"><div className="w-full max-w-md rounded-[2rem] bg-white p-8 shadow-2xl"><h2 className="text-2xl font-black text-slate-950">응답을 지금 마감하시겠습니까?</h2><p className="mt-3 leading-6 text-slate-500">마감 후에는 참여자가 응답을 수정할 수 없습니다.</p><div className="mt-7 flex gap-3"><button onClick={() => setConfirmClose(false)} className="flex-1 rounded-2xl border border-slate-200 py-3 font-bold">취소</button><button onClick={closeMeeting} className="flex-1 rounded-2xl bg-red-600 py-3 font-bold text-white">마감하기</button></div></div></div>}
    </div></div>
  );
}

export default MeetingStatusPage;
