import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

const demoScreens = [
  { name: "모임 생성", src: "/images/landing/step-01-create.png", targetX: "35%", targetY: "86%" },
  { name: "캘린더 날짜 선택", src: "/images/landing/step-03-dates.png", targetX: "50%", targetY: "92%" },
  { name: "장소 선택", src: "/images/landing/step-04-place.png", targetX: "42%", targetY: "44%" },
  { name: "게스트 초대 링크", src: "/images/landing/step-05-status.png", targetX: "75%", targetY: "54%" },
  { name: "게스트 참여", src: "/images/landing/step-02-guest.png", targetX: "50%", targetY: "74%" },
];

const productFeatures = [
  { title: "모임 링크 초대", desc: "링크를 공유해 친구들을 모임에 초대해요.", type: "invite" },
  { title: "AI 일정 추천", desc: "가능한 날짜를 모아 모두에게 맞는 날을 골라요.", type: "schedule" },
  { title: "지도에서 장소 확인", desc: "약속 장소 위치를 지도로 바로 확인해요.", type: "map" },
  { title: "참석 현황 확인", desc: "누가 함께하는지 한눈에 확인해요.", type: "attendees" },
  { title: "모임 기록과 후기", desc: "다녀온 장소와 후기를 모임별로 남겨요.", type: "feedback" },
  { title: "다음 모임 준비", desc: "지난 기록을 참고해 다음 약속을 준비해요.", type: "memory" },
];

function FeaturePreview({ type }) {
  if (type === "invite") return <div className="flex w-full max-w-md items-center gap-2 rounded-xl border border-slate-200 bg-white p-2 shadow-sm"><span className="min-w-0 flex-1 truncate px-2 text-xs text-slate-500">초대 링크가 생성됐어요</span><span className="rounded-lg bg-[#4169e1] px-3 py-2 text-[10px] font-bold text-white">링크 복사</span></div>;
  if (type === "schedule") return <div className="w-full max-w-md rounded-xl border border-slate-200 bg-white p-4 shadow-sm"><div className="mb-3 flex items-center justify-between"><span className="text-sm font-bold text-slate-700">가능한 날짜</span><span className="rounded-full bg-blue-50 px-2.5 py-1 text-[10px] font-bold text-[#4169e1]">AI 추천 ✦</span></div><div className="grid grid-cols-5 gap-2">{["금 9", "토 10", "일 11", "금 16", "토 17"].map((day, index) => <span key={day} className={`rounded-lg py-2.5 text-center text-xs font-semibold ${index === 1 ? "bg-[#4169e1] text-white" : "bg-slate-50 text-slate-500"}`}>{day}</span>)}</div></div>;
  if (type === "map") return <div className="relative h-32 w-full max-w-md overflow-hidden rounded-xl border border-slate-200 bg-[#eef3f8]" style={{ backgroundImage: "linear-gradient(35deg, transparent 46%, white 47%, white 51%, transparent 52%), linear-gradient(145deg, transparent 40%, #d5e5db 41%, #d5e5db 57%, transparent 58%), linear-gradient(90deg, rgba(148,163,184,.12) 1px, transparent 1px), linear-gradient(rgba(148,163,184,.12) 1px, transparent 1px)", backgroundSize: "100% 100%, 100% 100%, 24px 24px, 24px 24px" }}><span className="absolute left-1/2 top-1/2 grid h-10 w-10 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full border-4 border-white bg-[#4169e1] text-xl text-white shadow-lg">⌖</span><span className="absolute bottom-2 left-3 rounded-md bg-white/90 px-2.5 py-1.5 text-[10px] font-bold text-slate-600 shadow-sm">성수동 · 모임 장소</span></div>;
  if (type === "attendees") return <div className="flex w-full max-w-md items-center justify-between rounded-xl border border-slate-200 bg-white p-5 shadow-sm"><div className="flex -space-x-2"><span className="grid h-10 w-10 place-items-center rounded-full border-2 border-white bg-amber-100 text-sm">민</span><span className="grid h-10 w-10 place-items-center rounded-full border-2 border-white bg-rose-100 text-sm">수</span><span className="grid h-10 w-10 place-items-center rounded-full border-2 border-white bg-cyan-100 text-sm">현</span><span className="grid h-10 w-10 place-items-center rounded-full border-2 border-white bg-violet-100 text-xs font-bold">+2</span></div><span className="rounded-full bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-700">4명 참석 확정</span></div>;
  if (type === "feedback") return <div className="w-full max-w-md rounded-xl border border-slate-200 bg-white p-5 shadow-sm"><div className="flex items-center justify-between"><span className="text-sm font-bold text-slate-700">지난 모임 후기</span><span className="text-base tracking-[0.12em] text-amber-400">★★★★★</span></div><div className="mt-4 flex gap-2"><span className="rounded-full bg-slate-50 px-3 py-2 text-xs text-slate-500">분위기 좋아요</span><span className="rounded-full bg-slate-50 px-3 py-2 text-xs text-slate-500">재방문 추천</span></div></div>;
  return <div className="w-full max-w-md rounded-xl border border-slate-200 bg-white p-4 shadow-sm"><span className="text-xs font-bold text-[#4169e1]">우리 모임 취향</span><div className="mt-3 flex flex-wrap gap-2"><span className="rounded-full bg-blue-50 px-2 py-1.5 text-[11px] font-semibold text-blue-700">조용한 카페</span><span className="rounded-full bg-violet-50 px-2 py-1.5 text-[11px] font-semibold text-violet-700">대중교통 편리</span><span className="rounded-full bg-amber-50 px-2 py-1.5 text-[11px] font-semibold text-amber-700">새로운 장소</span></div></div>;
}

