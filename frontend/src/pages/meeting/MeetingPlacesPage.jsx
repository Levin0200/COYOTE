import { useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { getMeetingByInviteToken, getResponses } from "../../services/meetingStorage";

const PLACE_STATUS = [
  { id: "FIXED", icon: "📌", title: "장소 확정됨", description: "이미 정해진 장소를 입력합니다." },
  { id: "UNDECIDED", icon: "❓", title: "장소 미확정", description: "추천을 받거나 장소 응답을 건너뜁니다." },
];
const UNDECIDED_MODES = [
  { id: "RECOMMEND", icon: "✨", title: "공통 범위 추천", description: "모두에게 같은 추천 후보를 보여줘요." },
  { id: "OPEN", icon: "⏭️", title: "건너뛰기", description: "장소를 정하지 않고 다음 단계로 이동해요." },
];
const readJson = (key) => { try { return JSON.parse(sessionStorage.getItem(key) || "{}"); } catch { return {}; } };
const makeOptions = (region, keyword, radius) => ["A", "B", "C"].map((suffix, index) => ({ id: index + 1, name: `${region} ${keyword.trim() || "모임 장소"} ${suffix}`, distance: `${Math.min(radius, (index + 1) * 0.5).toFixed(1)}km` }));

function MeetingPlacesPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { inviteToken } = useParams();
  const isGuest = location.pathname.startsWith("/invite/");
  const key = isGuest ? `coyoteGuestDraft_${inviteToken}` : "coyoteMeetingDraft";
  const draft = readJson(key);
  const meeting = isGuest ? getMeetingByInviteToken(inviteToken) : null;
  const policy = meeting?.placePolicy || {};
  const initialGuestMode = policy.mode === "OPEN" ? (["PROPOSE", "OPEN"].includes(draft.placeMode) ? draft.placeMode : "OPEN") : policy.mode || "OPEN";
  const [placeMode, setPlaceMode] = useState(isGuest ? initialGuestMode : draft.placeMode || "");
  const [placeStatus, setPlaceStatus] = useState((isGuest ? policy.mode : draft.placeMode) === "FIXED" ? "FIXED" : (isGuest || draft.placeMode ? "UNDECIDED" : ""));
  const [region, setRegion] = useState(isGuest ? policy.region || "" : draft.region || "");
  const [radius, setRadius] = useState(Number(isGuest ? policy.radius || 3 : draft.recommendationRadius || 3));
  const [keyword, setKeyword] = useState(isGuest ? policy.keyword || "" : draft.recommendationKeyword || "");
  const [fixedPlace, setFixedPlace] = useState(isGuest ? policy.fixedPlace || "" : draft.fixedPlace || draft.placePreference || "");
  const [searched, setSearched] = useState(Boolean(isGuest ? policy.region : draft.recommendationOptions?.length));
  const [placePreference, setPlacePreference] = useState(draft.placePreference || (isGuest && policy.mode === "FIXED" ? policy.fixedPlace : ""));
  const options = useMemo(() => policy.options?.length ? policy.options : makeOptions(region.trim(), keyword, radius), [policy.options, region, keyword, radius]);
  const sharedProposals = useMemo(() => {
    if (!meeting) return [];
    return [...new Set(getResponses(meeting.meetingId).filter((response) => response.placeMode === "PROPOSE" && response.placePreference).map((response) => response.placePreference))];
  }, [meeting]);
  const guestProposalFlow = isGuest && policy.mode === "OPEN";

  const searchPlaces = () => {
    if (!region.trim()) return alert("추천 기준 지역을 입력해주세요.");
    if (placeMode === "PROPOSE" && !keyword.trim()) return alert("어떤 장소를 추천받을지 키워드를 입력해주세요.");
    setPlacePreference("");
    setSearched(true);
  };
  const saveAndNext = () => {
    if (!placeMode) return alert("장소 방식을 선택해주세요.");
    if (placeMode === "FIXED" && !fixedPlace.trim()) return alert("확정된 장소를 입력해주세요.");
    if (placeMode === "RECOMMEND" && (!region.trim() || !searched)) return alert("추천 기준 지역과 범위를 설정해주세요.");
    if (placeMode === "RECOMMEND" && !placePreference) return alert("추천 장소를 선택해주세요.");
    if (placeMode === "PROPOSE" && !placePreference) return alert("기존 공동 후보를 선택하거나 새로운 장소를 제안해주세요.");
    sessionStorage.setItem(key, JSON.stringify({ ...draft, placeMode, region: ["RECOMMEND", "PROPOSE"].includes(placeMode) ? region.trim() : "", fixedPlace: placeMode === "FIXED" ? fixedPlace.trim() : "", placePreference: placeMode === "FIXED" ? fixedPlace.trim() : ["RECOMMEND", "PROPOSE"].includes(placeMode) ? placePreference : "", recommendationRadius: ["RECOMMEND", "PROPOSE"].includes(placeMode) ? radius : null, recommendationKeyword: ["RECOMMEND", "PROPOSE"].includes(placeMode) ? keyword.trim() : "", recommendationOptions: placeMode === "RECOMMEND" ? options : [] }));
    navigate(isGuest ? `/invite/${inviteToken}/review` : "/meeting/setup/review");
  };

  return <div className="min-h-screen bg-slate-50 px-6 py-10 text-slate-800"><div className="mx-auto max-w-5xl">
    <header className="mb-10"><p className="mb-3 text-sm font-semibold text-blue-600">Place preference</p><h1 className="mb-3 text-4xl font-black text-slate-950">장소는 어떻게 정할까요?</h1><p className="text-slate-500">{isGuest ? "호스트가 정한 장소 정책 안에서 응답해 주세요." : "확정 장소를 안내하거나, 모든 참여자가 함께 볼 추천 범위를 정해 주세요."}</p></header>
    {!isGuest && <><div className="mb-6 grid gap-4 md:grid-cols-2">{PLACE_STATUS.map((status) => <button key={status.id} onClick={() => { setPlaceStatus(status.id); setPlaceMode(status.id === "FIXED" ? "FIXED" : ""); setPlacePreference(""); setSearched(false); }} className={`rounded-3xl border-2 p-6 text-left transition ${placeStatus === status.id ? "border-blue-500 bg-blue-50 shadow-md" : "border-transparent bg-white shadow-sm hover:border-blue-100"}`}><span className="text-3xl">{status.icon}</span><h2 className="mt-4 text-xl font-black">{status.title}</h2><p className="mt-2 text-sm leading-6 text-slate-500">{status.description}</p></button>)}</div>{placeStatus === "UNDECIDED" && <section className="mb-8 rounded-[2rem] border border-blue-100 bg-white p-6 shadow-sm"><p className="mb-4 text-sm font-black text-blue-600">장소 미확정 · 다음 방법을 선택하세요</p><div className="grid gap-4 md:grid-cols-2">{UNDECIDED_MODES.map((mode) => <button key={mode.id} onClick={() => { setPlaceMode(mode.id); setPlacePreference(""); setSearched(false); }} className={`rounded-2xl border-2 p-5 text-left transition ${placeMode === mode.id ? "border-blue-500 bg-blue-50" : "border-slate-100 bg-slate-50 hover:border-blue-100"}`}><span className="text-2xl">{mode.icon}</span><h2 className="mt-3 text-lg font-black">{mode.title}</h2><p className="mt-1 text-sm text-slate-500">{mode.description}</p></button>)}</div></section>}</>}
    {isGuest && <div className="mb-6 rounded-2xl border border-blue-100 bg-blue-50 p-5"><p className="text-sm font-black text-blue-800">{policy.mode === "FIXED" ? "호스트가 장소를 확정했습니다" : policy.mode === "RECOMMEND" ? "아래 공통 후보 중 원하는 장소를 선택해 주세요" : "호스트가 장소 결정을 게스트 제안에 열어두었습니다"}</p><p className="mt-1 text-sm text-blue-600">{policy.mode === "OPEN" ? "장소를 제안하거나 이번 장소 의견만 건너뛸 수 있습니다." : "게스트는 호스트의 설정을 변경할 수 없습니다."}</p></div>}
    {guestProposalFlow && <section className="mb-6 rounded-[2rem] border border-blue-100 bg-white p-6 shadow-sm"><p className="mb-4 text-sm font-black text-blue-600">장소 의견을 선택하세요</p><div className="grid gap-4 md:grid-cols-2"><button onClick={() => { setPlaceMode("PROPOSE"); setRegion(""); setKeyword(""); setRadius(3); setPlacePreference(""); setSearched(false); }} className={`rounded-2xl border-2 p-5 text-left ${placeMode === "PROPOSE" ? "border-blue-500 bg-blue-50" : "border-slate-100 bg-slate-50"}`}><span className="text-2xl">✨</span><h2 className="mt-3 text-lg font-black">장소 제안하기</h2><p className="mt-1 text-sm text-slate-500">기존 공동 후보를 선택하거나 지역과 원하는 장소 유형을 입력해 새 후보를 찾아요.</p></button><button onClick={() => { setPlaceMode("OPEN"); setRegion(""); setKeyword(""); setPlacePreference(""); setSearched(false); }} className={`rounded-2xl border-2 p-5 text-left ${placeMode === "OPEN" ? "border-blue-500 bg-blue-50" : "border-slate-100 bg-slate-50"}`}><span className="text-2xl">⏭️</span><h2 className="mt-3 text-lg font-black">내 장소 의견 건너뛰기</h2><p className="mt-1 text-sm text-slate-500">다른 게스트의 장소 제안에는 영향을 주지 않습니다.</p></button></div></section>}
    {placeMode === "FIXED" && <section className="grid gap-6 rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm lg:grid-cols-[0.9fr_1.1fr]"><div><h2 className="text-xl font-black">이미 정해진 장소</h2><p className="mt-2 text-sm text-slate-500">투표 없이 이 장소를 최종 장소로 사용합니다.</p>{isGuest ? <div className="mt-5 rounded-2xl border border-blue-100 bg-blue-50 p-5"><p className="text-xs font-bold text-blue-500">확정 장소</p><p className="mt-2 text-lg font-black text-blue-950">📍 {fixedPlace}</p></div> : <input value={fixedPlace} onChange={(e) => setFixedPlace(e.target.value)} className="mt-5 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4" placeholder="예) 강남역 3번 출구 앞" />}</div><VenueMap title={fixedPlace || "확정 장소를 입력하면 지도에 표시됩니다"} places={fixedPlace ? [{ id: 1, name: fixedPlace }] : []} selected={fixedPlace} /></section>}
    {(placeMode === "RECOMMEND" || placeMode === "PROPOSE") && <>{placeMode === "PROPOSE" && sharedProposals.length > 0 && <section className="mb-6 rounded-[2rem] border border-emerald-100 bg-white p-6 shadow-sm"><h2 className="text-xl font-black">게스트 공동 제안</h2><p className="mt-1 text-sm text-slate-500">앞서 제출된 장소를 선택하거나 아래에서 새 장소를 찾아 제안할 수 있습니다.</p><div className="mt-4 flex flex-wrap gap-2">{sharedProposals.map((place) => <button key={place} onClick={() => setPlacePreference(place)} className={`rounded-xl border px-4 py-3 text-sm font-bold ${placePreference === place ? "border-emerald-500 bg-emerald-50 text-emerald-700" : "border-slate-200 bg-slate-50"}`}>📍 {place}</button>)}</div></section>}{placeMode === "PROPOSE" || !isGuest ? <section className="grid gap-4 rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm md:grid-cols-3"><Field label="추천 기준 지역"><input value={region} onChange={(e) => { setRegion(e.target.value); setSearched(false); }} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3" placeholder="예) 송도, 강남역" /></Field><Field label="추천 범위"><select value={radius} onChange={(e) => { setRadius(Number(e.target.value)); setSearched(false); }} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">{[1, 3, 5, 10].map((value) => <option key={value} value={value}>반경 {value}km</option>)}</select></Field><Field label={placeMode === "PROPOSE" ? "원하는 장소 유형 *" : "추천 키워드"}><input value={keyword} onChange={(e) => { setKeyword(e.target.value); setSearched(false); }} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3" placeholder="예) 조용한 카페, 한식, 주차 가능" /></Field><button onClick={searchPlaces} className="rounded-2xl bg-slate-900 px-5 py-3 font-bold text-white md:col-span-3">{placeMode === "PROPOSE" ? "입력한 기준으로 장소 찾기" : "이 조건으로 장소 추천받기"}</button></section> : <section className="flex flex-wrap gap-2 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-bold">📍 {region}</span><span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-bold">반경 {radius}km</span>{keyword && <span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-bold">🔎 {keyword}</span>}</section>}{(searched || (isGuest && placeMode === "RECOMMEND")) && <section className="mt-6 grid gap-6 rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm lg:grid-cols-[1.1fr_0.9fr]"><VenueMap title={`${region} 반경 ${radius}km`} places={options} selected={placePreference} /><div><h2 className="text-xl font-black">{placeMode === "PROPOSE" ? "입력 기준 추천 결과" : "공통 추천 장소"}</h2><p className="mt-1 text-sm text-slate-500">지도와 목록에서 같은 후보를 확인하고 하나를 선택하세요.</p><div className="mt-5 space-y-3">{options.map((place, index) => <button key={place.id} onClick={() => setPlacePreference(place.name)} className={`flex w-full items-center gap-3 rounded-2xl border p-4 text-left ${placePreference === place.name ? "border-blue-500 bg-blue-50" : "border-slate-200 bg-slate-50"}`}><span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-black ${placePreference === place.name ? "bg-blue-600 text-white" : "bg-white text-slate-600"}`}>{index + 1}</span><span><span className="block font-black">{place.name}</span><span className="mt-1 block text-sm text-slate-500">{region} · {place.distance}</span></span></button>)}</div></div></section>}</>}
    {placeMode === "OPEN" && <div className="rounded-[2rem] border border-dashed border-slate-300 bg-white p-10 text-center"><span className="text-4xl">⏭️</span><h2 className="mt-4 text-xl font-black">{guestProposalFlow ? "내 장소 의견을 건너뜁니다" : "장소 선택을 게스트 제안에 열어둡니다"}</h2><p className="mt-2 text-slate-500">{guestProposalFlow ? "다른 게스트가 제출한 장소 제안은 그대로 최종 집계에 반영됩니다." : "게스트는 장소를 제안하거나 본인 의견만 건너뛸 수 있습니다."}</p></div>}
    <div className="mt-8 flex gap-4"><button onClick={() => navigate(-1)} className="w-1/3 rounded-2xl border border-slate-200 bg-white py-4 font-bold">이전으로</button><button onClick={saveAndNext} className="w-2/3 rounded-2xl bg-blue-600 py-4 font-bold text-white">응답 확인하기</button></div>
  </div></div>;
}

function Field({ label, children }) { return <label><span className="mb-2 block text-sm font-bold">{label}</span>{children}</label>; }

function VenueMap({ title, places, selected }) {
  return <div className="relative min-h-80 overflow-hidden rounded-[2rem] border border-blue-100 bg-gradient-to-br from-blue-50 via-slate-50 to-emerald-50"><div className="absolute inset-0 opacity-30" style={{ backgroundImage: "linear-gradient(#94a3b8 1px, transparent 1px), linear-gradient(90deg, #94a3b8 1px, transparent 1px)", backgroundSize: "42px 42px" }} /><div className="absolute left-4 top-4 z-10 rounded-xl bg-white/95 px-4 py-2 text-sm font-black text-slate-700 shadow-sm">🗺️ {title}</div>{places.map((place, index) => <div key={place.id} className="absolute z-10 -translate-x-1/2 -translate-y-full" style={{ left: `${30 + index * 20}%`, top: `${48 + (index % 2) * 18}%` }}><div className={`flex h-10 w-10 items-center justify-center rounded-full border-4 border-white text-sm font-black shadow-lg ${selected === place.name ? "bg-blue-600 text-white" : "bg-slate-800 text-white"}`}>{index + 1}</div><div className="mt-1 max-w-36 truncate rounded-lg bg-white/95 px-2 py-1 text-xs font-bold shadow">{place.name}</div></div>)}{!places.length && <div className="absolute inset-0 flex items-center justify-center text-sm font-bold text-slate-400">장소를 입력하면 지도가 표시됩니다</div>}<div className="absolute bottom-3 right-4 text-[10px] font-bold text-slate-400">MAP PREVIEW · 지도 API 연결 예정</div></div>;
}

export default MeetingPlacesPage;
