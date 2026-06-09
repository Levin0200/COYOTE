import { useState } from "react";
import { useNavigate } from "react-router-dom";

function MeetingPlacesPage() {
  const navigate = useNavigate();

  const [place, setPlace] = useState("");
  const [places, setPlaces] = useState([]);

  const addPlace = () => {
    if (!place.trim()) {
      alert("장소를 입력해주세요.");
      return;
    }

    setPlaces([...places, place]);
    setPlace("");
  };

  const saveAndNext = () => {
    const draft = JSON.parse(sessionStorage.getItem("meetingDraft"));

    // sessionStorage 데이터 부재 시 런타임 에러 방지 방어 코드
    const updatedDraft = draft ? { ...draft, places } : { places };

    sessionStorage.setItem("meetingDraft", JSON.stringify(updatedDraft));

    navigate("/meeting/setup/vote");
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 px-6 py-10">
      <div className="max-w-5xl mx-auto">
        <header className="mb-10">
          <p className="text-sm text-blue-600 font-semibold mb-3">
            Place Option
          </p>
          <h1 className="text-4xl font-black mb-3 text-slate-950">
            장소 후보 등록
          </h1>
          <p className="text-slate-500">
            모임 장소 후보를 추가해주세요.
          </p>
        </header>

        <main className="grid lg:grid-cols-3 gap-8">
          <section className="lg:col-span-2 bg-white border border-slate-200 shadow-sm rounded-[2rem] p-8">
            <div className="space-y-5">
              <input
                type="text"
                value={place}
                onChange={(e) => setPlace(e.target.value)}
                placeholder="예) 강남역 스타벅스"
                className="w-full bg-slate-50 border border-slate-200 text-slate-800 placeholder-slate-400 rounded-2xl px-5 py-4 outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition"
              />

              <button
                onClick={addPlace}
                className="w-full bg-slate-100 hover:bg-slate-200 text-slate-800 transition py-4 rounded-2xl font-bold border border-slate-200"
              >
                장소 후보 추가
              </button>

              <button
                onClick={saveAndNext}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white transition py-4 rounded-2xl font-bold shadow-sm"
              >
                다음 단계로
              </button>
            </div>
          </section>

          <aside className="bg-white border border-slate-200 shadow-sm rounded-[2rem] p-6">
            <h2 className="text-lg font-bold mb-5 text-slate-900">
              추가된 장소 후보
            </h2>

            <div className="space-y-3">
              {places.length === 0 ? (
                <p className="text-slate-400 text-sm">
                  아직 추가된 장소가 없습니다.
                </p>
              ) : (
                places.map((place, index) => (
                  <div
                    key={index}
                    className="bg-slate-50 border border-slate-100 rounded-2xl p-4 text-slate-800 font-medium"
                  >
                    {place}
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

export default MeetingPlacesPage;