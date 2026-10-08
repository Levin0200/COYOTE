import { useState } from "react";
import AppHeader from "../../components/AppHeader";

const meetings = [
  { id: 1, title: "개발팀 주말 정기 회식", status: "진행 중", date: "가능한 날짜 투표 중", location: "장소 응답 대기 중", members: 5, responses: 4, progress: 80, deadline: "오늘까지" },
  { id: 2, title: "대학 동창 신년회 모임", status: "완료", date: "2026년 6월 6일 (토) 18:00", location: "홍대 스페이스 룸", members: 8, responses: 8, progress: 100 },
  { id: 3, title: "모여ON 네트워킹 데이", status: "진행 중", date: "가능한 날짜 투표 중", location: "장소 응답 대기 중", members: 12, responses: 5, progress: 42, deadline: "내일까지" },
  { id: 4, title: "금요 러닝 크루 번개", status: "완료", date: "2026년 6월 12일 (금) 20:00", location: "여의도 한강공원", members: 6, responses: 6, progress: 100 },
];

function MainPage() {
  const [activeFilter, setActiveFilter] = useState("all");
  const [remindedMeetings, setRemindedMeetings] = useState([]);
  const [notice, setNotice] = useState("");

  const ongoingMeetings = meetings.filter((meeting) => meeting.status === "진행 중");
  const completedMeetings = meetings.filter((meeting) => meeting.status === "완료");
  const responseCount = ongoingMeetings.reduce((total, meeting) => total + meeting.responses, 0);
  const memberCount = ongoingMeetings.reduce((total, meeting) => total + meeting.members, 0);
  const visibleMeetings = meetings.filter((meeting) => {
    if (activeFilter === "ongoing") return meeting.status === "진행 중";
    if (activeFilter === "completed") return meeting.status === "완료";
    return true;
  });

  const filters = [
    { id: "all", label: "전체", count: meetings.length },
    { id: "ongoing", label: "진행 중", count: ongoingMeetings.length },
    { id: "completed", label: "완료", count: completedMeetings.length },
  ];

  const sendReminder = (meeting) => {
    if (remindedMeetings.includes(meeting.id)) return;
    setRemindedMeetings((current) => [...current, meeting.id]);
    setNotice(`‘${meeting.title}’ 리마인더 요청을 기록했어요. 실제 알림은 백엔드 연결 후 전송됩니다.`);
    window.setTimeout(() => setNotice(""), 3500);
  };

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

          {notice && <div role="status" className="mb-5 flex items-start gap-3 rounded-2xl border border-blue-100 bg-blue-50 px-4 py-3.5 text-sm leading-6 text-blue-800"><span aria-hidden="true">✓</span><span>{notice}</span></div>}

          {visibleMeetings.length > 0 ? <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {visibleMeetings.map((meeting) => {
              const ongoing = meeting.status === "진행 중";
              const reminded = remindedMeetings.includes(meeting.id);
              return <article key={meeting.id} className="rounded-3xl border border-slate-200 bg-white p-5 transition duration-200 hover:border-blue-200 hover:shadow-md sm:p-6">
                <div className="mb-4 flex items-center justify-between gap-3">
                  <span className={`inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-bold ${ongoing ? "bg-blue-50 text-[#3D6BE5]" : "bg-emerald-50 text-emerald-700"}`}>
                    <span className={`h-1.5 w-1.5 rounded-full ${ongoing ? "bg-[#4F7DF3]" : "bg-emerald-500"}`} />{meeting.status}
                  </span>
                  {ongoing && <span className="text-xs font-medium text-slate-400">마감 {meeting.deadline}</span>}
                </div>

                <h3 className="mb-5 text-lg font-extrabold tracking-[-0.03em] text-slate-900">{meeting.title}</h3>

                <div className="mb-2 flex items-end justify-between gap-3">
                  <span className="text-sm font-medium text-slate-500">참여 응답</span>
                  <span className="text-sm font-bold text-slate-800">{meeting.responses}<span className="font-medium text-slate-400"> / {meeting.members}명</span></span>
                </div>
                <div role="progressbar" aria-label={`${meeting.title} 응답률`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={meeting.progress} className="h-2 overflow-hidden rounded-full bg-slate-100">
                  <div className={`h-full rounded-full transition-all duration-300 ${ongoing ? "bg-[#4F7DF3]" : "bg-emerald-500"}`} style={{ width: `${meeting.progress}%` }} />
                </div>
                <div className="mb-5 mt-2 flex items-center justify-between text-xs">
                  <span className="font-semibold text-slate-500">응답 진행률</span>
                  <span className={`font-bold ${ongoing ? "text-[#3D6BE5]" : "text-emerald-700"}`}>{meeting.progress}%</span>
                </div>

                {ongoing ? <button type="button" onClick={() => sendReminder(meeting)} disabled={reminded} className={`flex w-full items-center justify-center gap-2 rounded-2xl px-4 py-3 text-sm font-bold transition focus:outline-none focus:ring-4 focus:ring-blue-100 ${reminded ? "cursor-default border border-slate-200 bg-slate-50 text-slate-400" : "bg-[#4F7DF3] text-white shadow-sm hover:bg-[#3D6BE5]"}`}>
                  <span aria-hidden="true">{reminded ? "✓" : "↗"}</span>{reminded ? "리마인더 요청됨" : "미응답자에게 알림 보내기"}
                </button> : <div className="grid gap-3 border-t border-slate-100 pt-4 sm:grid-cols-2">
                  <div><p className="mb-1 text-xs font-medium text-slate-400">확정 날짜</p><p className="text-sm font-semibold text-slate-700">{meeting.date}</p></div>
                  <div><p className="mb-1 text-xs font-medium text-slate-400">모임 장소</p><p className="text-sm font-semibold text-slate-700">{meeting.location}</p></div>
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
