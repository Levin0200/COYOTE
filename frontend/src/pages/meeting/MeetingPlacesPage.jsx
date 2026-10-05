import { useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";

const MODES = [
  { id: "MANUAL", icon: "✏️", title: "직접 입력", description: "원하는 장소나 지역을 직접 정해요." },
  { id: "RECOMMEND", icon: "✨", title: "추천받기", description: "기준 지역 주변의 장소를 추천받아요." },
  { id: "SKIP", icon: "⏭️", title: "PASS", description: "장소는 나중에 정해도 괜찮아요." },
];
const MOCK_PLACES = [
  { id: 1, name: "센트럴파크 뷰 카페", area: "송도", distance: "0.8km" },
  { id: 2, name: "트리플스트리트 라운지", area: "송도", distance: "1.2km" },
  { id: 3, name: "커낼워크 브런치", area: "송도", distance: "1.6km" },
];
const readJson = (key) => { try { return JSON.parse(sessionStorage.getItem(key) || "{}"); } catch { return {}; } };

function MeetingPlacesPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { inviteToken } = useParams();
  const isGuest = location.pathname.startsWith("/invite/");
  const key = isGuest ? `coyoteGuestDraft_${inviteToken}` : "coyoteMeetingDraft";
  const draft = readJson(key);
  const [placeMode, setPlaceMode] = useState(draft.placeMode || "");
  const [region, setRegion] = useState(draft.region || "");
  const [searchedRegion, setSearchedRegion] = useState(draft.region || "");
  const [placePreference, setPlacePreference] = useState(draft.placePreference || "");

  const searchRegion = () => {
    if (!region.trim()) return alert("지역을 입력해주세요.");
    setSearchedRegion(region.trim());
    if (placeMode === "MANUAL") setPlacePreference(region.trim());
  };
  const saveAndNext = () => {
    if (!placeMode) return alert("장소 응답 방식을 선택해주세요.");
    if (placeMode !== "SKIP" && !placePreference) return alert(placeMode === "MANUAL" ? "검색 후 위치를 선택해주세요." : "추천 장소를 선택해주세요.");
    sessionStorage.setItem(key, JSON.stringify({ ...draft, placeMode, region: placeMode === "SKIP" ? "" : searchedRegion, placePreference: placeMode === "SKIP" ? "" : placePreference }));
    navigate(isGuest ? `/invite/${inviteToken}/review` : "/meeting/setup/review");
  };

  return (
    <div className="min-h-screen bg-slate-50 px-6 py-10 text-slate-800"><div className="mx-auto max-w-5xl">
      <header className="mb-10"><p className="mb-3 text-sm font-semibold text-blue-600">Place preference</p><h1 className="mb-3 text-4xl font-black text-slate-950">장소는 어떻게 정할까요?</h1><p className="text-slate-500">원하는 방식을 선택해 주세요. 실제 지도와 추천 API는 추후 연결됩니다.</p></header>
      <div className="mb-8 grid gap-4 md:grid-cols-3">{MODES.map((mode) => <button key={mode.id} onClick={() => { setPlaceMode(mode.id); setPlacePreference(""); }} className={`rounded-3xl border-2 p-6 text-left transition ${placeMode === mode.id ? "border-blue-500 bg-blue-50 shadow-md" : "border-transparent bg-white shadow-sm hover:border-blue-100"}`}><span className="text-3xl">{mode.icon}</span><h2 className="mt-4 text-xl font-black text-slate-900">{mode.title}</h2><p className="mt-2 text-sm leading-6 text-slate-500">{mode.description}</p></button>)}</div>
      {placeMode && placeMode !== "SKIP" && <section className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm"><h2 className="mb-4 text-xl font-black text-slate-900">{placeMode === "MANUAL" ? "장소 또는 지역 검색" : "추천 기준 지역"}</h2><div className="flex gap-3"><input value={region} onChange={(e) => setRegion(e.target.value)} onKeyDown={(e) => e.key === "Enter" && searchRegion()} className="min-w-0 flex-1 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 outline-none focus:ring-2 focus:ring-blue-500" placeholder="예) 송도" /><button onClick={searchRegion} className="rounded-2xl bg-slate-900 px-5 font-bold text-white">검색</button></div>
          {searchedRegion && <div className="mt-5"><p className="mb-3 text-sm font-bold text-slate-500">검색 결과</p><button onClick={() => setPlacePreference(searchedRegion)} className={`w-full rounded-2xl border p-4 text-left font-bold ${placePreference === searchedRegion ? "border-blue-500 bg-blue-50 text-blue-700" : "border-slate-200 bg-slate-50"}`}>📍 {searchedRegion} 중심 지역 <span className="float-right text-sm font-medium">이 위치 선택</span></button></div>}
        </div>
        <div className="relative min-h-72 overflow-hidden rounded-[2rem] border border-blue-100 bg-gradient-to-br from-blue-50 via-slate-50 to-emerald-50 shadow-sm"><div className="absolute inset-0 opacity-30" style={{ backgroundImage: "linear-gradient(#94a3b8 1px, transparent 1px), linear-gradient(90deg, #94a3b8 1px, transparent 1px)", backgroundSize: "42px 42px" }} /><div className="absolute inset-0 flex flex-col items-center justify-center"><span className="text-5xl drop-shadow-md">📍</span><p className="mt-3 rounded-full bg-white/90 px-4 py-2 font-bold text-slate-700 shadow-sm">{searchedRegion || "지역을 검색하면 포인터가 표시됩니다"}</p><span className="mt-2 text-xs font-semibold text-slate-400">MAP PLACEHOLDER</span></div></div>
      </section>}
      {placeMode === "RECOMMEND" && searchedRegion && <section className="mt-6 rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm"><div className="mb-5 flex items-center justify-between"><h2 className="text-xl font-black text-slate-900">Mock 추천 장소</h2><span className="rounded-full bg-purple-50 px-3 py-1 text-xs font-bold text-purple-600">API 연결 예정</span></div><div className="grid gap-3 md:grid-cols-3">{MOCK_PLACES.map((place) => <button key={place.id} onClick={() => setPlacePreference(place.name)} className={`rounded-2xl border p-4 text-left transition ${placePreference === place.name ? "border-blue-500 bg-blue-50" : "border-slate-200 bg-slate-50 hover:bg-slate-100"}`}><p className="font-black text-slate-900">{place.name}</p><p className="mt-2 text-sm text-slate-500">{searchedRegion} · {place.distance}</p></button>)}</div></section>}
      {placeMode === "SKIP" && <div className="rounded-[2rem] border border-dashed border-slate-300 bg-white p-10 text-center"><span className="text-4xl">👌</span><h2 className="mt-4 text-xl font-black text-slate-900">장소 응답 없이 진행합니다</h2><p className="mt-2 text-slate-500">나중에 참여자들과 장소를 정할 수 있어요.</p></div>}
      <div className="mt-8 flex gap-4"><button onClick={() => navigate(-1)} className="w-1/3 rounded-2xl border border-slate-200 bg-white py-4 font-bold">이전으로</button><button onClick={saveAndNext} className="w-2/3 rounded-2xl bg-blue-600 py-4 font-bold text-white shadow-sm hover:bg-blue-700">응답 확인하기</button></div>
    </div></div>
  );
}

export default MeetingPlacesPage;
