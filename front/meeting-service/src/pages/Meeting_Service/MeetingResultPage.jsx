import { useNavigate } from "react-router-dom";
import { useState } from "react";

function MeetingResultPage() {
  const navigate = useNavigate();

  const [aiResult, setAiResult] = useState("");
  const [loading, setLoading] = useState(false);

  const draft = JSON.parse(sessionStorage.getItem("meetingDraft"));

  if (!draft) {
    return (
      <div className="min-h-screen bg-slate-50 text-slate-800 flex items-center justify-center px-6">
        <div className="bg-white border border-slate-200 shadow-sm rounded-3xl p-8 max-w-md w-full text-center">
          <h1 className="text-2xl font-bold mb-4 text-slate-900">
            저장된 모임 정보가 없습니다.
          </h1>
          <button
            onClick={() => navigate("/create-meeting")}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-2xl font-bold transition shadow-sm"
          >
            모임 생성으로 이동
          </button>
        </div>
      </div>
    );
  }

  const requestAiRecommendation = async () => {
    setLoading(true);
    setAiResult("");

    try {
      const response = await fetch("http://localhost:8080/api/ai/meeting-report", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(draft),
      });

      const data = await response.text();

      if (!response.ok) {
        try {
          const errorJson = JSON.parse(data);
          setAiResult(
            errorJson.message ||
              "AI 추천 요청 중 오류가 발생했습니다."
          );
        } catch {
          setAiResult(data || "AI 추천 요청 중 오류가 발생했습니다.");
        }
        return;
      }

      setAiResult(data);
    } catch (error) {
      console.error(error);
      setAiResult("서버와 연결할 수 없습니다. 백엔드 서버가 실행 중인지 확인해주세요.");
    } finally {
      setLoading(false);
    }
  };

  const createMeeting = async () => {
    try {
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

      const data = await response.text();

      if (!response.ok) {
        try {
          const errorJson = JSON.parse(data);
          alert(errorJson.message || "모임 생성에 실패했습니다.");
        } catch {
          alert(data || "모임 생성에 실패했습니다.");
        }
        return;
      }

      const result = JSON.parse(data);

      sessionStorage.removeItem("meetingDraft");
      alert("모임 생성 완료!");
      navigate(`/meeting/${result.meetingId}`);
    } catch (error) {
      console.error(error);
      alert("서버와 연결할 수 없습니다. 백엔드 서버가 실행 중인지 확인해주세요.");
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 px-6 py-10">
      <div className="max-w-6xl mx-auto">
        <header className="mb-10">
          <p className="text-sm text-blue-600 font-semibold mb-3">
            Final Check
          </p>
          <h1 className="text-5xl font-black mb-4 text-slate-950">
            최종 확인
          </h1>
          <p className="text-slate-500">
            입력한 모임 정보를 확인하고 AI 추천을 받아보세요.
          </p>
        </header>

        <main className="grid lg:grid-cols-3 gap-6">
          <section className="lg:col-span-2 bg-white border border-slate-200 shadow-sm rounded-[2rem] p-8">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8">
              <div>
                <p className="text-sm text-slate-400 mb-2">
                  모임 기본 정보
                </p>
                <h2 className="text-4xl font-black text-slate-900">
                  {draft.title}
                </h2>
              </div>
              <span className="self-start sm:self-center text-sm bg-blue-50 text-blue-600 border border-blue-100 px-4 py-2 rounded-full font-medium">
                카테고리 : {draft.category}
              </span>
            </div>

            <div className="grid sm:grid-cols-2 gap-4 mb-6">
              <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4">
                <p className="text-sm text-slate-400 mb-1">카테고리</p>
                <p className="font-bold text-lg text-slate-800">
                  {draft.category}
                </p>
              </div>

              <div className="bg-slate-50 border border-slate-100 rounded-2xl p-4">
                <p className="text-sm text-slate-400 mb-1">모임 생성자</p>
                <p className="font-bold text-lg text-slate-800">
                  사용자 #{draft.hostId}
                </p>
              </div>
            </div>

            <div className="bg-slate-50 border border-slate-100 rounded-2xl p-5">
              <p className="text-sm text-slate-400 mb-2">모임 설명</p>
              <p className="text-slate-700 leading-relaxed">
                {draft.description}
              </p>
            </div>
          </section>

          <section className="bg-blue-600 text-white rounded-[2rem] p-8 shadow-md">
            <p className="text-blue-200 text-sm mb-3 font-medium">선택 결과</p>
            <h2 className="text-2xl font-black mb-6">최종 선택</h2>

            {draft.votes?.length === 0 || !draft.votes ? (
              <p className="text-blue-200 text-sm">
                선택된 투표 정보가 없습니다.
              </p>
            ) : (
              draft.votes.map((vote, index) => (
                <div
                  key={index}
                  className="bg-white/10 rounded-2xl p-5 border border-white/10"
                >
                  <p className="text-sm text-blue-100 mb-2">선택 시간</p>
                  <p className="font-bold text-lg mb-4 text-white">
                    {vote.selectedTime}
                  </p>
                  <p className="text-sm text-blue-100 mb-2">선택 장소</p>
                  <p className="font-bold text-lg text-white">
                    {vote.selectedPlace}
                  </p>
                </div>
              ))
            )}
          </section>

          <section className="bg-white border border-slate-200 shadow-sm rounded-[2rem] p-8">
            <h3 className="text-2xl font-bold mb-5 text-slate-900">
              시간 후보
            </h3>
            <div className="space-y-3">
              {draft.timeOptions?.map((option, index) => (
                <div
                  key={index}
                  className="bg-slate-50 border border-slate-100 rounded-2xl p-4"
                >
                  <p className="font-bold text-slate-800">{option.date}</p>
                  <p className="text-blue-600 font-medium">{option.time}</p>
                </div>
              ))}
            </div>
          </section>

          <section className="lg:col-span-2 bg-white border border-slate-200 shadow-sm rounded-[2rem] p-8">
            <h3 className="text-2xl font-bold mb-5 text-slate-900">
              장소 후보
            </h3>
            <div className="grid sm:grid-cols-2 gap-3">
              {draft.places?.map((place, index) => (
                <div
                  key={index}
                  className="bg-slate-50 border border-slate-100 rounded-2xl p-4 text-slate-800 font-medium"
                >
                  {place}
                </div>
              ))}
            </div>
          </section>
        </main>

        <section className="mt-8 bg-white border border-slate-200 shadow-sm rounded-[2rem] p-8">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
            <div>
              <p className="text-sm text-purple-600 font-semibold mb-2">
                AI Meeting Report
              </p>
              <h2 className="text-3xl font-black text-slate-900">
                AI 조율 리포트
              </h2>
            </div>

            <button
              onClick={requestAiRecommendation}
              disabled={loading}
              className="bg-purple-600 hover:bg-purple-700 disabled:bg-purple-200 disabled:text-purple-400 text-white transition px-6 py-3 rounded-2xl font-bold whitespace-nowrap shadow-sm"
            >
              {loading ? "AI 분석 중..." : "AI 추천 받기"}
            </button>
          </div>

          {aiResult ? (
            <pre className="whitespace-pre-wrap text-slate-700 bg-slate-50 border border-slate-200 rounded-2xl p-5 leading-relaxed font-sans">
              {aiResult}
            </pre>
          ) : (
            <p className="text-slate-400 text-sm">
              AI 추천 받기를 누르면 시간 후보, 장소 후보, 투표 정보를 기반으로 조율 리포트를 생성합니다.
            </p>
          )}
        </section>

        <div className="mt-8 flex gap-4">
          <button
            onClick={() => navigate("/meeting/setup/vote")}
            className="w-1/3 bg-slate-100 hover:bg-slate-200 text-slate-800 transition py-4 rounded-2xl font-bold border border-slate-200"
          >
            이전으로
          </button>

          <button
            onClick={createMeeting}
            className="w-2/3 bg-blue-600 hover:bg-blue-700 text-white transition py-4 rounded-2xl font-black shadow-md shadow-blue-600/10"
          >
            최종 생성하기
          </button>
        </div>
      </div>
    </div>
  );
}

export default MeetingResultPage;