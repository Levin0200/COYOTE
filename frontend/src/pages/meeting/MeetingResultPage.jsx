import { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { FiCalendar, FiCheck, FiClock, FiHome, FiMapPin, FiShare2, FiStar, FiUsers } from "react-icons/fi";
import KakaoMap from "../../components/KakaoMap";
import PlaceCandidateList from "../../components/place/PlaceCandidateList";
import { centerOf, hasCoordinates } from "../../utils/placeCandidates";
import { PLACE_STATUS, decisionLabel, placeStatusOf, remainingText } from "../../utils/placeStatus";
import { formatSelectedDate } from "../../utils/dateFormat";
import { getErrorCode } from "../../services/api";
import { getMeetingErrorMessage } from "../../utils/meetingErrors";
import { choosePlace, fetchGuestResult, fetchHostMeeting, fetchHostResult, fetchInviteInfo } from "../../services/meetingService";

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
  const [pendingExpired, setPendingExpired] = useState(false);
  const pollTicks = useRef(0);

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
  const hasResult = Boolean(result);
  useEffect(() => {
    if (!hasResult) return undefined;
    let seen = false;
    try { seen = localStorage.getItem(`coyoteResultSeen_${seenId}`) === "1"; } catch { /* 저장소를 쓸 수 없으면 매번 재생한다 */ }
    const progressId = seen ? null : window.setInterval(() => setProgress((value) => Math.min(value + 16, 92)), 250);
    const revealId = window.setTimeout(() => {
      setProgress(100);
      setRevealed(true);
      try { localStorage.setItem(`coyoteResultSeen_${seenId}`, "1"); } catch { /* 저장소를 쓸 수 없으면 기록하지 않는다 */ }
    }, seen ? 0 : 1600);
    return () => { if (progressId) window.clearInterval(progressId); window.clearTimeout(revealId); };
  }, [hasResult, seenId]);

  const placeStatus = result ? placeStatusOf(result) : null;
  const pollMs = placeStatus === PLACE_STATUS.PENDING ? 2500 : placeStatus === PLACE_STATUS.AWAITING_HOST && isGuest ? 30000 : 0;
  useEffect(() => {
    if (!pollMs) return undefined;
    pollTicks.current = 0;
    const id = window.setInterval(async () => {
      pollTicks.current += 1;
      if (pollMs === 2500 && pollTicks.current > 24) {
        window.clearInterval(id);
        setPendingExpired(true);
        return;
      }
      try {
        setResult(await (isGuest ? fetchGuestResult(inviteToken) : fetchHostResult(meetingId)));
      } catch { /* 다음 주기에 다시 시도한다 */ }
    }, pollMs);
    return () => window.clearInterval(id);
  }, [pollMs, isGuest, inviteToken, meetingId, reloadKey]);

  const backToStatus = () => navigate(isGuest ? `/invite/${inviteToken}/status` : `/meeting/${meetingId}/status`);
  const backToHome = () => navigate("/main");
  const reload = () => { setPendingExpired(false); setLoading(true); setLoadError(null); setReloadKey((value) => value + 1); };

  if (loading) return <div className="flex min-h-screen items-center justify-center bg-slate-50"><p className="font-bold text-slate-400">불러오는 중...</p></div>;
  if (loadError?.code === "INVITE_EXPIRED") return <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6"><div className="max-w-md rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">초대 링크가 만료되었습니다.</h1><p className="mt-3 text-slate-500">게스트는 더 이상 이 모임에 접근할 수 없습니다.</p></div></div>;
  if (loadError?.code === "MEETING_NOT_CLOSED" || loadError?.code === "RESULT_NOT_READY") return <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6"><div className="max-w-md rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">{loadError.code === "MEETING_NOT_CLOSED" ? "아직 결과가 없습니다." : "결과를 준비하고 있습니다."}</h1><p className="mt-3 text-slate-500">{loadError.message}</p><div className="mt-5 flex justify-center gap-3">{loadError.code === "RESULT_NOT_READY" && <button onClick={reload} className="rounded-xl border border-slate-200 bg-white px-5 py-3 font-bold hover:bg-slate-100">다시 불러오기</button>}<button onClick={backToStatus} className="rounded-xl bg-blue-600 px-5 py-3 font-bold text-white">진행 상태로 돌아가기</button></div></div></div>;
  if (!meeting || !result) return <div className="flex min-h-screen items-center justify-center bg-slate-50"><div className="rounded-3xl bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">모임을 찾을 수 없습니다.</h1>{loadError && loadError.code !== "MEETING_NOT_FOUND" && <p className="mt-3 text-slate-500">{loadError.message}</p>}</div></div>;

  const completed = revealed && placeStatus !== PLACE_STATUS.PENDING;
  const linkExpired = Boolean(meeting.linkExpirationDate && now >= new Date(meeting.linkExpirationDate).getTime());
  // 호스트는 로그인이 필요한 호스트 전용 주소 대신 게스트가 열 수 있는 결과 주소를 복사한다.
  const copyResultLink = async () => { const url = isGuest ? window.location.href : `${window.location.origin}/invite/${meeting.inviteToken}/result`; try { await navigator.clipboard.writeText(url); alert("결과 링크를 복사했습니다."); } catch { window.prompt("결과 링크를 복사해 주세요.", url); } };
  const recommendAgain = () => {
    if (isGuest) return;
    if (linkExpired) return alert("링크가 만료되어 더 이상 재추천할 수 없습니다.");
    alert("다시 추천 기능은 준비 중입니다.");
  };

  const decided = placeStatus === PLACE_STATUS.DECIDED;
  const awaitingHost = placeStatus === PLACE_STATUS.AWAITING_HOST;
  const placeLabel = decided ? "최종 장소" : "장소";
  const placeValue = decided ? result.selectedPlace : awaitingHost ? "호스트가 고르는 중" : "추후 협의";
  const heading = !completed ? "결과 분석 중" : decided || placeStatus === PLACE_STATUS.NONE ? "최종 결과" : "날짜가 정해졌어요";

  return (
    <div className="min-h-screen bg-slate-50 px-6 py-10 text-slate-800">
      <div className="mx-auto max-w-5xl">
        <header className="mb-10">
          <p className="mb-3 flex items-center gap-2 text-sm font-semibold text-purple-600"><FiStar /> AI Meeting Report</p>
          <h1 className="mb-4 text-5xl font-black text-slate-950">{heading}</h1>
          <p className="text-slate-500">{meeting.title}</p>
        </header>
        {!completed ? (
          <section className="overflow-hidden rounded-[2rem] border border-purple-100 bg-gradient-to-br from-white via-purple-50/50 to-blue-50 p-10 text-center shadow-sm">
            <div className="mx-auto flex h-20 w-20 animate-pulse items-center justify-center rounded-3xl bg-purple-100 text-4xl">✨</div>
            <h2 className="mt-6 text-3xl font-black text-slate-900">응답이 마감되었습니다</h2>
            <p className="mt-4 leading-7 text-slate-600">참여자 응답을 취합하고 있습니다.<br />{placeStatus === PLACE_STATUS.PENDING ? "제안된 장소 주변에서 후보를 고르고 있어요." : "최종 날짜와 장소를 분석하고 있어요."}</p>
            <div className="mx-auto mt-8 max-w-xl">
              <div className="mb-2 flex justify-between text-xs font-bold text-purple-600"><span>분석 중</span>{placeStatus !== PLACE_STATUS.PENDING && <span>{progress}%</span>}</div>
              <div className="h-3 overflow-hidden rounded-full bg-purple-100"><div className={`h-full rounded-full bg-gradient-to-r from-purple-500 to-blue-500 transition-all duration-300 ${placeStatus === PLACE_STATUS.PENDING ? "w-full animate-pulse" : ""}`} style={placeStatus === PLACE_STATUS.PENDING ? undefined : { width: `${progress}%` }} /></div>
            </div>
            {pendingExpired && <div className="mt-6"><p className="text-sm text-slate-500">후보를 만드는 데 시간이 걸리고 있습니다.</p><button onClick={reload} className="mt-3 rounded-xl border border-slate-200 bg-white px-5 py-3 font-bold hover:bg-slate-100">다시 불러오기</button></div>}
          </section>
        ) : (
          <>
            <section className="overflow-hidden rounded-[2rem] border border-purple-100 bg-gradient-to-br from-white via-purple-50/40 to-blue-50 p-8 shadow-sm">
              <div className="mb-7 flex items-center justify-between">
                <div>
                  <span className="rounded-full bg-purple-600 px-4 py-2 text-xs font-black text-white">BEST PICK · {result.generation || 1}차</span>
                  <h2 className="mt-4 text-3xl font-black text-slate-950">{decided || placeStatus === PLACE_STATUS.NONE ? "모임 최종 결정" : "날짜 확정 · 장소 선택 대기"}</h2>
                </div>
                <span className="flex h-12 w-12 items-center justify-center rounded-full bg-white text-xl text-emerald-500 shadow-sm"><FiCheck /></span>
              </div>
              <div className="grid gap-5 md:grid-cols-2">
                <ResultCard icon={<FiCalendar />} label="최종 일정" value={result.selectedDate.includes("-") ? formatSelectedDate(result.selectedDate) : result.selectedDate} />
                <ResultCard icon={<FiMapPin />} label={placeLabel} value={placeValue} />
              </div>
              {decided && <PlaceDetails places={result.selectedPlaceDetails} />}
              <div className="mt-5 rounded-2xl border border-white bg-white/80 p-6">
                <p className="text-sm font-bold text-purple-600">선정 이유 · {decisionLabel(result.decisionType)}</p>
                <p className="mt-3 leading-7 text-slate-600">{result.reason}</p>
                <p className="mt-4 flex items-center gap-2 text-xs font-semibold text-slate-400"><FiUsers /> 총 {result.totalResponses}명 응답 · 날짜 {result.dateVoteCount}표{decided ? ` · 장소 ${result.placeVoteCount}표` : ""}</p>
              </div>
            </section>
            {awaitingHost && (isGuest
              ? <GuestWaiting deadline={result.hostDecisionDeadline} now={now} />
              : <HostPlacePicker meetingId={meetingId} result={result} now={now} onDecided={setResult} onReload={reload} />)}
            {!isGuest && (
              <div className="mt-6 grid gap-3 sm:grid-cols-2">
                <button onClick={copyResultLink} className="flex items-center justify-center gap-2 rounded-2xl bg-blue-600 py-4 font-black text-white shadow-sm transition hover:bg-blue-700"><FiShare2 /> 결과 링크 복사</button>
                <button onClick={recommendAgain} disabled={linkExpired} className="rounded-2xl bg-purple-600 py-4 font-black text-white shadow-sm transition hover:bg-purple-700 disabled:cursor-not-allowed disabled:bg-slate-300">{linkExpired ? "링크 만료 · 재추천 불가" : "이전 응답으로 다시 추천"}</button>
              </div>
            )}
          </>
        )}
        {!isGuest && (
          <button onClick={backToHome} className="mt-3 flex w-full items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white py-4 font-bold text-slate-700 transition hover:border-slate-300 hover:bg-slate-100"><FiHome /> 홈으로</button>
        )}
      </div>
    </div>
  );
}

