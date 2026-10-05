import { useNavigate } from "react-router-dom";

function MeetingDetailPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-slate-50 px-6 py-10">
      <div className="max-w-2xl mx-auto">

        <button
          onClick={() => navigate(-1)}
          className="text-gray-500 hover:text-gray-800 mb-6"
        >
          ← 모임 상세
        </button>

        <div className="bg-white rounded-3xl border border-gray-100 shadow-sm overflow-hidden">

          <div className="p-8 pb-6">

            <div className="inline-flex items-center px-3 py-1 rounded-full bg-green-50 text-green-600 text-sm font-medium mb-4">
              ✓ 일정 확정
            </div>

            <h1 className="text-2xl font-bold text-gray-900">
              대학 동창 신년회 모임
            </h1>

            <p className="text-gray-500 mt-2">
              일정과 장소가 최종 확정된 모임입니다.
            </p>

          </div>

          <div className="px-8 pb-8">

            <div className="rounded-2xl bg-slate-50 p-6">

              <div className="mb-5">
                <p className="text-sm text-gray-400 mb-1">
                  📅 만나는 날짜
                </p>

                <p className="text-xl font-bold text-gray-900">
                  2026년 06월 06일 (토)
                </p>
              </div>

              <div className="grid grid-cols-2 gap-4">

                <div>
                  <p className="text-sm text-gray-400 mb-1">
                    🕐 만나는 시간
                  </p>

                  <p className="text-lg font-semibold text-gray-900">
                    18:00
                  </p>
                </div>

                <div>
                  <p className="text-sm text-gray-400 mb-1">
                    👥 참여 인원
                  </p>

                  <p className="text-lg font-semibold text-gray-900">
                    8명
                  </p>
                </div>

              </div>

            </div>

            <div className="mt-4 rounded-2xl border border-gray-100 p-6">

              <p className="text-sm text-gray-400 mb-2">
                📍 만나는 장소
              </p>

              <p className="text-lg font-semibold text-gray-900">
                홍대 스페이스 룸
              </p>

              <button
                onClick={() => alert("지도에서 위치를 확인하는 기능은 준비 중입니다.")}
                className="w-full mt-4 bg-[#4F7DF3] text-white py-3 rounded-xl font-medium hover:bg-[#3D6BE5] transition"
              >
                📍 지도에서 위치 보기
              </button>

            </div>

            <div className="mt-4">

              <p className="text-sm font-medium text-gray-500 mb-2">
                모임 설명
              </p>

              <div className="rounded-2xl bg-slate-50 p-5">
                <p className="text-gray-700 leading-7">
                  오랜만에 만나는 동창들과 저녁 식사를 하는 모임입니다.
                </p>
              </div>

            </div>

            <div className="grid grid-cols-2 gap-3 mt-6">

              <button
                onClick={() => alert("캘린더 추가 기능은 준비 중입니다.")}
                className="py-3 rounded-xl border border-gray-200 bg-white text-gray-700 font-medium hover:bg-gray-50 transition"
              >
                📅 캘린더에 추가
              </button>

              <button
                onClick={() => alert("모임 공유 기능은 준비 중입니다.")}
                className="py-3 rounded-xl border border-gray-200 bg-white text-gray-700 font-medium hover:bg-gray-50 transition"
              >
                🔗 모임 공유하기
              </button>

            </div>

          </div>

        </div>
      </div>
    </div>
  );
}

export default MeetingDetailPage;
