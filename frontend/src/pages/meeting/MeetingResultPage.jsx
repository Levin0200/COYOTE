import { useEffect, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { FiCalendar, FiCheck, FiMapPin, FiShare2, FiStar, FiUsers } from "react-icons/fi";
import { createMockFinalResult, getFinalResult, getMeetingById, getMeetingByInviteToken, saveFinalResult, updateMeeting } from "../../services/meetingStorage";
import { formatSelectedDate } from "../../utils/dateFormat";

function MeetingResultPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { meetingId, inviteToken } = useParams();
  const isGuest = location.pathname.startsWith("/invite/");
  const [meeting, setMeeting] = useState(() => isGuest ? getMeetingByInviteToken(inviteToken) : getMeetingById(meetingId));
  const [result, setResult] = useState(() => meeting ? getFinalResult(meeting.meetingId) : null);
  const [progress, setProgress] = useState(18);
  const [now, setNow] = useState(0);

  useEffect(() => {
    const refresh = () => setNow(Date.now());
    refresh();
    const id = window.setInterval(refresh, 60000);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    if (!meeting || meeting.status !== "CLOSED") return undefined;
    const id = window.setTimeout(() => setMeeting(updateMeeting(meeting.meetingId, { status: "ANALYZING" })), 150);
    return () => window.clearTimeout(id);
  }, [meeting]);

  useEffect(() => {
    if (!meeting || meeting.status !== "ANALYZING") return undefined;
    const progressId = window.setInterval(() => setProgress((value) => Math.min(value + 16, 92)), 250);
    const resultId = window.setTimeout(() => {
      const created = createMockFinalResult(meeting.meetingId);
      saveFinalResult(created);
      setResult(created);
      setProgress(100);
      setMeeting(updateMeeting(meeting.meetingId, { status: "COMPLETED" }));
    }, 1600);
    return () => { window.clearInterval(progressId); window.clearTimeout(resultId); };
  }, [meeting]);

  if (!meeting) return <div className="flex min-h-screen items-center justify-center bg-slate-50"><div className="rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">모임을 찾을 수 없습니다.</h1></div></div>;

  const completed = meeting.status === "COMPLETED" && result;
  const linkExpired = meeting.linkExpirationDate && now >= new Date(meeting.linkExpirationDate).getTime();
  const copyResultLink = async () => { try { await navigator.clipboard.writeText(window.location.href); alert("결과 링크를 복사했습니다."); } catch { window.prompt("결과 링크를 복사해 주세요.", window.location.href); } };
  const recommendAgain = () => {
    if (isGuest) return;
    if (linkExpired) return alert("링크가 만료되어 더 이상 재추천할 수 없습니다.");
    const previous = { selectedDate: result.selectedDate, selectedPlaces: result.selectedPlaces || [result.selectedPlace], createdAt: result.createdAt };
    const created = createMockFinalResult(meeting.meetingId, (result.generation || 1) + 1);
    const nextResult = { ...created, history: [...(result.history || []), previous] };
    saveFinalResult(nextResult);
    setResult(nextResult);
  };

  return (
    <div className="min-h-screen bg-slate-50 px-6 py-10 text-slate-800"><div className="mx-auto max-w-5xl"><header className="mb-10"><p className="mb-3 flex items-center gap-2 text-sm font-semibold text-purple-600"><FiStar /> AI Meeting Report</p><h1 className="mb-4 text-5xl font-black text-slate-950">{completed ? "최종 결과" : "결과 분석 중"}</h1><p className="text-slate-500">{meeting.title}</p></header>
      {!completed ? <section className="overflow-hidden rounded-[2rem] border border-purple-100 bg-gradient-to-br from-white via-purple-50/50 to-blue-50 p-10 text-center shadow-sm"><div className="mx-auto flex h-20 w-20 animate-pulse items-center justify-center rounded-3xl bg-purple-100 text-4xl">✨</div><h2 className="mt-6 text-3xl font-black text-slate-900">응답이 마감되었습니다</h2><p className="mt-4 leading-7 text-slate-600">참여자 응답을 취합하고 있습니다.<br />최종 날짜와 장소를 분석하고 있어요.</p><div className="mx-auto mt-8 max-w-xl"><div className="mb-2 flex justify-between text-xs font-bold text-purple-600"><span>MOCK ANALYSIS</span><span>{progress}%</span></div><div className="h-3 overflow-hidden rounded-full bg-purple-100"><div className="h-full rounded-full bg-gradient-to-r from-purple-500 to-blue-500 transition-all duration-300" style={{ width: `${progress}%` }} /></div></div></section> : <><section className="overflow-hidden rounded-[2rem] border border-purple-100 bg-gradient-to-br from-white via-purple-50/40 to-blue-50 p-8 shadow-sm"><div className="mb-7 flex items-center justify-between"><div><span className="rounded-full bg-purple-600 px-4 py-2 text-xs font-black text-white">BEST PICK · {result.generation || 1}차</span><h2 className="mt-4 text-3xl font-black text-slate-950">모임 최종 결정</h2></div><span className="flex h-12 w-12 items-center justify-center rounded-full bg-white text-xl text-emerald-500 shadow-sm"><FiCheck /></span></div><div className="grid gap-5 md:grid-cols-2"><ResultCard icon={<FiCalendar />} label="최종 일정" value={result.selectedDate.includes("-") ? formatSelectedDate(result.selectedDate) : result.selectedDate} /><ResultCard icon={<FiMapPin />} label={result.selectedPlaces?.length > 1 ? "공동 1위 장소" : "최종 장소"} value={result.selectedPlace} /></div><div className="mt-5 rounded-2xl border border-white bg-white/80 p-6"><p className="text-sm font-bold text-purple-600">선정 이유 · {result.decisionType}</p><p className="mt-3 leading-7 text-slate-600">{result.reason}</p><p className="mt-4 flex items-center gap-2 text-xs font-semibold text-slate-400"><FiUsers /> 현재는 프론트 Mock 집계 결과입니다.</p></div></section><div className="mt-6 grid gap-3 sm:grid-cols-2"><button onClick={copyResultLink} className="flex items-center justify-center gap-2 rounded-2xl bg-blue-600 py-4 font-black text-white hover:bg-blue-700"><FiShare2 /> 결과 링크 복사</button>{!isGuest && <button onClick={recommendAgain} disabled={linkExpired} className="rounded-2xl bg-purple-600 py-4 font-black text-white hover:bg-purple-700 disabled:cursor-not-allowed disabled:bg-slate-300">{linkExpired ? "링크 만료 · 재추천 불가" : "이전 응답으로 다시 추천"}</button>}</div>{!isGuest && result.history?.length > 0 && <p className="mt-3 text-center text-xs font-semibold text-slate-400">이전 추천 결과 {result.history.length}건을 보관 중입니다.</p>}</>}
      <button onClick={() => navigate(isGuest ? `/invite/${inviteToken}/status` : `/meeting/${meeting.meetingId}/status`)} className="mt-4 w-full rounded-2xl border border-slate-200 bg-white py-4 font-bold hover:bg-slate-100">진행 상태로 돌아가기</button>
    </div></div>
  );
}

function ResultCard({ icon, label, value }) { return <div className="rounded-3xl bg-white p-6 shadow-sm"><span className="flex h-11 w-11 items-center justify-center rounded-xl bg-purple-50 text-xl text-purple-600">{icon}</span><p className="mt-5 text-sm font-bold text-slate-400">{label}</p><p className="mt-2 text-2xl font-black text-slate-900">{value}</p></div>; }

export default MeetingResultPage;
