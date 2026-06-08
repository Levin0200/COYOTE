import { useNavigate } from "react-router-dom";
import { useState } from "react";

function MeetingResultPage() {
  const navigate = useNavigate();

  const [aiResult, setAiResult] = useState("");
  const [loading, setLoading] = useState(false);

  const draft = JSON.parse(sessionStorage.getItem("meetingDraft"));

  if (!draft) {
    return (
      <div className="min-h-screen bg-[#0B1020] text-white flex items-center justify-center">
        <div className="bg-white/10 border border-white/10 rounded-3xl p-8">
          <h1 className="text-2xl font-bold mb-4">
            저장된 모임 정보가 없습니다.
          </h1>

          <button
            onClick={() => navigate("/create-meeting")}
            className="bg-blue-500 px-6 py-3 rounded-2xl font-bold"
          >
            모임 생성으로 이동
          </button>
        </div>
      </div>
    );
  }

  const requestAiRecommendation = async () => {
    setLoading(true);

    try {
      const response = await fetch("http://localhost:8080/api/ai/meeting-report", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(draft),
      });

      const data = await response.text();
      setAiResult(data);
    } catch (error) {
      console.error(error);
      alert("AI 추천 요청 실패");
    } finally {
      setLoading(false);
    }
  };

  const createMeeting = async () => {
    const response = await fetch("http://localhost:8080/api/meetings", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        hostId: draft.hostId,
        title: draft.title,
        category: draft.category,
        description: draft.description,
      }),
    });

    const data = await response.json();

    sessionStorage.removeItem("meetingDraft");

    alert("모임 생성 완료!");

    navigate(`/meeting/${data.meetingId}`);
  };

  return (
    <div className="min-h-screen bg-[#0B1020] text-white px-6 py-10">
      <div className="max-w-6xl mx-auto">
        <header className="mb-10">
          <p className="text-sm text-blue-300 font-semibold mb-3">
            Final Check
          </p>

          <h1 className="text-5xl font-black mb-4">
            최종 확인
          </h1>

          <p className="text-slate-300">
            입력한 모임 정보를 확인하고 AI 추천을 받아보세요.
          </p>
        </header>

        <main className="grid lg:grid-cols-3 gap-6">
          <section className="lg:col-span-2 bg-white/10 border border-white/10 rounded-[2rem] p-8">
            <div className="flex items-center justify-between mb-8">
              <div>
                <p className="text-sm text-slate-400 mb-2">
                  모임 기본 정보
                </p>

                <h2 className="text-4xl font-black">
                  {draft.title}
                </h2>
              </div>

              <span className="text-sm bg-blue-500/20 text-blue-300 px-4 py-2 rounded-full">
                카테고리 : {draft.category}
              </span>
            </div>

            <div className="grid sm:grid-cols-2 gap-4 mb-6">
              <div className="bg-white/5 border border-white/10 rounded-2xl p-4">
                <p className="text-sm text-slate-400 mb-1">
                  카테고리
                </p>

                <p className="font-bold text-lg">
                  {draft.category}
                </p>
              </div>

              <div className="bg-white/5 border border-white/10 rounded-2xl p-4">
                <p className="text-sm text-slate-400 mb-1">
                  모임 생성자
                </p>

                <p className="font-bold text-lg">
                  사용자 #{draft.hostId}
                </p>
              </div>
            </div>

            <div className="bg-white/5 border border-white/10 rounded-2xl p-5">
              <p className="text-sm text-slate-400 mb-2">
                모임 설명
              </p>

              <p className="text-slate-100 leading-relaxed">
                {draft.description}
              </p>
            </div>
          </section>

          <section className="bg-blue-500 rounded-[2rem] p-8">
            <p className="text-blue-100 text-sm mb-3">
              선택 결과
            </p>

            <h2 className="text-2xl font-black mb-6">
              최종 선택
            </h2>

            {draft.votes?.map((vote, index) => (
              <div
                key={index}
                className="bg-white/15 rounded-2xl p-5"
              >
                <p className="text-sm text-blue-100 mb-2">
                  선택 시간
                </p>

                <p className="font-bold mb-4">
                  {vote.selectedTime}
                </p>

                <p className="text-sm text-blue-100 mb-2">
                  선택 장소
                </p>

                <p className="font-bold">
                  {vote.selectedPlace}
                </p>
              </div>
            ))}
          </section>

          <section className="bg-white/5 border border-white/10 rounded-[2rem] p-8">
            <h3 className="text-2xl font-bold mb-5">
              시간 후보
            </h3>

            <div className="space-y-3">
              {draft.timeOptions?.map((option, index) => (
                <div
                  key={index}
                  className="bg-white/5 border border-white/10 rounded-2xl p-4"
                >
                  <p className="font-bold">
                    {option.date}
                  </p>

                  <p className="text-blue-300">
                    {option.time}
                  </p>
                </div>
              ))}
            </div>
          </section>

          <section className="lg:col-span-2 bg-white/5 border border-white/10 rounded-[2rem] p-8">
            <h3 className="text-2xl font-bold mb-5">
              장소 후보
            </h3>

            <div className="grid sm:grid-cols-2 gap-3">
              {draft.places?.map((place, index) => (
                <div
                  key={index}
                  className="bg-white/5 border border-white/10 rounded-2xl p-4"
                >
                  {place}
                </div>
              ))}
            </div>
          </section>
        </main>

        <section className="mt-8 bg-white/10 border border-white/10 rounded-[2rem] p-8">
          <div className="flex items-center justify-between gap-4 mb-6">
            <div>
              <p className="text-sm text-purple-300 font-semibold mb-2">
                AI Meeting Report
              </p>
              <h2 className="text-3xl font-black">
                AI 조율 리포트
              </h2>
            </div>

            <button
              onClick={requestAiRecommendation}
              disabled={loading}
              className="bg-purple-500 hover:bg-purple-400 disabled:bg-purple-900 transition px-6 py-3 rounded-2xl font-bold"
            >
              {loading ? "AI 분석 중..." : "AI 추천 받기"}
            </button>
          </div>

          {aiResult ? (
            <pre className="whitespace-pre-wrap text-slate-200 bg-black/20 border border-white/10 rounded-2xl p-5 leading-relaxed">
              {aiResult}
            </pre>
          ) : (
            <p className="text-slate-400">
              AI 추천 받기를 누르면 시간 후보, 장소 후보, 투표 정보를 기반으로 조율 리포트를 생성합니다.
            </p>
          )}
        </section>

        <div className="mt-8 flex gap-4">
          <button
            onClick={() => navigate("/meeting/setup/vote")}
            className="w-1/3 bg-white/10 hover:bg-white/15 transition py-4 rounded-2xl font-bold border border-white/10"
          >
            이전으로
          </button>

          <button
            onClick={createMeeting}
            className="w-2/3 bg-blue-500 hover:bg-blue-400 transition py-4 rounded-2xl font-black shadow-lg shadow-blue-500/25"
          >
            최종 생성하기
          </button>
        </div>
      </div>
    </div>
  );
}

export default MeetingResultPage;