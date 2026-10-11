import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import AppHeader from "../../components/AppHeader";
import { fetchMyMeetings } from "../../services/meetingService";
import { getMeetingErrorMessage } from "../../utils/meetingErrors";
import { formatSelectedDate } from "../../utils/dateFormat";

const POLL_MS = 60000;
const pad = (value) => String(value).padStart(2, "0");

// 마감 일시를 "오늘 18:30" / "내일 18:30" / "10월 13일 18:30" 형식으로 보여준다.
const formatDeadline = (isoString) => {
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "마감일 미설정";
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`;
  const startOfDay = (value) => new Date(value.getFullYear(), value.getMonth(), value.getDate()).getTime();
  const diffDays = Math.round((startOfDay(date) - startOfDay(new Date())) / 86400000);
  if (diffDays === 0) return `오늘 ${time}`;
  if (diffDays === 1) return `내일 ${time}`;
  return `${date.getMonth() + 1}월 ${date.getDate()}일 ${time}`;
};

const progressOf = (meeting) => (meeting.participantCount > 0 ? Math.round((meeting.responseCount / meeting.participantCount) * 100) : 0);

function MainPage() {
  const navigate = useNavigate();
  const [activeFilter, setActiveFilter] = useState("all");
  const [meetings, setMeetings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const loadedOnce = useRef(false);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const list = await fetchMyMeetings();
        if (cancelled) return;
        setMeetings(list);
        loadedOnce.current = true;
        setLoadError("");
      } catch (error) {
        if (!cancelled && !loadedOnce.current) setLoadError(getMeetingErrorMessage(error));
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    const id = window.setInterval(load, POLL_MS);
    return () => { cancelled = true; window.clearInterval(id); };
  }, [reloadKey]);

  const reload = () => { setLoading(true); setLoadError(""); setReloadKey((value) => value + 1); };

  const ongoingMeetings = meetings.filter((meeting) => !meeting.closed);
  const completedMeetings = meetings.filter((meeting) => meeting.closed);
  const responseCount = ongoingMeetings.reduce((total, meeting) => total + meeting.responseCount, 0);
  const memberCount = ongoingMeetings.reduce((total, meeting) => total + meeting.participantCount, 0);
  const visibleMeetings = meetings.filter((meeting) => {
    if (activeFilter === "ongoing") return !meeting.closed;
    if (activeFilter === "completed") return meeting.closed;
    return true;
  });

  const filters = [
    { id: "all", label: "전체", count: meetings.length },
    { id: "ongoing", label: "진행 중", count: ongoingMeetings.length },
    { id: "completed", label: "완료", count: completedMeetings.length },
  ];

  return (
    <main className="min-h-screen bg-slate-50 text-slate-900">
      <AppHeader />

      <div className="mx-auto max-w-6xl px-6 pb-20 pt-12 sm:pt-16">
        <section className="mb-8 flex flex-wrap items-end justify-between gap-5">
          <div>
            <p className="mb-3 text-xs font-bold uppercase tracking-[0.14em] text-[#4F7DF3]">Meeting dashboard</p>
            <h1 className="text-3xl font-black tracking-[-0.045em] text-slate-900 sm:text-4xl">모임 현황</h1>
            <p className="mt-3 text-sm leading-6 text-slate-500 sm:text-base">진행 중인 모임의 응답 상황을 한눈에 확인하세요.</p>
          </div>
        </section>

        <section aria-label="모임 요약" className="mb-8 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div className="rounded-3xl border border-slate-200/80 bg-white p-5 shadow-sm">
            <p className="text-sm font-medium text-slate-500">진행 중인 모임</p>
            <p className="mt-2 text-3xl font-black tracking-tight text-slate-900">{ongoingMeetings.length}<span className="ml-1 text-base font-bold text-slate-400">개</span></p>
          </div>
          <div className="rounded-3xl border border-slate-200/80 bg-white p-5 shadow-sm">
            <p className="text-sm font-medium text-slate-500">현재 응답 현황</p>
            <p className="mt-2 text-3xl font-black tracking-tight text-[#4F7DF3]">{responseCount}<span className="mx-1 text-base font-bold text-slate-400">/</span>{memberCount}<span className="ml-1 text-base font-bold text-slate-400">명</span></p>
          </div>
          <div className="rounded-3xl border border-slate-200/80 bg-white p-5 shadow-sm">
            <p className="text-sm font-medium text-slate-500">완료된 모임</p>
            <p className="mt-2 text-3xl font-black tracking-tight text-slate-900">{completedMeetings.length}<span className="ml-1 text-base font-bold text-slate-400">개</span></p>
          </div>
        </section>

        <section className="rounded-[2rem] border border-slate-200/80 bg-white p-5 shadow-sm sm:p-7">
          <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
            <div>
              <h2 className="text-lg font-extrabold tracking-tight text-slate-900">내 모임</h2>
              <p className="mt-1 text-sm text-slate-500">응답률과 진행 상태를 확인할 수 있어요.</p>
            </div>
            <div role="tablist" aria-label="모임 상태 필터" className="inline-flex rounded-xl bg-slate-100 p-1">
              {filters.map((filter) => {
                const selected = activeFilter === filter.id;
                return <button key={filter.id} role="tab" aria-selected={selected} onClick={() => setActiveFilter(filter.id)} className={`rounded-lg px-3.5 py-2 text-sm font-semibold transition sm:px-4 ${selected ? "bg-white text-[#3D6BE5] shadow-sm" : "text-slate-500 hover:text-slate-800"}`}>
                  {filter.label}<span className={`ml-1.5 text-xs ${selected ? "text-[#4F7DF3]" : "text-slate-400"}`}>{filter.count}</span>
                </button>;
              })}
            </div>
          </div>

          {loading ? <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-14 text-center">
            <p className="text-sm font-semibold text-slate-500">불러오는 중...</p>
          </div> : loadError ? <div role="alert" className="rounded-3xl border border-red-100 bg-red-50 px-6 py-14 text-center">
            <p className="text-sm font-semibold text-red-600">{loadError}</p>
            <button type="button" onClick={reload} className="mt-4 rounded-xl border border-red-200 bg-white px-5 py-2.5 text-sm font-bold text-red-600 hover:bg-red-50">다시 불러오기</button>
          </div> : meetings.length === 0 ? <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-14 text-center">
            <p className="text-sm font-semibold text-slate-600">아직 만든 모임이 없어요.</p>
            <p className="mt-1 text-xs text-slate-400">새 모임을 만들어 보세요.</p>
          </div> : visibleMeetings.length > 0 ? <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {visibleMeetings.map((meeting) => {
              const ongoing = !meeting.closed;
              const progress = progressOf(meeting);
              const result = meeting.result;
              return <article key={meeting.meetingId} className="rounded-3xl border border-slate-200 bg-white p-5 transition duration-200 hover:border-blue-200 hover:shadow-md sm:p-6">
                <div className="mb-4 flex items-center justify-between gap-3">
                  <span className={`inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-bold ${ongoing ? "bg-blue-50 text-[#3D6BE5]" : "bg-emerald-50 text-emerald-700"}`}>
                    <span className={`h-1.5 w-1.5 rounded-full ${ongoing ? "bg-[#4F7DF3]" : "bg-emerald-500"}`} />{ongoing ? "진행 중" : "완료"}
                  </span>
                  {ongoing && <span className="text-xs font-medium text-slate-400">마감 {formatDeadline(meeting.responseDeadline)}</span>}
                </div>

                <h3 className="mb-5 text-lg font-extrabold tracking-[-0.03em] text-slate-900">{meeting.title}</h3>

                <div className="mb-2 flex items-end justify-between gap-3">
                  <span className="text-sm font-medium text-slate-500">참여 응답</span>
                  <span className="text-sm font-bold text-slate-800">{meeting.responseCount}<span className="font-medium text-slate-400"> / {meeting.participantCount}명</span></span>
                </div>
                <div role="progressbar" aria-label={`${meeting.title} 응답률`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={progress} className="h-2 overflow-hidden rounded-full bg-slate-100">
                  <div className={`h-full rounded-full transition-all duration-300 ${ongoing ? "bg-[#4F7DF3]" : "bg-emerald-500"}`} style={{ width: `${progress}%` }} />
                </div>
                <div className="mb-5 mt-2 flex items-center justify-between text-xs">
                  <span className="font-semibold text-slate-500">응답 진행률</span>
                  <span className={`font-bold ${ongoing ? "text-[#3D6BE5]" : "text-emerald-700"}`}>{progress}%</span>
                </div>

                {ongoing ? <button type="button" onClick={() => navigate(`/meeting/${meeting.meetingId}/status`)} className="flex w-full items-center justify-center gap-2 rounded-2xl bg-[#4F7DF3] px-4 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#3D6BE5] focus:outline-none focus:ring-4 focus:ring-blue-100">
                  현황 보기<span aria-hidden="true">→</span>
                </button> : <div className="border-t border-slate-100 pt-4">
                  <div className="grid gap-3 sm:grid-cols-2">
                    <div><p className="mb-1 text-xs font-medium text-slate-400">확정 날짜</p><p className="text-sm font-semibold text-slate-700">{result ? formatSelectedDate(result.selectedDate) : "결과 집계 중"}</p></div>
                    <div><p className="mb-1 text-xs font-medium text-slate-400">모임 장소</p><p className="text-sm font-semibold text-slate-700">{result ? result.selectedPlace : "결과 집계 중"}</p></div>
                  </div>
                  <button type="button" onClick={() => navigate(`/meeting/${meeting.meetingId}/result`)} className="mt-4 flex w-full items-center justify-center gap-2 rounded-2xl bg-[#4F7DF3] px-4 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#3D6BE5] focus:outline-none focus:ring-4 focus:ring-blue-100">
                    결과 보기<span aria-hidden="true">→</span>
                  </button>
                </div>}
              </article>;
            })}
          </div> : <div className="rounded-3xl border border-dashed border-slate-300 bg-slate-50 px-6 py-14 text-center">
            <p className="text-sm font-semibold text-slate-600">이 상태의 모임이 아직 없어요.</p>
            <p className="mt-1 text-xs text-slate-400">다른 필터를 선택하거나 새 모임을 만들어 보세요.</p>
          </div>}
        </section>
      </div>
    </main>
  );
}

export default MainPage;
