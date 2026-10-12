import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { formatSelectedDate } from "../../utils/dateFormat";
import { getGuestId } from "../../utils/guestId";
import { getErrorCode } from "../../services/api";
import { getMeetingErrorMessage } from "../../utils/meetingErrors";
import { closeMeeting as closeMeetingApi, fetchHostMeeting, fetchInviteInfo, fetchMyGuestResponse } from "../../services/meetingService";

const POLL_MS = 60000;

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
  const [meeting, setMeeting] = useState(null);
  const [myResponse, setMyResponse] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [now, setNow] = useState(0);
  const [confirmClose, setConfirmClose] = useState(false);
  const [closing, setClosing] = useState(false);
  const loadedOnce = useRef(false);
  const guestId = isGuest ? getGuestId(inviteToken) : null;

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        if (isGuest) {
          const info = await fetchInviteInfo(inviteToken);
          let mine = null;
          if (guestId) {
            try {
              mine = await fetchMyGuestResponse(inviteToken, guestId);
            } catch (error) {
              if (getErrorCode(error) !== "RESPONSE_NOT_FOUND") throw error;
            }
          }
          if (cancelled) return;
          setMeeting(info);
          setMyResponse(mine);
        } else {
          const detail = await fetchHostMeeting(meetingId);
          if (cancelled) return;
          setMeeting(detail);
        }
        loadedOnce.current = true;
        setLoadError(null);
      } catch (error) {
        const code = getErrorCode(error);
        if (!cancelled && (!loadedOnce.current || code === "INVITE_EXPIRED")) setLoadError({ code, message: getMeetingErrorMessage(error) });
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    const tick = () => setNow(Date.now());
    tick();
    load();
    const id = window.setInterval(() => { tick(); load(); }, POLL_MS);
    return () => { cancelled = true; window.clearInterval(id); };
  }, [isGuest, inviteToken, meetingId, guestId]);

  const responseDeadline = meeting?.responseDeadline;
  const responseDeadlineTime = new Date(responseDeadline).getTime();

  if (loading) return <div className="flex min-h-screen items-center justify-center bg-slate-50"><p className="font-bold text-slate-400">불러오는 중...</p></div>;

  const expired = loadError?.code === "INVITE_EXPIRED" || Boolean(isGuest && meeting?.linkExpirationDate && now >= new Date(meeting.linkExpirationDate).getTime());

  if (!meeting && !expired) return <div className="flex min-h-screen items-center justify-center bg-slate-50"><div className="rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">모임을 찾을 수 없습니다.</h1>{loadError && loadError.code !== "MEETING_NOT_FOUND" && <p className="mt-3 text-slate-500">{loadError.message}</p>}<button onClick={() => navigate("/")} className="mt-5 rounded-xl bg-blue-600 px-5 py-3 font-bold text-white">홈으로</button></div></div>;

  const closed = Boolean(meeting?.closed);
  const participantCount = meeting?.participantCount ?? 0;
  const responseCount = meeting?.responseCount ?? 0;
  const hostResponse = !isGuest ? meeting?.myResponse : null;
  const invitePath = `/invite/${isGuest ? inviteToken : meeting?.inviteToken}`;

  const copyInviteLink = async () => {
    const url = `${window.location.origin}${invitePath}`;
    try { await navigator.clipboard.writeText(url); alert("초대 링크를 복사했습니다."); } catch { window.prompt("초대 링크를 복사해 주세요.", url); }
  };
  const closeMeeting = async () => {
    setClosing(true);
    try {
      setMeeting(await closeMeetingApi(meeting.meetingId));
    } catch (error) {
      alert(getMeetingErrorMessage(error));
    } finally {
      setClosing(false);
      setConfirmClose(false);
    }
  };
  const editHostResponse = () => {
    if (!hostResponse) return alert("수정할 호스트 응답을 찾을 수 없습니다.");
    const policy = meeting.placePolicy || {};
    sessionStorage.setItem("coyoteMeetingDraft", JSON.stringify({
      ...hostResponse,
      editingMeetingId: meeting.meetingId,
      policyLocked: responseCount > 1,
      placePolicy: policy,
      title: meeting.title,
      description: meeting.description,
      candidateStartDate: meeting.candidateStartDate,
      candidateEndDate: meeting.candidateEndDate,
      responseDeadline: meeting.responseDeadline,
      linkExpirationDate: meeting.linkExpirationDate || "",
      fixedPlace: policy.fixedPlace || "",
      recommendationRadius: policy.radius || null,
      recommendationKeyword: policy.keyword || "",
      recommendationOptions: policy.options || [],
    }));
    navigate("/meeting/setup/availability");
  };

  if (expired) return <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6"><div className="max-w-md rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">초대 링크가 만료되었습니다.</h1><p className="mt-3 text-slate-500">게스트는 더 이상 이 모임에 접근할 수 없습니다.</p></div></div>;

  return (
    <div className="min-h-screen bg-slate-50 px-6 py-10 text-slate-800"><div className="mx-auto max-w-5xl"><header className="mb-8"><div className="mb-4 flex items-center gap-3"><span className={`rounded-full px-4 py-2 text-sm font-bold ${closed ? "bg-amber-100 text-amber-700" : "bg-blue-100 text-blue-700"}`}>{closed ? "응답 마감" : "투표 진행 중"}</span><span className="text-sm text-slate-400">{isGuest ? "GUEST VIEW" : "HOST VIEW"}</span></div><h1 className="text-4xl font-black text-slate-950">{meeting.title}</h1><p className="mt-3 text-slate-500">{closed ? "응답이 마감되었습니다. 결과 분석 단계로 이동할 수 있어요." : "참여자들의 응답을 기다리고 있어요."}</p></header>
      <div className="mb-6 grid gap-4 sm:grid-cols-2"><div className="rounded-3xl bg-blue-600 p-7 text-white shadow-md"><p className="text-sm font-semibold text-blue-100">현재 응답</p><p className="mt-2 text-4xl font-black">{responseCount}명</p></div><div className="rounded-3xl border border-slate-200 bg-white p-7 shadow-sm"><p className="text-sm font-semibold text-slate-400">응답 마감까지</p><p className="mt-2 text-3xl font-black text-slate-900">{closed ? "응답 마감" : remainingText(responseDeadline, now)}</p><p className="mt-2 text-xs text-slate-400">{Number.isFinite(responseDeadlineTime) ? new Date(responseDeadlineTime).toLocaleString("ko-KR") : "마감일 정보가 없습니다."}</p></div></div>
      {!isGuest && !closed && (
        <section className="mb-6 rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-black text-slate-900">게스트 초대 링크</h2>
                <span className="rounded-full bg-blue-50 px-2.5 py-1 text-[10px] font-bold tracking-wide text-blue-700">INVITE LINK</span>
              </div>
              <p className="mt-1.5 text-sm text-slate-500">링크를 공유하면 게스트가 모임에 참여할 수 있어요.</p>
            </div>
          </div>

          <div className="mt-5 flex flex-col gap-3 sm:flex-row">
            <div className="flex min-w-0 flex-1 items-center rounded-xl bg-slate-50 px-4 py-3.5">
              <code className="block min-w-0 flex-1 truncate text-sm text-slate-600">{`${window.location.origin}${invitePath}`}</code>
            </div>
            <button onClick={copyInviteLink} className="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-6 py-3.5 text-sm font-bold text-white shadow-sm transition hover:bg-blue-700 focus:outline-none focus:ring-4 focus:ring-blue-100">
              링크 복사
            </button>
          </div>

          <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4">
            <span className="text-xs text-slate-400">참여 현황은 이 페이지에서 확인할 수 있습니다.</span>
            <button onClick={() => navigate("/main")} className="inline-flex items-center gap-1.5 rounded-lg px-2 py-2 text-sm font-semibold text-slate-600 transition hover:bg-slate-50 hover:text-blue-700">
              진행 중인 모임 보러가기 <span aria-hidden="true">→</span>
            </button>
          </div>
        </section>
      )}
      {isGuest ? <section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm"><h2 className="text-xl font-black text-slate-900">내 응답</h2>{myResponse ? <><p className="mt-5 text-sm font-bold text-slate-400">가능한 날짜</p><div className="mt-2 flex flex-wrap gap-2">{myResponse.selectedDates.map((date) => <span key={date} className="rounded-xl bg-blue-50 px-3 py-2 text-sm font-bold text-blue-700">{formatSelectedDate(date)}</span>)}</div><p className="mt-5 text-sm font-bold text-slate-400">장소</p><p className="mt-2 font-black text-slate-800">{myResponse.placeMode === "OPEN" ? "내 장소 의견 건너뛰기" : myResponse.placePreference || "저장된 장소 정보 없음"}</p>{!closed && <button onClick={() => { sessionStorage.setItem(`coyoteGuestDraft_${inviteToken}`, JSON.stringify({ ...myResponse, guestId, nickname: myResponse.nickname, inviteToken, candidateStartDate: meeting.candidateStartDate, candidateEndDate: meeting.candidateEndDate, placePolicy: meeting.placePolicy })); navigate(`/invite/${inviteToken}/availability`); }} className="mt-6 rounded-2xl bg-blue-600 px-6 py-3 font-bold text-white">내 응답 수정</button>}</> : <p className="mt-4 text-slate-500">아직 저장된 응답이 없습니다.</p>}</section> : <section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm"><h2 className="text-xl font-black text-slate-900">모임 상태</h2><p className="mt-3 text-slate-500">개별 참여자와 세부 선택은 응답 기간 중 공개하지 않습니다.</p><div className="mt-5 flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-slate-50 p-4"><div><span className="font-bold">참여 등록</span><span className="ml-3 font-black text-blue-600">{participantCount}명</span></div><div className="flex flex-wrap items-center justify-end gap-2">{!closed && hostResponse && <button onClick={editHostResponse} className="rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-bold text-white hover:bg-blue-700">내 응답 수정</button>}{!closed && <button onClick={() => setConfirmClose(true)} className="rounded-xl border border-red-200 bg-white px-5 py-2.5 text-sm font-black text-red-600 shadow-sm transition hover:bg-red-50">모임 마감</button>}</div></div></section>}
      {closed && <button onClick={() => navigate(isGuest ? `/invite/${inviteToken}/result` : `/meeting/${meeting.meetingId}/result`)} className="mt-6 w-full rounded-2xl bg-purple-600 py-4 font-black text-white shadow-md hover:bg-purple-700">분석 및 결과 확인</button>}
      {confirmClose && <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 px-6"><div className="w-full max-w-md rounded-[2rem] bg-white p-8 shadow-2xl"><h2 className="text-2xl font-black text-slate-950">응답을 지금 마감하시겠습니까?</h2><p className="mt-3 leading-6 text-slate-500">마감 후에는 참여자가 응답을 수정할 수 없습니다.</p><div className="mt-7 flex gap-3"><button onClick={() => setConfirmClose(false)} className="flex-1 rounded-2xl border border-slate-200 py-3 font-bold">취소</button><button onClick={closeMeeting} disabled={closing} className="flex-1 rounded-2xl bg-red-600 py-3 font-bold text-white disabled:bg-slate-300">마감하기</button></div></div></div>}
    </div></div>
  );
}

export default MeetingStatusPage;