function StoryPreview({ type }) {
  if (type === "invite") {
    return (
      <div className="flex h-full flex-col justify-between rounded-xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
        <div>
          <div className="flex items-center justify-between gap-3">
            <span className="text-[10px] font-bold tracking-[0.12em] text-blue-600">NEW MEETING</span>
            <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-[10px] font-bold text-emerald-700">모임 생성 완료</span>
          </div>
          <h3 className="mt-2 text-base font-extrabold text-slate-900 sm:text-lg">캡스톤 프로젝트 회식</h3>
          <p className="mt-1 text-xs text-slate-500">모임 링크를 공유하고 함께 날짜를 정해보세요.</p>
        </div>
        <div className="mt-4 rounded-xl bg-slate-50 p-3 sm:p-4">
          <p className="text-[11px] font-bold text-slate-600">게스트 초대 링크</p>
          <div className="mt-2 flex min-w-0 items-center gap-2 rounded-lg border border-slate-200 bg-white p-2">
            <span className="min-w-0 flex-1 truncate px-1 text-[10px] text-slate-500 sm:text-xs">moyeo.on/invite/capstone-dinner</span>
            <span className="shrink-0 rounded-md bg-blue-600 px-2.5 py-1.5 text-[10px] font-bold text-white">링크 복사</span>
          </div>
          <div className="mt-3 flex items-center justify-between text-[10px] text-slate-400"><span>참여 응답</span><span className="font-semibold text-slate-600">4명 초대 · 2명 응답</span></div>
          <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-slate-200"><div className="h-full w-1/2 rounded-full bg-blue-500" /></div>
        </div>
      </div>
    );
  }

  if (type === "schedule") {
    const dates = ["", "", "", "", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31"];
    return (
      <div className="grid h-full grid-cols-1 gap-3 sm:grid-cols-[1.2fr_.8fr] sm:gap-4">
        <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-sm sm:p-4">
          <div className="mb-3 flex items-center justify-between"><span className="text-xs font-bold text-slate-800 sm:text-sm">2026년 10월</span><span className="text-[10px] font-semibold text-slate-400">가능한 날짜를 선택하세요</span></div>
          <div className="grid grid-cols-7 gap-1 text-center text-[9px] font-semibold text-slate-400 sm:gap-1.5 sm:text-[10px]">
            {["일", "월", "화", "수", "목", "금", "토"].map((day) => <span key={day} className="py-1">{day}</span>)}
            {dates.map((day, index) => <span key={index} className={`grid aspect-square place-items-center rounded-md ${[15, 17, 22].includes(index) ? "bg-blue-600 font-bold text-white shadow-sm" : day ? "bg-slate-50 text-slate-600" : "text-transparent"}`}>{day || "·"}</span>)}
          </div>
        </div>
        <div className="flex flex-col justify-between rounded-xl border border-blue-100 bg-blue-50 p-3 sm:p-4">
          <div><span className="rounded-full bg-white px-2 py-1 text-[9px] font-bold text-blue-700">AI 추천</span><p className="mt-3 text-[10px] font-semibold text-slate-500">가장 많은 사람이 가능한 날</p><p className="mt-1 text-base font-black text-slate-900 sm:text-lg">10월 12일 <span className="text-xs font-semibold text-slate-500">월요일</span></p></div>
          <div className="mt-3 border-t border-blue-100 pt-3"><div className="flex -space-x-1.5"><span className="grid h-6 w-6 place-items-center rounded-full border-2 border-blue-50 bg-amber-200 text-[9px] font-bold text-amber-800">민</span><span className="grid h-6 w-6 place-items-center rounded-full border-2 border-blue-50 bg-rose-200 text-[9px] font-bold text-rose-800">수</span><span className="grid h-6 w-6 place-items-center rounded-full border-2 border-blue-50 bg-cyan-200 text-[9px] font-bold text-cyan-800">현</span><span className="grid h-6 w-6 place-items-center rounded-full border-2 border-blue-50 bg-violet-200 text-[9px] font-bold text-violet-800">준</span></div><p className="mt-2 text-[10px] font-semibold text-blue-800">4명 모두 가능한 일정</p></div>
        </div>
      </div>
    );
  }

  if (type === "map") {
    return (
      <div className="grid h-full grid-cols-1 gap-3 sm:grid-cols-[.9fr_1.1fr] sm:gap-4">
        <div className="flex flex-col rounded-xl border border-slate-200 bg-white p-3 shadow-sm sm:p-4">
          <div className="flex items-center justify-between"><h3 className="text-xs font-extrabold text-slate-900 sm:text-sm">추천 장소</h3><span className="rounded-full bg-blue-50 px-2 py-1 text-[9px] font-bold text-blue-700">가까운 순</span></div>
          <p className="mt-1 text-[10px] text-slate-500">인천 송림동 주변</p>
          <div className="mt-3 space-y-2">
            {[['센트럴파크 뷰 카페', '0.8km', '분위기 좋은 카페'], ['트리플스트리트 라운지', '1.2km', '대화하기 좋은 장소']].map(([name, distance, note], index) => <div key={name} className={`rounded-lg border p-2.5 ${index === 0 ? "border-blue-200 bg-blue-50/70" : "border-slate-100 bg-white"}`}><div className="flex items-center justify-between gap-2"><span className="truncate text-[10px] font-bold text-slate-800 sm:text-xs">{name}</span><span className="shrink-0 text-[9px] font-semibold text-slate-400">{distance}</span></div><p className="mt-1 text-[9px] text-slate-500">{note}</p></div>)}
          </div>
        </div>
        <div className="relative min-h-[150px] overflow-hidden rounded-xl border border-slate-200 bg-[#edf3ed] shadow-sm" style={{ backgroundImage: "linear-gradient(25deg, transparent 42%, #fff 43%, #fff 47%, transparent 48%), linear-gradient(155deg, transparent 28%, #fff 29%, #fff 34%, transparent 35%), linear-gradient(95deg, transparent 62%, #fff 63%, #fff 68%, transparent 69%), linear-gradient(135deg, #e9f0e7 0%, #f5f3e8 48%, #e5eef0 100%)" }}>
          <span className="absolute left-[28%] top-[25%] h-8 w-8 rounded-full bg-emerald-200/70" /><span className="absolute bottom-[12%] right-[12%] h-12 w-12 rounded-full bg-emerald-200/70" />
          <span className="absolute left-[58%] top-[43%] grid h-9 w-9 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full border-[3px] border-white bg-blue-600 text-sm text-white shadow-lg">●</span>
          <span className="absolute left-[58%] top-[20%] -translate-x-1/2 rounded-lg bg-white px-2.5 py-1.5 text-[9px] font-bold text-slate-700 shadow-md">센트럴파크 뷰 카페</span>
          <span className="absolute bottom-2 left-2 rounded-md bg-white/90 px-2 py-1 text-[9px] font-semibold text-slate-500">지도에서 위치 확인</span>
        </div>
      </div>
    );
  }

  return (
    <div className="grid h-full grid-cols-1 gap-3 sm:grid-cols-[1fr_.9fr] sm:gap-4">
      <div className="flex flex-col justify-between rounded-xl border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
        <div><div className="flex items-center justify-between gap-2"><span className="text-[10px] font-bold tracking-wide text-slate-400">지난 모임 기록</span><span className="text-xs tracking-wider text-amber-400">★★★★★</span></div><h3 className="mt-2 text-sm font-extrabold text-slate-900 sm:text-base">성수에서 만난 토요일</h3><p className="mt-1 text-[10px] text-slate-500">2026년 9월 19일 · 5명 참석</p></div>
        <div className="mt-3 rounded-lg bg-slate-50 p-3"><p className="text-[10px] leading-5 text-slate-600">“조용해서 대화하기 좋았어요. 다음에도 여기 오고 싶어요.”</p></div>
      </div>
      <div className="flex flex-col justify-between rounded-xl border border-blue-100 bg-blue-50/70 p-4 sm:p-5">
        <div><span className="text-[10px] font-bold tracking-wide text-blue-700">우리 모임 취향</span><p className="mt-2 text-sm font-extrabold text-slate-900 sm:text-base">다음 모임도 더 잘 맞게</p><p className="mt-1 text-[10px] text-slate-500">지난 기록에서 발견한 선호예요.</p></div>
        <div className="mt-3 flex flex-wrap gap-1.5"><span className="rounded-full bg-white px-2.5 py-1.5 text-[9px] font-semibold text-slate-600">조용한 장소</span><span className="rounded-full bg-white px-2.5 py-1.5 text-[9px] font-semibold text-slate-600">대중교통 편리</span><span className="rounded-full bg-white px-2.5 py-1.5 text-[9px] font-semibold text-slate-600">재방문 선호</span></div>
      </div>
    </div>
  );
}

const problems = [
  ["01", "단톡방에서 끝없는 일정 조율", "누가 언제 가능한지 계속 물어보느라 약속 잡기가 어려워요."],
  ["02", "장소 검색과 비교의 번거로움", "취향과 위치를 고려해 모두가 만족할 장소를 찾기 힘들어요."],
  ["03", "참석 여부 확인의 어려움", "참석자와 변경 사항을 매번 따로 정리해야 해요."],
  ["04", "모임 뒤에 남는 기록이 없어요", "좋았던 장소와 경험이 다음 모임으로 이어지지 않아요."],
];

const stories = [
  {
    label: "모임 링크로 간편하게 공유하기",
    eyebrow: "01 · SHARE & GATHER",
    title: "링크 하나로\n모임을 시작해요",
    desc: "모임을 만든 뒤 초대 링크만 공유하세요. 참여자는 링크를 통해 모임에 들어와 일정과 장소를 함께 정할 수 있어요.",
    sub: "초대와 안내를 한 번에, 모임 준비는 간편하게.",
    preview: "invite",
    points: ["간편한 모임 생성", "초대 링크 공유", "참여 현황 한눈에 확인"],
  },
  {
    label: "AI로 최적의 날짜 추천받기",
    eyebrow: "02 · AI SCHEDULE",
    title: "가능한 날짜를 모아\nAI가 최종 일정을 골라요",
    desc: "캘린더에서 각자 가능한 날짜를 선택하면, 모여ON이 응답을 모아 여러 후보를 비교하고 가장 적합한 날짜를 추천해요.",
    sub: "흩어진 일정을 한곳에 모아 최적의 날짜를 찾아요.",
    preview: "schedule",
    points: ["캘린더에서 가능 날짜 선택", "후보 일정 자동 정리", "AI의 최종 날짜 추천"],
  },
  {
    label: "장소를 지도에서 확인하기",
    eyebrow: "03 · PLACE ON MAP",
    title: "정해진 장소를\n지도에서 바로 확인해요",
    desc: "모임 장소가 정해지면 지도 미리보기와 위치 정보를 함께 제공해요. 참여자 모두가 장소와 주변 위치를 쉽게 확인할 수 있어요.",
    sub: "장소 이름만 공유하지 않고, 위치까지 한눈에.",
    preview: "map",
    points: ["선택한 장소 정보 확인", "지도에서 위치 미리보기", "참여자와 장소 정보 공유"],
  },
  {
    label: "모임을 기록으로 남기기",
    eyebrow: "04 · BETTER EVERY TIME",
    title: "경험이 쌓일수록\n모임은 더 좋아져요",
    desc: "장소와 피드백을 모임 기록으로 남기면, 다음에는 우리에게 더 잘 맞는 모임을 만들 수 있어요.",
    sub: "모임의 순간이 다음 만남의 힌트가 됩니다.",
    preview: "history",
    points: ["모임 기록과 피드백", "취향 데이터 축적", "더 나은 추천 경험"],
  },
];

function LandingPage() {
  const navigate = useNavigate();
  const [activeStory, setActiveStory] = useState(0);
  const [activeDemo, setActiveDemo] = useState(0);
  const selectedStory = stories[activeStory];
  const selectedDemo = demoScreens[activeDemo];

  useEffect(() => {
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) return undefined;
    const timer = window.setInterval(() => {
      setActiveDemo((current) => (current + 1) % demoScreens.length);
    }, 5000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) return undefined;
    const timer = window.setTimeout(() => {
      setActiveStory((current) => (current + 1) % stories.length);
    }, 5000);
    return () => window.clearTimeout(timer);
  }, [activeStory]);

  return (
    <main className="overflow-hidden bg-white text-slate-950">
      <header className="absolute inset-x-0 top-0 z-20">
        <nav className="mx-auto flex max-w-7xl items-center justify-between px-6 py-5 lg:px-10">
          <a href="#home" className="flex items-center gap-2 text-xl font-black tracking-tight">
            <span className="grid h-9 w-9 place-items-center rounded-xl bg-[#4169e1] text-white">M</span>
            모여ON
          </a>
          <div className="hidden items-center gap-9 text-sm font-medium text-slate-600 md:flex">
            <a className="transition hover:text-slate-950" href="#features">서비스</a>
            <a className="transition hover:text-slate-950" href="#how-it-works">이용 방법</a>
            <a className="transition hover:text-slate-950" href="#about">모여ON 이야기</a>
          </div>
          <button onClick={() => navigate("/login")} className="rounded-full bg-slate-950 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-[#4169e1]">
            시작하기 <span aria-hidden="true">→</span>
          </button>
        </nav>
      </header>

      <section id="home" className="relative isolate flex min-h-[780px] flex-col items-center justify-center px-6 pb-16 pt-28 text-center lg:min-h-[850px]">
        <div className="absolute inset-x-0 top-0 -z-10 h-[690px] bg-[radial-gradient(ellipse_at_50%_10%,#e9efff_0%,#f7f9ff_43%,white_78%)]" />
        <div className="mb-7 inline-flex items-center gap-2 rounded-full border border-blue-100 bg-white/80 px-4 py-2 text-xs font-semibold text-[#4169e1] shadow-sm">
          <span className="h-2 w-2 rounded-full bg-[#4169e1]" /> AI 기반 모임 매니저, 모여ON
        </div>
        <h1 className="max-w-4xl text-5xl font-bold leading-[1.12] tracking-[-0.055em] sm:text-6xl lg:text-7xl">
          약속을 잡는 순간부터<br />
          <span className="text-[#4169e1]">함께하는 즐거움</span>까지
        </h1>
        <p className="mt-7 max-w-xl text-base leading-7 text-slate-600 sm:text-lg sm:leading-8">
          일정 조율부터 장소 추천까지, 모여ON이 도와드려요.<br className="hidden sm:block" />
          이제 약속을 잡는 일도 함께하는 즐거움이 됩니다.
        </p>
        <div className="mt-8 flex flex-col items-center gap-3 sm:flex-row">
          <button onClick={() => navigate("/login")} className="rounded-full bg-[#4169e1] px-7 py-4 text-sm font-semibold text-white shadow-lg shadow-blue-200 transition hover:-translate-y-0.5 hover:bg-[#3158cf]">
            모여ON 시작하기 <span aria-hidden="true">→</span>
          </button>
          <a href="#features" className="rounded-full px-6 py-4 text-sm font-semibold text-slate-700 transition hover:bg-white">서비스 둘러보기 ↓</a>
        </div>

        <div className="relative mt-14 w-full max-w-6xl sm:mt-16">
          <div className="overflow-hidden rounded-[1.7rem] border border-slate-200/80 bg-white shadow-[0_30px_90px_-35px_rgba(35,63,129,0.32)] sm:rounded-[2rem]">
            <div className="flex h-11 items-center gap-2 border-b border-slate-100 px-5 sm:h-12 sm:px-7" aria-hidden="true">
              <span className="h-2.5 w-2.5 rounded-full bg-[#ff6259]" /><span className="h-2.5 w-2.5 rounded-full bg-[#ffbd2e]" /><span className="h-2.5 w-2.5 rounded-full bg-[#28c840]" />
            </div>
            <div key={activeDemo} className="flex h-[340px] items-center justify-center overflow-hidden bg-white p-3 sm:h-[480px] sm:p-5 lg:h-[580px]" style={{ animation: "moyeoDemoEnter .5s ease-out" }}>
              <div className="relative flex h-full max-w-full items-center justify-center">
                <img src={selectedDemo.src} alt="" aria-hidden="true" className="max-h-full max-w-full object-contain" />
                <div className="moyeo-cursor pointer-events-none absolute inset-0" style={{ "--cursor-x": selectedDemo.targetX, "--cursor-y": selectedDemo.targetY, animation: "moyeoCursorMove 5s ease-in-out infinite" }} aria-hidden="true">
                  <svg className="absolute left-0 top-0 h-9 w-9 -translate-x-1 -translate-y-1 drop-shadow-[0_2px_4px_rgba(15,23,42,0.45)] sm:h-11 sm:w-11" viewBox="0 0 32 40" fill="none"><path d="M3 2.5v28l7.4-7.1 5.2 11.1 5.3-2.5-5.1-10.8 10.1-.6L3 2.5Z" fill="white" stroke="#172033" strokeWidth="2.5" strokeLinejoin="round" /></svg>
                  <span className="moyeo-click-pulse absolute left-0 top-0 h-8 w-8 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-[#4169e1]" style={{ animation: "moyeoClickPulse 5s ease-out infinite" }} />
                </div>
              </div>
            </div>
          </div>
          <style>{`@keyframes moyeoDemoEnter { from { opacity: .4; transform: translateY(7px); } to { opacity: 1; transform: translateY(0); } } @keyframes moyeoStoryEnter { from { opacity: .55; transform: translateY(5px) scale(.99); } to { opacity: 1; transform: translateY(0) scale(1); } } @keyframes moyeoCursorMove { 0%, 9% { left: 13%; top: 17%; transform: scale(1); } 43% { left: var(--cursor-x); top: var(--cursor-y); transform: scale(1); } 49% { left: var(--cursor-x); top: var(--cursor-y); transform: scale(.82); } 56%, 100% { left: var(--cursor-x); top: var(--cursor-y); transform: scale(1); } } @keyframes moyeoClickPulse { 0%, 43%, 100% { opacity: 0; transform: translate(-50%, -50%) scale(.25); } 47% { opacity: .75; transform: translate(-50%, -50%) scale(.65); } 58% { opacity: 0; transform: translate(-50%, -50%) scale(1.35); } } @media (prefers-reduced-motion: reduce) { .moyeo-cursor, .moyeo-click-pulse { display: none !important; } .moyeo-story-preview { animation: none !important; } }`}</style>
          <div className="mt-4 flex items-center justify-center gap-2.5" role="tablist" aria-label="모임 사용 흐름">
            {demoScreens.map((screen, index) => <button key={screen.src} type="button" role="tab" onClick={() => setActiveDemo(index)} aria-label={`${index + 1}단계: ${screen.name}`} aria-selected={activeDemo === index} className={`h-2 rounded-full transition-all duration-300 ${activeDemo === index ? "w-8 bg-[#4169e1]" : "w-2 bg-slate-300 hover:bg-slate-400"}`} />)}
          </div>
        </div>
      </section>

      <section id="how-it-works" className="relative isolate overflow-hidden bg-white py-12 sm:py-16">
        <div aria-hidden="true" className="absolute inset-y-6 left-1/2 z-0 w-[145%] -translate-x-1/2 bg-gradient-to-r from-[#075bef] via-[#3235f4] to-[#5700f5] shadow-[0_35px_100px_-65px_rgba(32,57,210,0.75)]" style={{ borderRadius: "50% / 12%" }} />
        <div aria-hidden="true" className="pointer-events-none absolute inset-0 z-10 opacity-70" style={{ backgroundImage: "radial-gradient(circle at 1px 1px, rgba(255,255,255,.22) 1px, transparent 0)", backgroundSize: "27px 27px", maskImage: "linear-gradient(to bottom, transparent 0%, black 16%, black 86%, transparent 100%)" }} />
        <div className="relative z-20 mx-auto max-w-7xl px-6 py-1 sm:px-10 sm:py-2">
          <div className="mx-auto mb-7 max-w-4xl text-center text-white sm:mb-9">
            <h2 className="text-4xl font-bold leading-tight tracking-tight sm:text-6xl">모여ON과 함께라면<br />모임이 달라집니다</h2>
            <p className="mx-auto mt-3 max-w-2xl text-base leading-6 text-blue-100 sm:mt-4 sm:text-lg sm:leading-7">모임을 만들고, 모두의 시간을 맞추고, 장소를 찾는 과정까지<br className="hidden sm:block" /></p>
          </div>
          <div className="mx-auto grid max-w-6xl items-center gap-6 lg:grid-cols-[0.78fr_1.5fr] lg:gap-10">
            <div className="flex flex-col gap-2" role="tablist" aria-label="모여ON 기능 소개">
              {stories.map((story, index) => <button key={story.eyebrow} type="button" role="tab" aria-selected={activeStory === index} onClick={() => setActiveStory(index)} className={`group flex w-full items-center gap-4 rounded-2xl border px-4 py-4 text-left transition sm:px-5 sm:py-5 ${activeStory === index ? "border-white/70 bg-white text-slate-900 shadow-xl shadow-blue-950/15" : "border-white/10 bg-white/[0.06] text-white/85 backdrop-blur-sm hover:border-white/25 hover:bg-white/[0.12] hover:text-white"}`}><span className={`grid h-10 w-10 shrink-0 place-items-center rounded-xl text-sm font-bold ${activeStory === index ? "bg-blue-50 text-[#4169e1]" : "bg-white/10 text-white"}`}>0{index + 1}</span><span className="min-w-0 flex-1"><span className="block whitespace-pre-line break-keep text-base font-bold leading-snug sm:text-lg">{story.label}</span></span><span className={`ml-auto shrink-0 text-xl transition-transform ${activeStory === index ? "translate-x-0 text-[#4169e1]" : "-translate-x-1 text-white/50 group-hover:translate-x-0"}`}>→</span></button>)}
            </div>
            <div role="tabpanel" className="min-w-0 overflow-hidden rounded-[1.4rem] border border-white/80 bg-white p-2 shadow-[0_25px_80px_-35px_rgba(10,24,100,0.65)] sm:rounded-[1.8rem] sm:p-3">
              <div className="flex h-8 items-center gap-1.5 px-2 sm:h-9"><span className="h-2.5 w-2.5 rounded-full bg-[#ff6259]" /><span className="h-2.5 w-2.5 rounded-full bg-[#ffbd2e]" /><span className="h-2.5 w-2.5 rounded-full bg-[#28c840]" /></div>
              <div key={selectedStory.preview} className="moyeo-story-preview min-h-[300px] w-full rounded-xl bg-slate-50 p-3 sm:min-h-[340px] sm:p-4 lg:min-h-[360px]" style={{ animation: "moyeoStoryEnter .45s ease-out" }}>
                <StoryPreview type={selectedStory.preview} />
              </div>
            </div>
          </div>
          <div className="mt-6 flex items-center justify-center gap-2 lg:hidden">{stories.map((story, index) => <button key={story.eyebrow} aria-label={`${index + 1}번 기능 보기`} onClick={() => setActiveStory(index)} className={`h-1.5 rounded-full transition-all ${activeStory === index ? "w-7 bg-white" : "w-1.5 bg-white/40"}`} />)}</div>
          <div className="mt-5 hidden items-center justify-center gap-2 text-[10px] font-semibold tracking-[0.14em] text-white/60 lg:flex"><span className="h-px w-10 bg-white/30" />SELECT A FEATURE TO PREVIEW<span className="h-px w-10 bg-white/30" /></div>
        </div>
      </section>

      <section className="bg-[#f7f8fb] py-24 sm:py-32">
        <div className="mx-auto max-w-6xl px-6">
          <div className="mx-auto mb-14 max-w-2xl text-center sm:mb-16">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#4169e1]">SOUNDS FAMILIAR?</p>
            <h2 className="mt-4 text-3xl font-bold tracking-tight sm:text-5xl">약속 하나 잡는데<br />왜 이렇게 오래 걸릴까요?</h2>
            <p className="mt-5 text-base leading-7 text-slate-500">모여ON은 모임을 준비할 때 생기는 작은 번거로움을 덜어드려요.</p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            {problems.map(([number, title, desc]) => <article key={number} className="group rounded-3xl border border-slate-100 bg-white p-7 transition duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-slate-200/60 sm:p-9"><div className="flex items-center justify-between"><span className="text-xs font-bold tracking-[0.16em] text-[#4169e1]">PROBLEM {number}</span><span className="text-xl text-slate-300 transition group-hover:text-[#4169e1]">↗</span></div><h3 className="mt-8 text-xl font-bold tracking-tight sm:text-2xl">{title}</h3><p className="mt-3 max-w-md text-sm leading-6 text-slate-500">{desc}</p></article>)}
          </div>
        </div>
      </section>

      <section id="features" className="bg-white py-20 sm:py-28">
        <div className="mx-auto max-w-7xl px-6">
          <div className="mx-auto mb-10 max-w-3xl text-center sm:mb-12">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-[#4169e1]">MADE FOR YOUR GATHERINGS</p>
            <h2 className="mt-4 text-3xl font-bold leading-tight tracking-tight sm:text-5xl">모여ON은 이렇게 해결합니다</h2>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 lg:gap-5">
            {productFeatures.map((feature) => <article key={feature.type} className="group rounded-[1.5rem] border border-slate-200 bg-white p-4 transition duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-slate-200/60 sm:p-5">
              <div className="flex h-36 items-center justify-center rounded-[1.1rem] bg-[#f5f7fa] px-4"><FeaturePreview type={feature.type} /></div>
              <div className="px-1 pb-1 pt-4"><h3 className="text-base font-bold tracking-tight text-slate-900 sm:text-lg">{feature.title}</h3><p className="mt-1.5 text-sm leading-5 text-slate-500">{feature.desc}</p></div>
            </article>)}
          </div>
        </div>
      </section>



      <section id="about" className="relative isolate overflow-hidden bg-white py-20 sm:py-24">
        <div aria-hidden="true" className="absolute inset-y-5 left-1/2 z-0 w-[145%] -translate-x-1/2 bg-gradient-to-r from-[#075bef] via-[#3235f4] to-[#5700f5]" style={{ borderRadius: "50% / 14%" }} />
        <div aria-hidden="true" className="pointer-events-none absolute inset-0 z-10 opacity-60" style={{ backgroundImage: "radial-gradient(circle at 1px 1px, rgba(255,255,255,.22) 1px, transparent 0)", backgroundSize: "27px 27px", maskImage: "linear-gradient(to bottom, transparent 0%, black 20%, black 80%, transparent 100%)" }} />
        <div className="relative z-20 mx-auto flex max-w-5xl flex-col items-center px-6 py-8 text-center text-white sm:py-12">
          <h2 className="text-4xl font-bold tracking-tight sm:text-6xl">다음 약속은, 모여ON에서</h2>
          <p className="mt-4 text-base text-blue-100 sm:text-lg">지금 바로 시작해보세요.</p>
          <button onClick={() => navigate("/login")} className="mt-8 rounded-xl bg-white px-8 py-4 text-sm font-bold text-[#3626d9] shadow-lg shadow-blue-950/15 transition hover:-translate-y-0.5 hover:bg-blue-50">로그인하러 가기 <span aria-hidden="true">→</span></button>
        </div>
      </section>

      <footer className="border-t border-slate-100 px-6 py-8"><div className="mx-auto flex max-w-7xl flex-col items-center justify-between gap-4 text-xs text-slate-400 sm:flex-row"><a href="#home" className="font-bold text-slate-700">모여ON</a><span>함께하는 순간을 더 쉽게.</span><span>© {new Date().getFullYear()} 모여ON</span></div></footer>
    </main>
  );
}

export default LandingPage;
