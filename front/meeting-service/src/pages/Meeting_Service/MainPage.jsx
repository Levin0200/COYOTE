// 얘가 페이지 이동 담당함
import { useNavigate } from "react-router-dom";

// 디자인 개쩌는 페이지 Keep!
// 삭제될 수 있는 페이지

function MainPage() {
  // 페이지 이동 담당할 수 있게 해주는 함수
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#0B1020] text-white overflow-hidden">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(59,130,246,0.25),transparent_35%),radial-gradient(circle_at_bottom_right,rgba(168,85,247,0.18),transparent_35%)]" />

      <div className="relative max-w-6xl mx-auto px-6 py-8">
        <header className="flex justify-between items-center mb-24">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-blue-500 flex items-center justify-center font-black">
              C
            </div>

            <h1 className="text-xl font-black tracking-tight">
              COYOTE
            </h1>
          </div>

          <span className="hidden sm:block text-sm text-slate-300 border border-white/10 px-4 py-2 rounded-full bg-white/5">
            초대 기반 모임 추천 서비스
          </span>
        </header>

        <main className="grid lg:grid-cols-2 gap-14 items-center">
          <section>
            <div className="inline-flex items-center gap-2 bg-white/5 border border-white/10 rounded-full px-4 py-2 text-sm text-blue-200 mb-6">
              <span className="w-2 h-2 rounded-full bg-blue-400" />
              Smart meeting coordination
            </div>

            <h2 className="text-5xl sm:text-6xl font-black leading-tight tracking-tight mb-6">
              모임 조율을
              <br />
              더 빠르고 쉽게.
            </h2>

            <p className="text-slate-300 text-lg leading-relaxed mb-10 max-w-xl">
              COYOTE는 초대받은 사람들의 일정과 선호 정보를 바탕으로
              모임 시간과 장소를 정리해주는 초대 기반 모임 추천 서비스입니다.
            </p>

            <div className="flex flex-col sm:flex-row gap-4">
              <button
                onClick={() => navigate("/create-meeting")}
                className="bg-blue-500 hover:bg-blue-400 transition px-8 py-4 rounded-2xl font-bold shadow-lg shadow-blue-500/25"
              >
                모임 생성하기
              </button>

              <button className="bg-white/5 hover:bg-white/10 transition px-8 py-4 rounded-2xl font-bold border border-white/10">
                서비스 살펴보기
              </button>
            </div>
          </section>

          <section className="relative">
            <div className="absolute -inset-4 bg-blue-500/20 blur-3xl rounded-full" />

            <div className="relative bg-white/10 backdrop-blur-2xl border border-white/10 rounded-4xl p-7 shadow-2xl">
              <div className="bg-[#111827] rounded-3xl p-5 mb-5 border border-white/10">
                <p className="text-sm text-slate-400 mb-2">
                  New meeting
                </p>

                <h3 className="text-2xl font-bold mb-4">
                  강남 점심 모임
                </h3>

                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-white/5 rounded-2xl p-4">
                    <p className="text-xs text-slate-400">
                      Category
                    </p>

                    <p className="font-semibold mt-1">
                      식사
                    </p>
                  </div>

                  <div className="bg-white/5 rounded-2xl p-4">
                    <p className="text-xs text-slate-400">
                      Status
                    </p>

                    <p className="font-semibold mt-1 text-blue-300">
                      OPEN
                    </p>
                  </div>
                </div>
              </div>

              <div className="space-y-3">
                {[
                  "모임 정보 입력",
                  "초대 링크 공유",
                  "AI 추천으로 시간·장소 조율",
                ].map((text, index) => (
                  <div
                    key={text}
                    className="flex items-center gap-4 bg-white/5 rounded-2xl p-4 border border-white/5"
                  >
                    <div className="w-9 h-9 rounded-xl bg-blue-500/20 text-blue-300 flex items-center justify-center font-bold">
                      {index + 1}
                    </div>

                    <p className="font-semibold">
                      {text}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
}

export default MainPage;