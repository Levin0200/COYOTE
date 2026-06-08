import { useState } from "react";
import { useNavigate } from "react-router-dom";

function CreateMeetingPage() {
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("");
  const [description, setDescription] = useState("");

  const saveDraftAndNext = () => {
    if (!title.trim()) {
      alert("모임 제목을 입력해주세요.");
      return;
    }

    const meetingDraft = {
      hostId: 1,
      title,
      category,
      description,
    };

    sessionStorage.setItem(
      "meetingDraft",
      JSON.stringify(meetingDraft)
    );

    navigate("/meeting/setup/time-options");
  };

  return (
    <div className="min-h-screen bg-[#0B1020] text-white overflow-hidden">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(59,130,246,0.25),transparent_35%),radial-gradient(circle_at_bottom_right,rgba(168,85,247,0.18),transparent_35%)]" />

      <div className="relative max-w-5xl mx-auto px-6 py-8">
        <header className="mb-14">
          <p className="text-sm text-blue-300 font-semibold mb-3">
            Create new meeting
          </p>
          <h1 className="text-4xl font-black tracking-tight mb-3">
            모임 생성
          </h1>
          <p className="text-slate-300">
            모임 정보를 입력하고 다음 단계로 이동하세요.
          </p>
        </header>

        <main className="grid lg:grid-cols-3 gap-8">
          <section className="lg:col-span-2 bg-white/10 backdrop-blur-2xl border border-white/10 rounded-[2rem] p-8 shadow-2xl">
            <div className="space-y-5">
              <input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 outline-none focus:ring-2 focus:ring-blue-400 placeholder:text-slate-400"
                placeholder="모임 제목"
              />

              <input
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 outline-none focus:ring-2 focus:ring-blue-400 placeholder:text-slate-400"
                placeholder="카테고리"
              />

              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 h-40 outline-none focus:ring-2 focus:ring-blue-400 resize-none placeholder:text-slate-400"
                placeholder="모임 설명"
              />

              <button
                onClick={saveDraftAndNext}
                className="w-full bg-blue-500 hover:bg-blue-400 transition py-4 rounded-2xl font-bold shadow-lg shadow-blue-500/25"
              >
                다음 단계로
              </button>
            </div>
          </section>

          <aside className="bg-white/5 border border-white/10 rounded-[2rem] p-6">
            <h2 className="text-lg font-bold mb-5">생성 흐름</h2>

            <div className="space-y-4">
              {["정보 입력", "시간 후보 등록", "장소 후보 등록", "최종 확인"].map(
                (text, index) => (
                  <div key={text} className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-xl bg-blue-500/20 text-blue-300 flex items-center justify-center font-bold">
                      {index + 1}
                    </div>
                    <p className="text-slate-200">{text}</p>
                  </div>
                )
              )}
            </div>
          </aside>
        </main>
      </div>
    </div>
  );
}

export default CreateMeetingPage;