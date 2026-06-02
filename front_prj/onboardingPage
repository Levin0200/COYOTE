import { useState } from "react";
import { useNavigate } from "react-router-dom";

function OnboardingPage() {
  const navigate = useNavigate();

  const [selectedInterests, setSelectedInterests] = useState([]);

  const interests = [
    "스터디",
    "운동",
    "여행",
    "맛집",
    "게임",
    "기타",
  ];

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-6">

      <div
        className="
          w-full
          max-w-md
          bg-white
          rounded-3xl
          border
          border-gray-100
          p-8
          shadow-sm
        "
      >

        {/* 로고 */}

        <div className="text-center mb-8">

          <div className="text-[#4F7DF3] font-bold text-xl mb-2">
            모여ON
          </div>

          <h2 className="text-3xl font-bold mb-2">
            프로필 설정
          </h2>

          <p className="text-gray-500">
            AI 추천 정확도를 높이기 위한 선택 정보입니다.
          </p>

        </div>

        {/* 주소 */}

        <div className="mb-8">

          <label className="block text-sm font-medium text-gray-700 mb-2">
            집 주소 (선택)
          </label>

          <input
            type="text"
            placeholder="예) 인천광역시 동구"
            className="
              w-full
              border
              border-gray-200
              rounded-xl
              p-3
              focus:outline-none
              focus:ring-2
              focus:ring-[#4F7DF3]
            "
          />

        </div>

        {/* 관심 모임 */}

        <div className="mb-10">

          <p className="text-sm font-medium text-gray-700 mb-4">
            관심 있는 모임
          </p>

          <div className="flex flex-wrap gap-3">

            {interests.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => {
                  if (selectedInterests.includes(item)) {
                    setSelectedInterests(
                      selectedInterests.filter(
                        (interest) => interest !== item
                      )
                    );
                  } else {
                    setSelectedInterests([
                      ...selectedInterests,
                      item,
                    ]);
                  }
                }}
                className={`
                  px-4
                  py-2
                  rounded-full
                  transition
                  ${
                    selectedInterests.includes(item)
                      ? "bg-[#4F7DF3] text-white"
                      : "bg-slate-100 text-gray-700 hover:bg-slate-200"
                  }
                `}
              >
                {item}
              </button>
            ))}

          </div>

        </div>

        {/* 저장 */}

        <button
          onClick={() => navigate("/main")}
          className="
            w-full
            bg-[#4F7DF3]
            text-white
            py-3
            rounded-xl
            font-medium
            hover:bg-[#3D6BE5]
            transition
            mb-3
          "
        >
          저장하고 시작하기
        </button>

        {/* 건너뛰기 */}

        <button
          onClick={() => navigate("/main")}
          className="
            w-full
            border
            border-gray-200
            py-3
            rounded-xl
            text-gray-600
            hover:bg-slate-50
            transition
          "
        >
          건너뛰기
        </button>

      </div>

    </div>
  );
}

export default OnboardingPage;
