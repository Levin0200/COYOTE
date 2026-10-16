import { useState } from "react";
import { Navigate, useLocation, useNavigate, useParams } from "react-router-dom";
import KakaoMap from "../../components/KakaoMap";
import PlaceCandidateList from "../../components/place/PlaceCandidateList";
import { recommendPlaces, searchPlacesAsGuest } from "../../services/meetingService";
import { getMeetingErrorMessage } from "../../utils/meetingErrors";
import { DEFAULT_CENTER } from "../../utils/kakaoMap";
import { centerOf, fromOption, fromPlaceDetail, fromSearchResult, hasCoordinates, toOption, toPlaceDetail } from "../../utils/placeCandidates";

const PLACE_STATUS = [
  { id: "FIXED", icon: "📌", title: "장소 확정됨", description: "이미 정해진 장소를 입력합니다." },
  { id: "UNDECIDED", icon: "❓", title: "장소 미확정", description: "추천을 받거나 장소 응답을 건너뜁니다." },
];
const UNDECIDED_MODES = [
  { id: "RECOMMEND", icon: "✨", title: "공통 범위 추천", description: "모두에게 같은 추천 후보를 보여줘요." },
  { id: "OPEN", icon: "⏭️", title: "건너뛰기", description: "장소를 정하지 않고 다음 단계로 이동해요." },
];
// 서버가 4.5km까지 검색하지만 가까운 곳부터 추천하므로 화면의 선택지는 1·2·3km로 한다.
const RADIUS_KM = [1, 2, 3];
const FIRST_PAGE = 3;
const readJson = (key) => { try { return JSON.parse(sessionStorage.getItem(key) || "{}"); } catch { return {}; } };

function MeetingPlacesPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { inviteToken } = useParams();
  const isGuest = location.pathname.startsWith("/invite/");
  const key = isGuest ? `coyoteGuestDraft_${inviteToken}` : "coyoteMeetingDraft";
  const draft = readJson(key);
  const policy = (isGuest ? draft.placePolicy : null) || {};
  const locked = !isGuest && Boolean(draft.policyLocked);
  // 이미 서버에 저장된 추천 후보(게스트 화면, 잠긴 호스트 수정) 또는 호스트가 이전에 고른 후보
  const savedOptions = (isGuest ? policy.options : locked ? draft.placePolicy?.options : draft.recommendationOptions) || [];
  const initialGuestMode = policy.mode === "OPEN" ? (["PROPOSE", "OPEN"].includes(draft.placeMode) ? draft.placeMode : "OPEN") : policy.mode || "OPEN";
  const startMode = isGuest ? initialGuestMode : draft.placeMode || "";

  const [placeMode, setPlaceMode] = useState(startMode);
  const [placeStatus, setPlaceStatus] = useState((isGuest ? policy.mode : draft.placeMode) === "FIXED" ? "FIXED" : (isGuest || draft.placeMode ? "UNDECIDED" : ""));
  const [region, setRegion] = useState(isGuest ? policy.region || "" : draft.region || "");
  const [radius, setRadius] = useState(Number(isGuest ? policy.radius || 2 : draft.recommendationRadius || 2));
  const [keyword, setKeyword] = useState(isGuest ? policy.keyword || "" : draft.recommendationKeyword || "");
  const [fixedPlace, setFixedPlace] = useState(isGuest ? policy.fixedPlace || "" : draft.fixedPlace || draft.placePreference || "");

  // 지도에서 고른 기준 위치와 장소 후보. results.places 앞에서 visibleCount개만 보여 준다.
  const initialPlaces = startMode === "RECOMMEND" && savedOptions.length
    ? savedOptions.map(fromOption)
    : startMode === "PROPOSE" && draft.place?.name ? [fromPlaceDetail(draft.place)] : [];
  const [results, setResults] = useState(initialPlaces.length ? { places: initialPlaces, aiUsed: null } : null);
  const [visibleCount, setVisibleCount] = useState(initialPlaces.length || FIRST_PAGE);
  const [refreshed, setRefreshed] = useState(initialPlaces.length > FIRST_PAGE);
  const [selectedKey, setSelectedKey] = useState(draft.placeId || draft.place?.id || draft.placePreference || "");
  const [center, setCenter] = useState(draft.recommendationCenter || centerOf(initialPlaces) || DEFAULT_CENTER);
  const [centerChosen, setCenterChosen] = useState(Boolean(draft.recommendationCenter || initialPlaces.length));
  const [searching, setSearching] = useState(false);
  const [message, setMessage] = useState("");

  const guestProposalFlow = isGuest && policy.mode === "OPEN";
  // 위치를 고르고 장소를 찾는 화면: 호스트의 추천 설정, 게스트의 장소 제안
  const searchable = (!isGuest && placeMode === "RECOMMEND" && !locked) || (isGuest && placeMode === "PROPOSE");
  // 이미 정해진 후보 중에서 고르기만 하는 화면: 게스트의 추천 후보 선택, 잠긴 호스트 수정
  const choosingOnly = placeMode === "RECOMMEND" && !searchable;
  const candidates = results ? results.places.slice(0, visibleCount) : [];
  const markers = candidates.filter(hasCoordinates).map((place) => ({ id: place.key, name: place.name, lat: place.latitude, lng: place.longitude }));
  const radiusChoices = [...new Set([...RADIUS_KM, radius])].sort((a, b) => a - b);

  const resetResults = () => {
    setResults(null);
    setVisibleCount(FIRST_PAGE);
    setRefreshed(false);
    setSelectedKey("");
    setMessage("");
  };
  const pickCenter = ({ lat, lng, label }) => {
    setCenter({ lat, lng });
    setCenterChosen(true);
    if (label && !region.trim()) setRegion(label.slice(0, 40));
    resetResults();
  };
  const applyResults = (places, aiUsed) => {
    setSelectedKey("");
    setRefreshed(false);
    setResults({ places, aiUsed });
    setVisibleCount(isGuest ? places.length : Math.min(FIRST_PAGE, places.length));
    setMessage(places.length === 0 ? "이 위치 주변에서 조건에 맞는 장소를 찾지 못했습니다. 반경을 넓히거나 키워드를 바꿔 보세요." : "");
  };
  const findPlaces = async () => {
    if (!centerChosen) return setMessage("지도에서 기준 위치를 먼저 선택해주세요.");
    if (!isGuest && !region.trim()) return setMessage("기준 지역 이름을 입력해주세요.");
    if (isGuest && !keyword.trim()) return setMessage("어떤 장소를 찾을지 장소 유형을 입력해주세요.");
    setSearching(true);
    setMessage("");
    try {
      if (isGuest) {
        const data = await searchPlacesAsGuest(inviteToken, draft.guestId, { latitude: center.lat, longitude: center.lng, radiusMeters: radius * 1000, keyword: keyword.trim(), limit: 10 });
        applyResults(data.places.map(fromSearchResult), null);
      } else {
        const data = await recommendPlaces({ latitude: center.lat, longitude: center.lng, radiusMeters: radius * 1000, keyword: keyword.trim() || undefined, title: draft.title, description: draft.description });
        applyResults(data.places.map(fromSearchResult), data.aiUsed);
      }
    } catch (error) {
      setMessage(getMeetingErrorMessage(error));
    } finally {
      setSearching(false);
    }
  };
  const showMore = () => {
    setVisibleCount(Math.min(FIRST_PAGE * 2, results.places.length));
    setRefreshed(true);
  };

  const saveAndNext = () => {
    if (!placeMode) return alert("장소 방식을 선택해주세요.");
    if (placeMode === "FIXED" && !fixedPlace.trim()) return alert("확정된 장소를 입력해주세요.");
    const choosing = ["RECOMMEND", "PROPOSE"].includes(placeMode);
    const chosen = choosing ? candidates.find((place) => place.key === selectedKey) : null;
    if (placeMode === "RECOMMEND" && searchable && (!region.trim() || candidates.length === 0)) return alert("기준 지역과 위치를 정하고 장소를 추천받아 주세요.");
    if (placeMode === "RECOMMEND" && !chosen) return alert("추천 장소를 선택해주세요.");
    if (placeMode === "PROPOSE" && !chosen) return alert("지도에서 위치를 정해 장소를 찾은 뒤 하나를 선택해주세요.");
    const options = placeMode !== "RECOMMEND" ? [] : searchable ? candidates.map(toOption) : draft.recommendationOptions || savedOptions;
    sessionStorage.setItem(key, JSON.stringify({
      ...draft,
      placeMode,
      region: choosing ? region.trim() : "",
      fixedPlace: placeMode === "FIXED" ? fixedPlace.trim() : "",
      placePreference: placeMode === "FIXED" ? fixedPlace.trim() : chosen ? chosen.name : "",
      placeId: chosen ? chosen.placeId : "",
      place: chosen ? toPlaceDetail(chosen) : null,
      recommendationRadius: choosing ? radius : null,
      recommendationKeyword: choosing ? keyword.trim() : "",
      recommendationOptions: options,
      recommendationCenter: searchable ? center : draft.recommendationCenter,
    }));
    navigate(isGuest ? `/invite/${inviteToken}/review` : "/meeting/setup/review");
  };

  if (isGuest && !(draft.guestId && draft.placePolicy)) return <Navigate to={`/invite/${inviteToken}`} replace />;

  const aiNote = results?.aiUsed === true ? "AI가 모임 제목·설명과 주변 장소를 보고 골랐어요."
    : results?.aiUsed === false ? "가까운 순서와 업종 다양성으로 골랐어요(지금은 AI 추천을 쓸 수 없어요)." : "";

  return <div className="min-h-screen bg-slate-50 px-6 py-10 text-slate-800"><div className="mx-auto max-w-5xl">
    <header className="mb-10"><p className="mb-3 text-sm font-semibold text-blue-600">Place preference</p><h1 className="mb-3 text-4xl font-black text-slate-950">장소는 어떻게 정할까요?</h1><p className="text-slate-500">{isGuest ? "호스트가 정한 장소 정책 안에서 응답해 주세요." : "확정 장소를 안내하거나, 모든 참여자가 함께 볼 추천 범위를 정해 주세요."}</p></header>
    {locked && <div className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 p-5"><p className="text-sm font-black text-amber-800">게스트 응답이 있어 장소 선택은 수정 불가합니다.</p></div>}

    {!isGuest && <><div className="mb-6 grid gap-4 md:grid-cols-2">{PLACE_STATUS.map((status) => <button key={status.id} disabled={locked} onClick={() => { setPlaceStatus(status.id); setPlaceMode(status.id === "FIXED" ? "FIXED" : ""); resetResults(); }} className={`rounded-3xl border-2 p-6 text-left transition ${placeStatus === status.id ? "border-blue-500 bg-blue-50 shadow-md" : "border-transparent bg-white shadow-sm hover:border-blue-100"} disabled:cursor-not-allowed disabled:opacity-60`}><span className="text-3xl">{status.icon}</span><h2 className="mt-4 text-xl font-black">{status.title}</h2><p className="mt-2 text-sm leading-6 text-slate-500">{status.description}</p></button>)}</div>
      {placeStatus === "UNDECIDED" && <section className="mb-8 rounded-[2rem] border border-blue-100 bg-white p-6 shadow-sm"><p className="mb-4 text-sm font-black text-blue-600">장소 미확정 · 다음 방법을 선택하세요</p><div className="grid gap-4 md:grid-cols-2">{UNDECIDED_MODES.map((mode) => <button key={mode.id} disabled={locked} onClick={() => { setPlaceMode(mode.id); resetResults(); }} className={`rounded-2xl border-2 p-5 text-left transition ${placeMode === mode.id ? "border-blue-500 bg-blue-50" : "border-slate-100 bg-slate-50 hover:border-blue-100"} disabled:cursor-not-allowed disabled:opacity-60`}><span className="text-2xl">{mode.icon}</span><h2 className="mt-3 text-lg font-black">{mode.title}</h2><p className="mt-1 text-sm text-slate-500">{mode.description}</p></button>)}</div></section>}</>}

    {isGuest && <div className="mb-6 rounded-2xl border border-blue-100 bg-blue-50 p-5"><p className="text-sm font-black text-blue-800">{policy.mode === "FIXED" ? "호스트가 장소를 확정했습니다" : policy.mode === "RECOMMEND" ? "아래 공통 후보 중 원하는 장소를 선택해 주세요" : "호스트가 장소 결정을 게스트 제안에 열어두었습니다"}</p><p className="mt-1 text-sm text-blue-600">{policy.mode === "OPEN" ? "장소를 제안하거나 이번 장소 의견만 건너뛸 수 있습니다." : "게스트는 호스트의 설정을 변경할 수 없습니다."}</p></div>}

    {guestProposalFlow && <section className="mb-6 rounded-[2rem] border border-blue-100 bg-white p-6 shadow-sm"><p className="mb-4 text-sm font-black text-blue-600">장소 의견을 선택하세요</p><div className="grid gap-4 md:grid-cols-2"><button onClick={() => { setPlaceMode("PROPOSE"); setRegion(""); setKeyword(""); setRadius(2); setCenterChosen(false); resetResults(); }} className={`rounded-2xl border-2 p-5 text-left ${placeMode === "PROPOSE" ? "border-blue-500 bg-blue-50" : "border-slate-100 bg-slate-50"}`}><span className="text-2xl">✨</span><h2 className="mt-3 text-lg font-black">장소 제안하기</h2><p className="mt-1 text-sm text-slate-500">지도에서 원하는 위치를 정하고 장소 유형을 입력해 후보를 찾은 뒤 하나를 제안해요.</p></button><button onClick={() => { setPlaceMode("OPEN"); setRegion(""); setKeyword(""); resetResults(); }} className={`rounded-2xl border-2 p-5 text-left ${placeMode === "OPEN" ? "border-blue-500 bg-blue-50" : "border-slate-100 bg-slate-50"}`}><span className="text-2xl">⏭️</span><h2 className="mt-3 text-lg font-black">내 장소 의견 건너뛰기</h2><p className="mt-1 text-sm text-slate-500">다른 게스트의 장소 제안에는 영향을 주지 않습니다.</p></button></div></section>}

    {placeMode === "FIXED" && <section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm"><h2 className="text-xl font-black">이미 정해진 장소</h2><p className="mt-2 text-sm text-slate-500">투표 없이 이 장소를 최종 장소로 사용합니다.</p>{isGuest ? <div className="mt-5 rounded-2xl border border-blue-100 bg-blue-50 p-5"><p className="text-xs font-bold text-blue-500">확정 장소</p><p className="mt-2 text-lg font-black text-blue-950">📍 {fixedPlace}</p></div> : <input value={fixedPlace} maxLength={100} disabled={locked} onChange={(e) => setFixedPlace(e.target.value)} className="mt-5 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 disabled:bg-slate-100 disabled:text-slate-400" placeholder="예) 강남역 3번 출구 앞" />}</section>}

    {searchable && <section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm">
      <h2 className="text-xl font-black">{isGuest ? "원하는 위치 정하기" : "모임 기준 위치"}</h2>
      <p className="mt-1 text-sm text-slate-500">지도를 누르거나 핀을 끌어 위치를 정해 주세요. 위쪽 검색창에 지역 이름을 입력하면 지도가 그곳으로 이동해요.</p>
      <div className="mt-5"><KakaoMap center={center} radiusMeters={radius * 1000} markers={markers} selectedId={selectedKey} pickable showSearch onPick={pickCenter} onSelectMarker={setSelectedKey} /></div>
      <div className="mt-5 grid gap-4 md:grid-cols-3">
        <Field label={isGuest ? "지역 이름 (선택)" : "추천 기준 지역 이름 *"}><input value={region} maxLength={40} onChange={(e) => { setRegion(e.target.value); resetResults(); }} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3" placeholder="예) 송도, 강남역" /></Field>
        <Field label="찾을 범위"><select value={radius} onChange={(e) => { setRadius(Number(e.target.value)); resetResults(); }} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">{radiusChoices.map((value) => <option key={value} value={value}>반경 {value}km</option>)}</select></Field>
        <Field label={isGuest ? "원하는 장소 유형 *" : "추천 키워드"}><input value={keyword} maxLength={50} onChange={(e) => { setKeyword(e.target.value); resetResults(); }} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3" placeholder="예) 카페, 한식, 고기, 노래방" /></Field>
      </div>
      {message && <p role="alert" className="mt-4 rounded-xl bg-rose-50 px-4 py-3 text-sm font-bold text-rose-600">{message}</p>}
      <button type="button" onClick={findPlaces} disabled={searching} className="mt-5 w-full rounded-2xl bg-slate-900 px-5 py-3 font-bold text-white disabled:bg-slate-400">{searching ? (isGuest ? "찾는 중..." : "추천 중...") : isGuest ? "입력한 기준으로 장소 찾기" : "이 조건으로 장소 추천받기"}</button>
    </section>}

    {choosingOnly && <section className="rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm">
      <div className="flex flex-wrap gap-2"><span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-bold">📍 {region}</span><span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-bold">반경 {radius}km</span>{keyword && <span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-bold">🔎 {keyword}</span>}</div>
      {markers.length > 0 && <div className="mt-5"><KakaoMap center={center} markers={markers} selectedId={selectedKey} onSelectMarker={setSelectedKey} /></div>}
    </section>}

    {(searchable || choosingOnly) && results && candidates.length > 0 && <section className="mt-6 rounded-[2rem] border border-slate-200 bg-white p-7 shadow-sm">
      <h2 className="text-xl font-black">{isGuest && placeMode === "PROPOSE" ? "찾은 장소" : choosingOnly ? "공통 추천 장소" : "추천 장소"}</h2>
      <p className="mt-1 text-sm text-slate-500">지도와 목록의 번호는 같아요. 하나를 선택하세요.</p>
      {aiNote && <p className="mt-2 text-xs font-bold text-purple-600">✨ {aiNote}</p>}
      <div className="mt-5"><PlaceCandidateList candidates={candidates} selectedKey={selectedKey} onSelect={setSelectedKey} /></div>
      {searchable && !isGuest && results.places.length > FIRST_PAGE && !refreshed && <button type="button" onClick={showMore} className="mt-5 w-full rounded-2xl border border-slate-200 bg-white px-5 py-3 font-bold text-slate-700 hover:bg-slate-50">🔄 다른 후보 {Math.min(FIRST_PAGE, results.places.length - FIRST_PAGE)}곳 더 보기 (1회)</button>}
      {searchable && !isGuest && refreshed && <p className="mt-4 text-center text-xs text-slate-400">후보를 모두 보여 드렸어요. 게스트에게는 지금 보이는 {candidates.length}곳이 후보로 전달돼요.</p>}
    </section>}

    {placeMode === "OPEN" && <div className="rounded-[2rem] border border-dashed border-slate-300 bg-white p-10 text-center"><span className="text-4xl">⏭️</span><h2 className="mt-4 text-xl font-black">{guestProposalFlow ? "내 장소 의견을 건너뜁니다" : "장소 선택을 게스트 제안에 열어둡니다"}</h2><p className="mt-2 text-slate-500">{guestProposalFlow ? "다른 게스트가 제출한 장소 제안은 그대로 최종 집계에 반영됩니다." : "게스트는 장소를 제안하거나 본인 의견만 건너뛸 수 있습니다."}</p></div>}
    <div className="mt-8 flex gap-4"><button onClick={() => navigate(-1)} className="w-1/3 rounded-2xl border border-slate-200 bg-white py-4 font-bold">이전으로</button><button onClick={saveAndNext} className="w-2/3 rounded-2xl bg-blue-600 py-4 font-bold text-white">응답 확인하기</button></div>
  </div></div>;
}

function Field({ label, children }) { return <label><span className="mb-2 block text-sm font-bold">{label}</span>{children}</label>; }

export default MeetingPlacesPage;
