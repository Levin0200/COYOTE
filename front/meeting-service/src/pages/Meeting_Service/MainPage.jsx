import { useNavigate } from "react-router-dom";

function MainPage() {
  const navigate = useNavigate();

  const steps = [
    "모임 정보 입력",
    "시간 후보 등록",
    "장소 후보 등록",
    "투표 진행",
  ];

  return (
    <div className="min-h-screen bg-white text-gray-900 overflow-hidden">
      <section className="min-h-screen flex flex-col justify-center px-6">
        <div className="max-w-6xl mx-auto w-full">
          <header className="flex justify-between items-center mb-24">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-[#4F7DF3] text-white flex items-center justify-center font-black">
                C
              </div>
              <h1 className="text-xl font-black tracking-tight">COYOTE</h1>
            </div>

            <span className="hidden sm:block text-sm text-gray-500 border border-gray-200 px-4 py-2 rounded-full bg-white shadow-sm">
              초대 기반 모임 추천 서비스
            </span>
          </header>

          <main className="grid lg:grid-cols-2 gap-16 items-center">
            <section>
              <p className="text-[#4F7DF3] font-semibold mb-4">
                AI 기반 모임 관리 플랫폼
              </p>

              <h2 className="text-6xl md:text-7xl font-black leading-tight mb-8">
                <span className="text-[#4F7DF3]">모임 조율</span>을
                <br />
                더 간편하게
              </h2>

              <p className="text-gray-500 text-lg leading-relaxed mb-10 max-w-xl">
                일정 조율부터 장소 추천까지, 참여자 데이터를 기반으로
                더 빠르고 편한 모임 생성을 도와드립니다.
              </p>

              <div className="flex flex-col sm:flex-row gap-4">
                <button
                  onClick={() => navigate("/create-meeting")}
                  className="bg-[#4F7DF3] hover:bg-[#3D6BE5] transition px-9 py-4 rounded-full text-white font-bold shadow-lg shadow-blue-200"
                >
                  모임 생성하기
                </button>

                <button className="bg-slate-50 hover:bg-slate-100 transition px-9 py-4 rounded-full font-bold border border-gray-200 text-gray-700">
                  서비스 살펴보기
                </button>
              </div>
            </section>

            <section className="relative">
              <div className="absolute -inset-6 bg-blue-100 blur-3xl rounded-full" />

              <div className="relative bg-white border border-gray-100 rounded-[2rem] p-7 shadow-xl">
                <div className="bg-slate-50 rounded-3xl p-6 mb-5 border border-gray-100">
                  <p className="text-sm text-[#4F7DF3] font-semibold mb-2">
                    New meeting
                  </p>

                  <h3 className="text-2xl font-black mb-5">
                    강남 점심 모임
                  </h3>

                  <div className="grid grid-cols-2 gap-3">
                    <div className="bg-white rounded-2xl p-4 border border-gray-100">
                      <p className="text-xs text-gray-400">Category</p>
                      <p className="font-bold mt-1">식사</p>
                    </div>

                    <div className="bg-white rounded-2xl p-4 border border-gray-100">
                      <p className="text-xs text-gray-400">Status</p>
                      <p className="font-bold mt-1 text-[#4F7DF3]">OPEN</p>
                    </div>
                  </div>
                </div>

                <div className="space-y-3">
                  {steps.map((text, index) => (
                    <div
                      key={text}
                      className="flex items-center gap-4 bg-white rounded-2xl p-4 border border-gray-100 shadow-sm hover:shadow-md transition"
                    >
                      <div className="w-9 h-9 rounded-xl bg-blue-50 text-[#4F7DF3] flex items-center justify-center font-black">
                        {index + 1}
                      </div>

                      <p className="font-bold text-gray-800">{text}</p>
                    </div>
                  ))}
                </div>
              </div>
            </section>
          </main>
        </div>
      </section>
    </div>
  );
}

export default MainPage;