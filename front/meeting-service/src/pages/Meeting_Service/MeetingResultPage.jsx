import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { FiCalendar, FiCheck, FiMapPin, FiStar, FiUsers } from "react-icons/fi";

const MEETINGS_STORAGE_KEY = "coyoteMeetings";
const AI_REPORTS_STORAGE_KEY = "coyoteAiReports";

const readStorageList = (key) => {
  try {
    const value = JSON.parse(localStorage.getItem(key) || "[]");
    return Array.isArray(value) ? value : [];
  } catch {
    return [];
  }
};

const createAiRecommendation = (draft) => {
  const votedTime = draft.votes?.[0]?.selectedTime;
  const votedPlace = draft.votes?.[0]?.selectedPlace;
  const times = (draft.timeOptions || []).map(({ date, time }) => `${date} ${time}`);
  const places = draft.places || [];
  const combinations = [];

  const addCandidate = (schedule, place, reason) => {
    if (!schedule || !place) return;
    if (combinations.some((item) => item.schedule === schedule && item.place === place)) return;

    combinations.push({
      id: `recommendation-${combinations.length + 1}`,
      rank: combinations.length + 1,
      schedule,
      place,
      fit: Math.max(80, 96 - combinations.length * 7),
      reason,
    });
  };

  addCandidate(votedTime, votedPlace, "참여자의 최종 선택을 모두 반영한 가장 안정적인 조합이에요.");

  for (let index = 0; index < Math.max(times.length, places.length) && combinations.length < 3; index += 1) {
    addCandidate(
      times[index % Math.max(times.length, 1)],
      places[index % Math.max(places.length, 1)],
      index === 0
        ? "등록 순서와 현재 선호도를 함께 고려한 균형 잡힌 조합이에요."
        : "다른 후보를 선호할 때 선택할 수 있는 대안 조합이에요.",
    );
  }

  for (const schedule of times) {
    for (const place of places) {
      if (combinations.length >= 3) break;
      addCandidate(schedule, place, "일정과 장소 후보를 교차 분석해 만든 대안 조합이에요.");
    }
  }

  return {
    recommendations: combinations,
    summary: `${draft.category || "모임"}의 목적과 현재 후보를 바탕으로 가장 적합한 조합부터 정리했어요. 원하는 카드를 선택해 주세요.`,
  };
};

