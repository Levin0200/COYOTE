import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { FiCalendar, FiCheck, FiMapPin, FiShare2, FiStar, FiUsers } from "react-icons/fi";
import { formatSelectedDate } from "../../utils/dateFormat";
import { getErrorCode } from "../../services/api";
import { getMeetingErrorMessage } from "../../utils/meetingErrors";
import { fetchGuestResult, fetchHostMeeting, fetchHostResult, fetchInviteInfo } from "../../services/meetingService";

function MeetingResultPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { meetingId, inviteToken } = useParams();
  const isGuest = location.pathname.startsWith("/invite/");
  const seenId = isGuest ? inviteToken : meetingId;
  const [meeting, setMeeting] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [progress, setProgress] = useState(18);
  const [revealed, setRevealed] = useState(false);
  const [now, setNow] = useState(0);

  useEffect(() => {
    const refresh = () => setNow(Date.now());
    refresh();
    const id = window.setInterval(refresh, 60000);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const [info, data] = await Promise.all(isGuest
          ? [fetchInviteInfo(inviteToken), fetchGuestResult(inviteToken)]
          : [fetchHostMeeting(meetingId), fetchHostResult(meetingId)]);
        if (cancelled) return;
        setMeeting(info);
        setResult(data);
        setLoadError(null);
      } catch (error) {
        if (!cancelled) setLoadError({ code: getErrorCode(error), message: getMeetingErrorMessage(error) });
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [isGuest, inviteToken, meetingId, reloadKey]);

  // 서버는 마감 즉시 결과를 만들어 두므로 분석 진행바는 화면 연출이다. 이 브라우저에서 해당 모임의 결과를 처음 열 때만 재생한다.
  useEffect(() => {
    if (!result) return undefined;
    let seen = false;
    try { seen = localStorage.getItem(`coyoteResultSeen_${seenId}`) === "1"; } catch { /* 저장소를 쓸 수 없으면 매번 재생한다 */ }
    const progressId = seen ? null : window.setInterval(() => setProgress((value) => Math.min(value + 16, 92)), 250);
    const revealId = window.setTimeout(() => {
      setProgress(100);
      setRevealed(true);
      try { localStorage.setItem(`coyoteResultSeen_${seenId}`, "1"); } catch { /* 저장소를 쓸 수 없으면 기록하지 않는다 */ }
    }, seen ? 0 : 1600);
    return () => { if (progressId) window.clearInterval(progressId); window.clearTimeout(revealId); };
  }, [result, seenId]);

  const backToStatus = () => navigate(isGuest ? `/invite/${inviteToken}/status` : `/meeting/${meetingId}/status`);
  const reload = () => { setLoading(true); setLoadError(null); setReloadKey((value) => value + 1); };

  if (loading) return <div className="flex min-h-screen items-center justify-center bg-slate-50"><p className="font-bold text-slate-400">불러오는 중...</p></div>;
  if (loadError?.code === "INVITE_EXPIRED") return <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6"><div className="max-w-md rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">초대 링크가 만료되었습니다.</h1><p className="mt-3 text-slate-500">게스트는 더 이상 이 모임에 접근할 수 없습니다.</p></div></div>;
  if (loadError?.code === "MEETING_NOT_CLOSED" || loadError?.code === "RESULT_NOT_READY") return <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6"><div className="max-w-md rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">{loadError.code === "MEETING_NOT_CLOSED" ? "아직 결과가 없습니다." : "결과를 준비하고 있습니다."}</h1><p className="mt-3 text-slate-500">{loadError.message}</p><div className="mt-5 flex justify-center gap-3">{loadError.code === "RESULT_NOT_READY" && <button onClick={reload} className="rounded-xl border border-slate-200 bg-white px-5 py-3 font-bold hover:bg-slate-100">다시 불러오기</button>}<button onClick={backToStatus} className="rounded-xl bg-blue-600 px-5 py-3 font-bold text-white">진행 상태로 돌아가기</button></div></div></div>;
  if (!meeting || !result) return <div className="flex min-h-screen items-center justify-center bg-slate-50"><div className="rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">모임을 찾을 수 없습니다.</h1>{loadError && loadError.code !== "MEETING_NOT_FOUND" && <p className="mt-3 text-slate-500">{loadError.message}</p>}</div></div>;

  const completed = revealed;
  const linkExpired = Boolean(meeting.linkExpirationDate && now >= new Date(meeting.linkExpirationDate).getTime());
  // 호스트는 로그인이 필요한 호스트 전용 주소 대신 게스트가 열 수 있는 결과 주소를 복사한다.
  const copyResultLink = async () => { const url = isGuest ? window.location.href : `${window.location.origin}/invite/${meeting.inviteToken}/result`; try { await navigator.clipboard.writeText(url); alert("결과 링크를 복사했습니다."); } catch { window.prompt("결과 링크를 복사해 주세요.", url); } };
  const recommendAgain = () => {
    if (isGuest) return;
    if (linkExpired) return alert("링크가 만료되어 더 이상 재추천할 수 없습니다.");
    alert("다시 추천 기능은 준비 중입니다.");
  };

  return (
    <div className="min-h-screen bg-slate-50 px-6 py-10 text-slate-800"><div className="mx-auto max-w-5xl"><header className="mb-10"><p className="mb-3 flex items-center gap-2 text-sm font-semibold text-purple-600"><FiStar /> AI Meeting Report</p><h1 className="mb-4 text-5xl font-black text-slate-950">{completed ? "최종 결과" : "결과 분석 중"}</h1><p className="text-slate-500">{meeting.title}</p></header>
      {!completed ? <section className="overflow-hidden rounded-[2rem] border border-purple-100 bg-gradient-to-br from-white via-purple-50/50 to-blue-50 p-10 text-center shadow-sm"><div className="mx-auto flex h-20 w-20 animate-pulse items-center justify-center rounded-3xl bg-purple-100 text-4xl">✨</div><h2 className="mt-6 text-3xl font-black text-slate-900">응답이 마감되었습니다</h2><p className="mt-4 leading-7 text-slate-600">참여자 응답을 취합하고 있습니다.<br />최종 날짜와 장소를 분석하고 있어요.</p><div className="mx-auto mt-8 max-w-xl"><div className="mb-2 flex justify-between text-xs font-bold text-purple-600"><span>분석 중</span><span>{progress}%</span></div><div className="h-3 overflow-hidden rounded-full bg-purple-100"><div className="h-full rounded-full bg-gradient-to-r from-purple-500 to-blue-500 transition-all duration-300" style={{ width: `${progress}%` }} /></div></div></section> : <><section className="overflow-hidden rounded-[2rem] border border-purple-100 bg-gradient-to-br from-white via-purple-50/40 to-blue-50 p-8 shadow-sm"><div className="mb-7 flex items-center justify-between"><div><span className="rounded-full bg-purple-600 px-4 py-2 text-xs font-black text-white">BEST PICK · {result.generation || 1}차</span><h2 className="mt-4 text-3xl font-black text-slate-950">모임 최종 결정</h2></div><span className="flex h-12 w-12 items-center justify-center rounded-full bg-white text-xl text-emerald-500 shadow-sm"><FiCheck /></span></div><div className="grid gap-5 md:grid-cols-2"><ResultCard icon={<FiCalendar />} label="최종 일정" value={result.selectedDate.includes("-") ? formatSelectedDate(result.selectedDate) : result.selectedDate} /><ResultCard icon={<FiMapPin />} label={result.selectedPlaces?.length > 1 ? "공동 1위 장소" : "최종 장소"} value={result.selectedPlace} /></div><div className="mt-5 rounded-2xl border border-white bg-white/80 p-6"><p className="text-sm font-bold text-purple-600">선정 이유 · {result.decisionType}</p><p className="mt-3 leading-7 text-slate-600">{result.reason}</p><p className="mt-4 flex items-center gap-2 text-xs font-semibold text-slate-400"><FiUsers /> 총 {result.totalResponses}명 응답 · 날짜 {result.dateVoteCount}표 · 장소 {result.placeVoteCount}표</p></div></section><div className="mt-6 grid gap-3 sm:grid-cols-2"><button onClick={copyResultLink} className="flex items-center justify-center gap-2 rounded-2xl bg-blue-600 py-4 font-black text-white hover:bg-blue-700"><FiShare2 /> 결과 링크 복사</button>{!isGuest && <button onClick={recommendAgain} disabled={linkExpired} className="rounded-2xl bg-purple-600 py-4 font-black text-white hover:bg-purple-700 disabled:cursor-not-allowed disabled:bg-slate-300">{linkExpired ? "링크 만료 · 재추천 불가" : "이전 응답으로 다시 추천"}</button>}</div></>}
      <button onClick={backToStatus} className="mt-4 w-full rounded-2xl border border-slate-200 bg-white py-4 font-bold hover:bg-slate-100">진행 상태로 돌아가기</button>
    </div></div>
  );
}

function ResultCard({ icon, label, value }) { return <div className="rounded-3xl bg-white p-6 shadow-sm"><span className="flex h-11 w-11 items-center justify-center rounded-xl bg-purple-50 text-xl text-purple-600">{icon}</span><p className="mt-5 text-sm font-bold text-slate-400">{label}</p><p className="mt-2 text-2xl font-black text-slate-900">{value}</p></div>; }

export default MeetingResultPage;
