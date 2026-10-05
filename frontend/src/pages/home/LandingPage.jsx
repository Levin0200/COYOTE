import { useNavigate } from "react-router-dom";

import { Swiper, SwiperSlide } from "swiper/react";
import { Autoplay, FreeMode } from "swiper/modules";

import "swiper/css";

function LandingPage() {
  const navigate = useNavigate();

  const features = [
    {
      title: "AI 기반 일정 조율",
      image: "/images/schedule.png",
    },
    {
      title: "위치 기반 장소 추천",
      image: "/images/location.png",
    },
    {
      title: "모임 카테고리",
      image: "/images/category.png",
    },
    {
      title: "모임 Lifecycle 관리",
      image: "/images/lifecycle.png",
    },
    {
      title: "모임 데이터 분석",
      image: "/images/analytics.png",
    },
  ];

  return (
    <div className="bg-white">

      {/* HERO */}

      <section className="min-h-screen flex flex-col justify-center items-center text-center px-6">

        <p className="text-[#4F7DF3] font-semibold mb-4">
          AI 기반 모임 관리 플랫폼
        </p>

        <h1 className="text-6xl md:text-7xl font-bold leading-tight">

          <span className="text-[#4F7DF3]">
            모임 조율
          </span>

          을

          <br />

          더 간편하게

        </h1>

        <p className="mt-8 text-gray-600 text-lg max-w-2xl">

          일정 조율부터 장소 추천까지,
          <br />
          AI가 참여자 데이터를 분석하여
          최적의 모임을 만들어드립니다.

        </p>

        {/* 기능 슬라이더 */}

        <div className="w-full max-w-7xl mt-20">

         <Swiper
           modules={[Autoplay, FreeMode]}
           freeMode={true}
           loop={true}
           speed={5000}
           autoplay={{
             delay: 0,
             disableOnInteraction: false,
           }}
           slidesPerView={3.2}
           spaceBetween={24}
         >

            {features.map((feature, index) => (
              <SwiperSlide key={index}>

                <div
                  className="
                  bg-white
                  border
                  border-gray-200
                  rounded-3xl
                  overflow-hidden
                  h-[320px]
                  "
                >

                  <div className="h-[220px] bg-slate-100">

                    <img
                      src={feature.image}
                      alt={feature.title}
                      className="
                      w-full
                      h-full
                      object-cover
                      "
                    />

                  </div>

                  <div className="p-5">

                    <h3 className="font-bold text-lg">
                      {feature.title}
                    </h3>

                  </div>

                </div>

              </SwiperSlide>
            ))}

          </Swiper>

        </div>

        {/* Scroll */}

        <div className="mt-14 animate-bounce">

          <p className="text-gray-400">
            Scroll Down
          </p>

          <div className="text-2xl">
            ↓
          </div>

        </div>

      </section>

      {/* 문제점 섹션 */}
      <section className="py-36 bg-slate-50">
        <div className="max-w-6xl mx-auto px-6">

          <h2 className="text-4xl font-bold text-center mb-16">
            이런 경험 없으셨나요?
          </h2>

          <div className="grid md:grid-cols-2 gap-6">

            {[
              {
                title: "단톡방에서 끝없는 일정 조율",
                desc: "누가 가능한지 계속 물어보느라 시간이 낭비됩니다.",
              },
              {
                title: "장소 검색과 비교의 번거로움",
                desc: "여러 사이트를 왔다 갔다 하며 장소를 고르기 어렵습니다.",
              },
              {
                title: "참석 여부 확인의 어려움",
                desc: "누가 오는지 정리하는 과정이 복잡합니다.",
              },
              {
                title: "변경 사항 전달의 불편함",
                desc: "일정이 바뀔 때마다 모든 사람에게 전달해야 합니다.",
              },
            ].map((item, index) => (
              <div
                key={index}
                className="
                  bg-white
                  rounded-3xl
                  p-8
                  border border-gray-100
                  shadow-sm
                  hover:shadow-lg
                  hover:-translate-y-1
                  transition
                "
              >

                {/* 상단 작은 라벨 */}
                <p className="text-sm text-[#4F7DF3] font-semibold mb-3">
                  Problem {index + 1}
                </p>

                {/* 타이틀 */}
                <h3 className="text-xl font-bold text-gray-900 mb-2">
                  {item.title}
                </h3>

                {/* 설명 */}
                <p className="text-gray-500 leading-relaxed">
                  {item.desc}
                </p>

              </div>
            ))}

          </div>

        </div>
      </section>

      {/* 핵심 가치 소개 */}
      <section className="py-32 bg-white">
        <div className="max-w-6xl mx-auto px-6">

          <h2 className="text-4xl font-bold text-center mb-20">
            모여온이 바꾸는 모임 경험
          </h2>

          <div className="space-y-28">

            {[
              {
                title: "스트레스 없이 모임을 생성하세요",
                desc: "복잡한 단톡방 조율 없이 몇 번의 클릭만으로 모임을 생성할 수 있습니다.",
                sub: "모임 시작 단계에서 발생하는 불필요한 커뮤니케이션을 최소화합니다.",
                img: "/images/feature1.png",
                highlight: "creation",
              },
              {
                title: "일정 조율은 AI가 대신합니다",
                desc: "참여자들의 가능한 시간을 자동으로 분석하여 최적의 일정을 추천합니다.",
                sub: "누구의 일정이 가능한지 일일이 확인할 필요가 없습니다.",
                img: "/images/feature2.png",
                highlight: "ai",
              },
              {
                title: "더 나은 모임은 데이터로 만들어집니다",
                desc: "모임 이후의 평점과 피드백이 다음 추천 정확도를 높입니다.",
                sub: "경험이 쌓일수록 더 정확한 모임 추천이 가능합니다.",
                img: "/images/feature3.png",
                highlight: "data",
              },
            ].map((item, index) => {
              const isReversed = index % 2 === 1;

              return (
                <div
                  key={index}
                  className="grid md:grid-cols-2 gap-12 items-center"
                >

                  {/* TEXT */}
                  <div className={isReversed ? "md:order-2" : ""}>

                    {/* TITLE */}
                    <h3 className="text-3xl font-extrabold text-gray-900 mb-4">
                      {item.title}
                    </h3>

                    {/* MAIN DESC */}
                    <p className="text-lg text-gray-700 font-medium mb-3 leading-relaxed">
                      {item.desc}
                    </p>

                    {/* SUB DESC */}
                    <p className="text-sm text-gray-400 leading-relaxed">
                      {item.sub}
                    </p>

                  </div>

                  {/* IMAGE */}
                  <div
                    className={`
                      h-[320px]
                      rounded-3xl
                      bg-slate-100
                      overflow-hidden
                      shadow-sm
                      ${isReversed ? "md:order-1" : ""}
                    `}
                  >
                    <img
                      src={item.img}
                      alt={item.title}
                      className="w-full h-full object-cover"
                    />
                  </div>

                </div>
              );
            })}

          </div>

        </div>
      </section>

      {/* 기술 스택 */}
      <section className="py-32 bg-white">
        <div className="max-w-6xl mx-auto px-6 text-center">

          <h2 className="text-4xl font-bold mb-6">
            기술보다 중요한 건, 경험입니다
          </h2>

          <p className="text-gray-500 mb-16">
            모여온은 안정적인 기술 위에서 더 나은 모임 경험을 제공합니다.
          </p>

          <div className="grid md:grid-cols-3 gap-6">

            {[
              {
                title: "빠르고 직관적인 UI",
                desc: "React + Tailwind 기반으로 누구나 쉽게 사용할 수 있는 인터페이스를 제공합니다.",
              },
              {
                title: "실시간 모임 관리",
                desc: "WebSocket을 활용해 참여 상태와 변경 사항을 즉시 반영합니다.",
              },
              {
                title: "AI 기반 추천 시스템",
                desc: "OpenAI API를 활용하여 일정과 장소를 자동으로 추천합니다.",
              },
              {
                title: "안정적인 데이터 관리",
                desc: "Spring Boot + MariaDB 기반으로 안전하게 데이터를 처리합니다.",
              },
              {
                title: "확장 가능한 구조",
                desc: "향후 마이데이터 및 외부 API 연동을 고려한 구조입니다.",
              },
              {
                title: "지속적인 개선",
                desc: "사용자 피드백과 데이터를 기반으로 지속적으로 발전합니다.",
              },
            ].map((item, index) => (
              <div
                key={index}
                className="
                  bg-slate-50
                  rounded-3xl
                  p-6
                  text-left
                  hover:shadow-md
                  hover:-translate-y-1
                  transition
                "
              >

                <h3 className="text-lg font-bold mb-2 text-gray-900">
                  {item.title}
                </h3>

                <p className="text-sm text-gray-500 leading-relaxed">
                  {item.desc}
                </p>

              </div>
            ))}

          </div>

        </div>
      </section>

      {/* CTA */}

      <section className="py-40 text-center px-6">

        <h2 className="text-5xl font-bold">
          지금 바로 시작해보세요
        </h2>

        <p className="text-gray-500 mt-5">
          AI와 함께 더 쉬운 모임 관리
        </p>

        <button
          onClick={() => navigate("/login")}
          className="
          mt-10
          bg-[#4F7DF3]
          text-white
          px-10
          py-4
          rounded-full
          hover:bg-[#3D6BE5]
          transition
          "
        >
          로그인하러 가기
        </button>

      </section>

    </div>
  );
}

export default LandingPage;