function MeetingResultPage() {
  const navigate = useNavigate();
  const draft = useMemo(() => {
    try {
      return JSON.parse(sessionStorage.getItem("meetingDraft"));
    } catch {
      return null;
    }
  }, []);
  const [aiResult, setAiResult] = useState(null);
  const [selectedRecommendationId, setSelectedRecommendationId] = useState(null);
  const [loading, setLoading] = useState(false);

  if (!draft) {
    return (
      <div className="min-h-screen bg-slate-50 text-slate-800 flex items-center justify-center px-6">
        <div className="bg-white border border-slate-200 shadow-sm rounded-3xl p-8 max-w-md w-full text-center">
          <h1 className="text-2xl font-bold mb-4 text-slate-900">저장된 모임 정보가 없습니다.</h1>
          <button onClick={() => navigate("/create-meeting")} className="w-full bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-2xl font-bold transition shadow-sm">
            모임 생성으로 이동
          </button>
        </div>
      </div>
    );
  }

  const requestAiRecommendation = () => {
    setLoading(true);

    // TODO: 서버 개발 이후 이 부분만 LLM API 요청으로 교체하면 됩니다.
    window.setTimeout(() => {
      const recommendation = createAiRecommendation(draft);
      setAiResult(recommendation);
      setSelectedRecommendationId(recommendation.recommendations[0]?.id || null);
      localStorage.setItem(
        AI_REPORTS_STORAGE_KEY,
        JSON.stringify([
          ...readStorageList(AI_REPORTS_STORAGE_KEY),
          { meetingTitle: draft.title, createdAt: new Date().toISOString(), ...recommendation },
        ]),
      );
      setLoading(false);
    }, 650);
  };

  const createMeeting = () => {
    const meetings = readStorageList(MEETINGS_STORAGE_KEY);
    const meetingId = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${meetings.length + 1}`;
    const newMeeting = {
      ...draft,
      meetingId,
      aiRecommendation: aiResult,
      selectedRecommendation: aiResult?.recommendations.find(
        ({ id }) => id === selectedRecommendationId,
      ) || null,
      createdAt: new Date().toISOString(),
    };

    localStorage.setItem(MEETINGS_STORAGE_KEY, JSON.stringify([...meetings, newMeeting]));
    sessionStorage.removeItem("meetingDraft");
    alert("모임이 기기에 저장되었습니다.");
    navigate(`/meeting/${meetingId}`);
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 px-6 py-10">
      <div className="max-w-6xl mx-auto">
        <header className="mb-10">
          <p className="text-sm text-blue-600 font-semibold mb-3">Final Check</p>
          <h1 className="text-5xl font-black mb-4 text-slate-950">최종 확인</h1>
          <p className="text-slate-500">입력한 모임 정보를 확인하고 AI 추천을 받아보세요.</p>
        </header>

        <main className="grid lg:grid-cols-3 gap-6">
          <section className="lg:col-span-2 bg-white border border-slate-200 shadow-sm rounded-[2rem] p-8">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
              <div>
                <p className="text-sm text-slate-400 mb-2">모임 기본 정보</p>
                <h2 className="text-4xl font-black text-slate-900">{draft.title}</h2>
              </div>
              <span className="self-start sm:self-center text-sm bg-blue-50 text-blue-600 border border-blue-100 px-4 py-2 rounded-full font-medium">
                카테고리 : {draft.category || "미지정"}
              </span>
            </div>

            <div className="grid sm:grid-cols-2 gap-4 mb-6">
              <InfoBox label="카테고리" value={draft.category || "미지정"} />
              <InfoBox label="모임 생성자" value={`사용자 #${draft.hostId}`} />
            </div>
            <div className="bg-slate-50 border border-slate-100 rounded-2xl p-5">
              <p className="text-sm text-slate-400 mb-2">모임 설명</p>
              <p className="text-slate-700 leading-relaxed">{draft.description || "작성된 설명이 없습니다."}</p>
            </div>
          </section>

          <section className="bg-blue-600 text-white rounded-[2rem] p-8 shadow-md">
            <p className="text-blue-200 text-sm mb-3 font-medium">선택 결과</p>
            <h2 className="text-2xl font-black mb-6">최종 선택</h2>
            {!draft.votes?.length ? (
              <p className="text-blue-200 text-sm">선택된 투표 정보가 없습니다.</p>
            ) : (
              draft.votes.map((vote, index) => (
                <div key={index} className="bg-white/10 rounded-2xl p-5 border border-white/10">
                  <p className="text-sm text-blue-100 mb-2">선택 시간</p>
                  <p className="font-bold text-lg mb-4 text-white">{vote.selectedTime}</p>
                  <p className="text-sm text-blue-100 mb-2">선택 장소</p>
                  <p className="font-bold text-lg text-white">{vote.selectedPlace}</p>
                </div>
              ))
            )}
          </section>

          <section className="bg-white border border-slate-200 shadow-sm rounded-[2rem] p-8">
            <h3 className="text-2xl font-bold mb-5 text-slate-900">시간 후보</h3>
            <div className="space-y-3">
              {draft.timeOptions?.map((option, index) => (
                <div key={index} className="bg-slate-50 border border-slate-100 rounded-2xl p-4">
                  <p className="font-bold text-slate-800">{option.date}</p>
                  <p className="text-blue-600 font-medium">{option.time}</p>
                </div>
              ))}
            </div>
          </section>

          <section className="lg:col-span-2 bg-white border border-slate-200 shadow-sm rounded-[2rem] p-8">
            <h3 className="text-2xl font-bold mb-5 text-slate-900">장소 후보</h3>
            <div className="grid sm:grid-cols-2 gap-3">
              {draft.places?.map((place, index) => (
                <div key={index} className="bg-slate-50 border border-slate-100 rounded-2xl p-4 text-slate-800 font-medium">{place}</div>
              ))}
            </div>
          </section>
        </main>

        <section className="mt-8 overflow-hidden bg-gradient-to-br from-white via-purple-50/40 to-blue-50 border border-purple-100 shadow-sm rounded-[2rem] p-8">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
            <div>
              <p className="flex items-center gap-2 text-sm text-purple-600 font-semibold mb-2"><FiStar /> AI Meeting Report</p>
              <h2 className="text-3xl font-black text-slate-900">AI 조율 리포트</h2>
            </div>
            <button onClick={requestAiRecommendation} disabled={loading} className="bg-purple-600 hover:bg-purple-700 disabled:bg-purple-200 disabled:text-purple-400 text-white transition px-6 py-3 rounded-2xl font-bold whitespace-nowrap shadow-sm">
              {loading ? "AI 분석 중..." : aiResult ? "다시 추천받기" : "AI 추천받기"}
            </button>
          </div>

          {aiResult ? (
            <div aria-live="polite">
              <p className="text-slate-600 mb-5 leading-relaxed">{aiResult.summary}</p>
              <div className="grid lg:grid-cols-3 gap-4">
                {aiResult.recommendations.map((recommendation) => (
                  <RecommendationCard
                    key={recommendation.id}
                    recommendation={recommendation}
                    meeting={draft}
                    selected={selectedRecommendationId === recommendation.id}
                    onSelect={() => setSelectedRecommendationId(recommendation.id)}
                  />
                ))}
              </div>
              <p className="mt-4 text-xs text-slate-400">
                현재는 등록된 후보와 투표 결과를 이용한 임시 추천입니다. 서버 연결 후 실제 AI 분석으로 전환됩니다.
              </p>
            </div>
          ) : (
            <div className="border border-dashed border-purple-200 bg-white/70 rounded-2xl p-6 text-center">
              <FiStar className="mx-auto mb-3 text-2xl text-purple-300" />
              <p className="text-slate-500 text-sm">AI 추천받기를 누르면 시간 후보, 장소 후보, 투표 정보를 바탕으로 추천 카드를 만들어요.</p>
            </div>
          )}
        </section>

        <div className="mt-8 flex gap-4">
          <button onClick={() => navigate("/meeting/setup/vote")} className="w-1/3 bg-slate-100 hover:bg-slate-200 text-slate-800 transition py-4 rounded-2xl font-bold border border-slate-200">이전으로</button>
          <button onClick={createMeeting} className="w-2/3 bg-blue-600 hover:bg-blue-700 text-white transition py-4 rounded-2xl font-black shadow-md shadow-blue-600/10">최종 생성하기</button>
        </div>
      </div>
    </div>
  );
}

