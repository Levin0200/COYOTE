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

    draft.votes = [
      {
        selectedTime,
        selectedPlace,
      },
    ];

    sessionStorage.setItem("meetingDraft", JSON.stringify(draft));

    navigate("/meeting/setup/result");
  };

  return (
    <div className="min-h-screen bg-[#0B1020] text-white px-6 py-10">
      <div className="max-w-5xl mx-auto">
        <header className="mb-10">
          <p className="text-sm text-blue-300 font-semibold mb-3">
            Vote
          </p>

          <h1 className="text-4xl font-black mb-3">
            모임 투표
          </h1>

          <p className="text-slate-300">
            원하는 시간과 장소를 선택해주세요.
          </p>
        </header>

        <main className="grid lg:grid-cols-2 gap-8">
          <section className="bg-white/10 border border-white/10 rounded-[2rem] p-8">
            <h2 className="text-xl font-bold mb-5">시간 선택</h2>

            <div className="space-y-3">
              {timeOptions.map((option, index) => {
                const value = `${option.date} ${option.time}`;

                return (
                  <button
                    key={index}
                    onClick={() => setSelectedTime(value)}
                    className={`w-full text-left rounded-2xl p-4 border transition ${
                      selectedTime === value
                        ? "bg-blue-500 border-blue-300"
                        : "bg-white/5 border-white/10 hover:bg-white/10"
                    }`}
                  >
                    <p className="font-bold">{option.date}</p>
                    <p className="text-blue-200">{option.time}</p>
                  </button>
                );
              })}
            </div>
          </section>

          <section className="bg-white/10 border border-white/10 rounded-[2rem] p-8">
            <h2 className="text-xl font-bold mb-5">장소 선택</h2>

            <div className="space-y-3">
              {places.map((place, index) => (
                <button
                  key={index}
                  onClick={() => setSelectedPlace(place)}
                  className={`w-full text-left rounded-2xl p-4 border transition ${
                    selectedPlace === place
                      ? "bg-blue-500 border-blue-300"
                      : "bg-white/5 border-white/10 hover:bg-white/10"
                  }`}
                >
                  {place}
                </button>
              ))}
            </div>
          </section>
        </main>

        <button
          onClick={saveVoteAndNext}
          className="mt-8 w-full bg-blue-500 hover:bg-blue-400 transition py-4 rounded-2xl font-bold"
        >
          결과 확인하기
        </button>
      </div>
    </div>
  );
}

export default MeetingVotePage;