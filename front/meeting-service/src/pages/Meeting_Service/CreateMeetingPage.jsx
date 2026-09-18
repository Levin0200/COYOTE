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

    sessionStorage.setItem("meetingDraft", JSON.stringify(meetingDraft));

    navigate("/meeting/setup/time-options");
  };

  return (
    <div className="min-h-screen bg-white text-gray-900 overflow-hidden">
      <section className="min-h-screen flex flex-col justify-center px-6">
        <div className="max-w-5xl mx-auto w-full">
          <header className="mb-14">
            <p className="text-[#4F7DF3] font-semibold mb-3">
              Create new meeting
            </p>

            <h1 className="text-5xl font-black tracking-tight mb-4">
              모임 생성
            </h1>

            <p className="text-gray-500 text-lg">
              모임 정보를 입력하고 다음 단계로 이동하세요.
            </p>
          </header>

          <main className="grid lg:grid-cols-3 gap-8">
            <section className="lg:col-span-2 bg-white border border-gray-100 rounded-[2rem] p-8 shadow-xl">
              <div className="space-y-5">
                <input
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full bg-slate-50 border border-gray-200 rounded-2xl px-5 py-4 outline-none focus:ring-2 focus:ring-[#4F7DF3] placeholder:text-gray-400"
                  placeholder="모임 제목"
                />

                <input
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full bg-slate-50 border border-gray-200 rounded-2xl px-5 py-4 outline-none focus:ring-2 focus:ring-[#4F7DF3] placeholder:text-gray-400"
                  placeholder="카테고리"
                />

                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  className="w-full bg-slate-50 border border-gray-200 rounded-2xl px-5 py-4 h-40 outline-none focus:ring-2 focus:ring-[#4F7DF3] resize-none placeholder:text-gray-400"
                  placeholder="모임 설명"
                />

                <button
                  onClick={saveDraftAndNext}
                  className="w-full bg-[#4F7DF3] hover:bg-[#3D6BE5] transition py-4 rounded-2xl text-white font-bold shadow-lg shadow-blue-200"
                >
                  다음 단계로
                </button>
              </div>
            </section>

            <aside className="bg-slate-50 border border-gray-100 rounded-[2rem] p-6 shadow-sm">
              <h2 className="text-lg font-black mb-5">생성 흐름</h2>

              <div className="space-y-4">
                {["정보 입력", "시간 후보 등록", "장소 후보 등록", "최종 확인"].map(
                  (text, index) => (
                    <div key={text} className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-xl bg-blue-50 text-[#4F7DF3] flex items-center justify-center font-black">
                        {index + 1}
                      </div>
                      <p className="text-gray-700 font-bold">{text}</p>
                    </div>
                  )
                )}
              </div>
            </aside>
          </main>
        </div>
      </section>
    </div>
  );
}

export default CreateMeetingPage;