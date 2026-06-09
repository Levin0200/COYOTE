import { useState } from "react";
import { useNavigate } from "react-router-dom";

function MeetingVotePage() {
  const navigate = useNavigate();

  const draft = JSON.parse(sessionStorage.getItem("meetingDraft"));

  const timeOptions = draft?.timeOptions || [];
  const places = draft?.places || [];

  const [selectedTime, setSelectedTime] = useState("");
  const [selectedPlace, setSelectedPlace] = useState("");

  const saveVoteAndNext = () => {
    if (!selectedTime || !selectedPlace) {
      alert("시간과 장소를 선택해주세요.");
      return;
    }

    // draft 객체가 없을 경우를 대비한 안전한 할당 구조
    const updatedDraft = draft ? { ...draft } : {};
    updatedDraft.votes = [
      {
        selectedTime,
        selectedPlace,
      },
    ];

    sessionStorage.setItem("meetingDraft", JSON.stringify(updatedDraft));

    navigate("/meeting/setup/result");
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 px-6 py-10">
      <div className="max-w-5xl mx-auto">
        <header className="mb-10">
          <p className="text-sm text-blue-600 font-semibold mb-3">
            Vote
          </p>

          <h1 className="text-4xl font-black mb-3 text-slate-950">
            모임 투표
          </h1>

          <p className="text-slate-500">
            원하는 시간과 장소를 선택해주세요.
          </p>
        </header>

        <main className="grid lg:grid-cols-2 gap-8">
          {/* 시간 선택 섹션 */}
          <section className="bg-white border border-slate-200 shadow-sm rounded-[2rem] p-8">
            <h2 className="text-xl font-bold mb-5 text-slate-900">시간 선택</h2>

            <div className="space-y-3">
              {timeOptions.length === 0 ? (
                <p className="text-slate-400 text-sm">등록된 시간 후보가 없습니다.</p>
              ) : (
                timeOptions.map((option, index) => {
                  const value = `${option.date} ${option.time}`;
                  const isSelected = selectedTime === value;

                  return (
                    <button
                      key={index}
                      onClick={() => setSelectedTime(value)}
                      className={`w-full text-left rounded-2xl p-4 border transition ${
                        isSelected
                          ? "bg-blue-50 border-blue-500 ring-1 ring-blue-500"
                          : "bg-slate-50 border-slate-200 hover:bg-slate-100 text-slate-800"
                      }`}
                    >
                      <p className={`font-bold ${isSelected ? "text-blue-900" : "text-slate-800"}`}>
                        {option.date}
                      </p>
                      <p className={`font-medium ${isSelected ? "text-blue-600" : "text-blue-500"}`}>
                        {option.time}
                      </p>
                    </button>
                  );
                })
              )}
            </div>
          </section>

          {/* 장소 선택 섹션 */}
          <section className="bg-white border border-slate-200 shadow-sm rounded-[2rem] p-8">
            <h2 className="text-xl font-bold mb-5 text-slate-900">장소 선택</h2>

            <div className="space-y-3">
              {places.length === 0 ? (
                <p className="text-slate-400 text-sm">등록된 장소 후보가 없습니다.</p>
              ) : (
                places.map((place, index) => {
                  const isSelected = selectedPlace === place;

                  return (
                    <button
                      key={index}
                      onClick={() => setSelectedPlace(place)}
                      className={`w-full text-left rounded-2xl p-4 border transition font-medium ${
                        isSelected
                          ? "bg-blue-50 border-blue-500 ring-1 ring-blue-500 text-blue-900"
                          : "bg-slate-50 border-slate-200 hover:bg-slate-100 text-slate-800"
                      }`}
                    >
                      {place}
                    </button>
                  );
                })
              )}
            </div>
          </section>
        </main>

        <button
          onClick={saveVoteAndNext}
          className="mt-8 w-full bg-blue-600 hover:bg-blue-700 text-white transition py-4 rounded-2xl font-bold shadow-sm"
        >
          결과 확인하기
        </button>
      </div>
    </div>
  );
}

export default MeetingVotePage;