function InfoBox({ label, value }) {
  return (
    <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4">
      <p className="text-sm text-slate-400 mb-1">{label}</p>
      <p className="font-bold text-lg text-slate-800">{value}</p>
    </div>
  );
}

function RecommendationCard({ recommendation, meeting, selected, onSelect }) {
  return (
    <button
      type="button"
      onClick={onSelect}
      aria-pressed={selected}
      className={`group relative overflow-hidden text-left rounded-3xl border-2 transition-all duration-200 ${
        selected
          ? "border-purple-500 bg-white shadow-lg shadow-purple-100 -translate-y-1"
          : "border-transparent bg-white shadow-sm hover:border-purple-200 hover:-translate-y-1"
      }`}
    >
      <div className="relative h-32 overflow-hidden bg-gradient-to-br from-slate-950 via-purple-950 to-blue-900 p-5 text-white">
        <div className="absolute -right-8 -top-10 h-32 w-32 rounded-full bg-purple-400/20 blur-xl" />
        <div className="absolute bottom-3 right-5 text-6xl text-white/10"><FiMapPin /></div>
        <div className="relative flex items-start justify-between">
          <span className="rounded-full bg-white/15 px-3 py-1 text-xs font-bold backdrop-blur-sm">
            {recommendation.rank === 1 ? "BEST PICK" : `${recommendation.rank}순위 추천`}
          </span>
          <span className="text-2xl font-black">{recommendation.fit}<small className="text-xs font-semibold text-white/60">%</small></span>
        </div>
        <div className="relative mt-5">
          <p className="text-xs text-white/60">{meeting.category || "모임"}</p>
          <p className="mt-1 truncate font-bold">{meeting.title}</p>
        </div>
      </div>

      <div className="p-5">
        <div className="mb-4 flex items-center justify-between">
          <span className="text-xs font-bold text-purple-600">적합도 {recommendation.fit}%</span>
          <span className={`flex h-7 w-7 items-center justify-center rounded-full border transition ${selected ? "border-purple-600 bg-purple-600 text-white" : "border-slate-200 text-transparent"}`}>
            <FiCheck />
          </span>
        </div>

        <div className="space-y-3">
          <div className="flex items-start gap-3">
            <span className="mt-0.5 rounded-lg bg-purple-50 p-2 text-purple-600"><FiCalendar /></span>
            <div>
              <p className="text-xs text-slate-400">추천 일정</p>
              <p className="mt-0.5 font-bold text-slate-900">{recommendation.schedule}</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <span className="mt-0.5 rounded-lg bg-blue-50 p-2 text-blue-600"><FiMapPin /></span>
            <div>
              <p className="text-xs text-slate-400">추천 장소</p>
              <p className="mt-0.5 font-bold text-slate-900">{recommendation.place}</p>
            </div>
          </div>
        </div>

        <div className="my-4 h-px bg-slate-100" />
        <p className="min-h-10 text-xs leading-relaxed text-slate-500">{recommendation.reason}</p>
        <div className="mt-4 flex items-center gap-2 text-xs font-medium text-slate-400">
          <FiUsers /> 투표와 후보 정보를 함께 반영
        </div>
      </div>
    </button>
  );
}

export default MeetingResultPage;
