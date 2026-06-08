import { useState } from "react";
import { useNavigate } from "react-router-dom";

function MeetingTimeOptionsPage() {
  const navigate = useNavigate();

  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [timeOptions, setTimeOptions] = useState([]);

  const addTimeOption = () => {
    if (!date || !time) {
      alert("날짜와 시간을 입력해주세요.");
      return;
    }

    const newOption = {
      date,
      time,
    };

    setTimeOptions([...timeOptions, newOption]);

    setDate("");
    setTime("");
  };

  const saveAndNext = () => {
    const draft = JSON.parse(sessionStorage.getItem("meetingDraft"));

    draft.timeOptions = timeOptions;

    sessionStorage.setItem("meetingDraft", JSON.stringify(draft));

    navigate("/meeting/setup/places");
  };

  return (
    <div className="min-h-screen bg-[#0B1020] text-white px-6 py-10">
      <div className="max-w-5xl mx-auto">
        <header className="mb-10">
          <p className="text-sm text-blue-300 font-semibold mb-3">
            Time Option
          </p>
          <h1 className="text-4xl font-black mb-3">
            시간 후보 등록
          </h1>
          <p className="text-slate-300">
            모임 가능한 날짜와 시간을 후보로 추가해주세요.
          </p>
        </header>

        <main className="grid lg:grid-cols-3 gap-8">
          <section className="lg:col-span-2 bg-white/10 border border-white/10 rounded-[2rem] p-8">
            <div className="space-y-5">
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 outline-none focus:ring-2 focus:ring-blue-400"
              />

              <input
                type="time"
                value={time}
                onChange={(e) => setTime(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 outline-none focus:ring-2 focus:ring-blue-400"
              />

              <button
                onClick={addTimeOption}
                className="w-full bg-white/10 hover:bg-white/15 transition py-4 rounded-2xl font-bold border border-white/10"
              >
                시간 후보 추가
              </button>

              <button
                onClick={saveAndNext}
                className="w-full bg-blue-500 hover:bg-blue-400 transition py-4 rounded-2xl font-bold"
              >
                다음 단계로
              </button>
            </div>
          </section>

          <aside className="bg-white/5 border border-white/10 rounded-[2rem] p-6">
            <h2 className="text-lg font-bold mb-5">추가된 시간 후보</h2>

            <div className="space-y-3">
              {timeOptions.length === 0 ? (
                <p className="text-slate-400">
                  아직 추가된 시간이 없습니다.
                </p>
              ) : (
                timeOptions.map((option, index) => (
                  <div
                    key={index}
                    className="bg-white/5 border border-white/10 rounded-2xl p-4"
                  >
                    <p className="font-bold">{option.date}</p>
                    <p className="text-blue-300">{option.time}</p>
                  </div>
                ))
              )}
            </div>
          </aside>
        </main>
      </div>
    </div>
  );
}

export default MeetingTimeOptionsPage;