/** 호스트가 후보 중에서 최종 장소를 고르는 패널. 확정은 되돌릴 수 없어서 한 번 더 확인한다. */
function HostPlacePicker({ meetingId, result, now, onDecided, onReload }) {
  const candidates = (result.placeCandidates || []).map((candidate) => ({
    key: candidate.id || candidate.name,
    placeId: candidate.id || "",
    name: candidate.name,
    category: candidate.category || "",
    address: candidate.address || "",
    latitude: candidate.latitude,
    longitude: candidate.longitude,
    distanceMeters: null,
    distanceLabel: candidate.votes > 0 ? `${candidate.votes}표` : "",
    reason: candidate.reason || "",
  }));
  const [selectedKey, setSelectedKey] = useState(null);
  const [confirming, setConfirming] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const selected = candidates.find((candidate) => candidate.key === selectedKey) || null;
  const located = candidates.filter(hasCoordinates);
  const center = centerOf(candidates);
  const remaining = remainingText(result.hostDecisionDeadline, now);

  const select = (key) => { setSelectedKey(key); setConfirming(false); setError(""); };
  const confirm = async () => {
    if (!selected) return;
    setSaving(true);
    setError("");
    try {
      onDecided(await choosePlace(meetingId, selected.placeId ? { placeId: selected.placeId } : { name: selected.name }));
    } catch (e) {
      setError(getMeetingErrorMessage(e));
      setConfirming(false);
      if (getErrorCode(e) === "PLACE_NOT_AWAITING") onReload();
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="mt-6 rounded-[2rem] border border-blue-100 bg-white p-8 shadow-sm">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-2xl font-black text-slate-950">최종 장소를 골라 주세요</h2>
          <p className="mt-1 text-sm text-slate-500">후보 중에서 하나를 고르면 모든 참여자에게 확정 장소로 보입니다.</p>
        </div>
        {remaining && <span className="flex items-center gap-2 rounded-full bg-amber-50 px-4 py-2 text-sm font-bold text-amber-700"><FiClock /> {remaining}</span>}
      </div>
      <p className="mb-5 text-xs text-slate-400">시간 안에 고르지 않으면 첫 번째 후보가 자동으로 선정됩니다.</p>
      {center && <div className="mb-5"><KakaoMap center={center} markers={located.map((place) => ({ id: place.key, name: place.name, lat: place.latitude, lng: place.longitude }))} selectedId={selectedKey} onSelectMarker={select} height={300} /></div>}
      <PlaceCandidateList candidates={candidates} selectedKey={selectedKey} onSelect={select} disabled={saving} />
      {error && <p role="alert" className="mt-4 text-sm font-bold text-rose-600">{error}</p>}
      {confirming && selected ? (
        <div className="mt-5 rounded-2xl border border-blue-100 bg-blue-50 p-5">
          <p className="font-bold text-slate-800">‘{selected.name}’(으)로 확정할까요?</p>
          <p className="mt-1 text-sm text-slate-500">확정하면 바꿀 수 없습니다.</p>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <button type="button" onClick={confirm} disabled={saving} className="rounded-2xl bg-blue-600 py-3 font-black text-white transition hover:bg-blue-700 disabled:bg-slate-300">{saving ? "확정하는 중..." : "확정"}</button>
            <button type="button" onClick={() => setConfirming(false)} disabled={saving} className="rounded-2xl border border-slate-200 bg-white py-3 font-bold text-slate-700 hover:bg-slate-100">취소</button>
          </div>
        </div>
      ) : (
        <button type="button" onClick={() => setConfirming(true)} disabled={!selected} className="mt-5 w-full rounded-2xl bg-blue-600 py-4 font-black text-white shadow-sm transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-slate-300">{selected ? "이 장소로 확정" : "후보를 선택해 주세요"}</button>
      )}
    </section>
  );
}

/** 게스트가 호스트의 장소 선택을 기다리는 안내. 결과 화면이 30초마다 새로고침되어 확정되면 바로 바뀐다. */
function GuestWaiting({ deadline, now }) {
  const remaining = remainingText(deadline, now);
  return (
    <section className="mt-6 rounded-[2rem] border border-blue-100 bg-white p-8 text-center shadow-sm">
      <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-blue-50 text-3xl">📍</div>
      <h2 className="mt-4 text-2xl font-black text-slate-950">호스트가 장소를 정하고 있어요</h2>
      <p className="mt-2 text-sm text-slate-500">정해지면 이 화면에 자동으로 나타납니다.</p>
      {remaining && <p className="mt-4 inline-flex items-center gap-2 rounded-full bg-amber-50 px-4 py-2 text-sm font-bold text-amber-700"><FiClock /> {remaining}</p>}
    </section>
  );
}

function PlaceDetails({ places }) {
  const details = places || [];
  if (details.length === 0) return null;
  const located = details.map((place) => ({ ...place, lat: place.latitude, lng: place.longitude })).filter((place) => hasCoordinates({ latitude: place.lat, longitude: place.lng }));
  const center = centerOf(located.map((place) => ({ latitude: place.lat, longitude: place.lng })));
  return (
    <div className="mt-5 space-y-3">
      {details.map((place) => <p key={place.id || place.name} className="rounded-2xl bg-white/80 px-5 py-3 text-sm text-slate-600"><span className="font-black text-slate-900">{place.name}</span>{place.address && <span className="ml-2 text-slate-500">{place.address}</span>}</p>)}
      {center && <KakaoMap center={center} markers={located.map((place) => ({ id: place.id || place.name, name: place.name, lat: place.lat, lng: place.lng }))} height={260} />}
    </div>
  );
}

function ResultCard({ icon, label, value }) { return <div className="rounded-3xl bg-white p-6 shadow-sm"><span className="flex h-11 w-11 items-center justify-center rounded-xl bg-purple-50 text-xl text-purple-600">{icon}</span><p className="mt-5 text-sm font-bold text-slate-400">{label}</p><p className="mt-2 text-2xl font-black text-slate-900">{value}</p></div>; }

export default MeetingResultPage;
