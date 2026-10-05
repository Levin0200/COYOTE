import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import "./MyPage.css";

// FullCalendar 라이브러리 및 플러그인
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import interactionPlugin from "@fullcalendar/interaction";

function MyPage() {
  const navigate = useNavigate();

  // 1. 프로필 및 취향 상태
  const [profile, setProfile] = useState({
    name: "김모여",
    email: "moyeon@example.com",
    address: "인천광역시 동구",
  });


  // 2. 캘린더 일정 데이터 (Mock Data - 향후 백엔드 GET /api/calendar/events 연동)
  const [events, setEvents] = useState([]);

  // 3. [추가] 팝업(모달)용 상태
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // 📍 1) 지도를 연결할 DOM 참조 생성
  const mapContainer = useRef(null);

  // 📍 2) 저장된 모임 장소 데이터 (나중에 백엔드 API 연동 위치)
  const savedPlaces = [
    { id: 1, name: "강남역 모임 장소", lat: 37.4979, lng: 127.0276 },
    { id: 2, name: "홍대 스페이스 룸", lat: 37.5563, lng: 126.9226 },
    { id: 3, name: "여의도 한강공원", lat: 37.5284, lng: 126.9331 },
  ];

  useEffect(() => {
    // 확정된 내 모임 일정 예시 데이터
    const mockEvents = [
      {
        id: 1,
        title: "캡스톤 회의",
        start: "2026-04-12T14:00:00",
        end: "2026-04-12T16:00:00",
        extendedProps: {
          location: "인천대학교 7호관 학술정보원",
          category: "공부/회의",
          description: "모여ON 프론트/백엔드 API 규격 최종 점검",
        },
      },
      {
        id: 2,
        title: "알고리즘 스터디",
        start: "2026-04-15T19:00:00",
        end: "2026-04-15T21:00:00",
        extendedProps: {
          location: "송도 탐앤탐스",
          category: "스터디",
          description: "백준 파이썬/자바 알고리즘 문제 풀이 공유",
        },
      },
    ];
    setEvents(mockEvents);
  }, []);

  // 캘린더 일정 클릭 시 실행되는 함수
  const handleEventClick = (clickInfo) => {
    const eventObj = clickInfo.event;

    // 클릭된 일정 정보를 상태에 저장하고 모달을 열어줍니다.
    setSelectedEvent({
      id: eventObj.id,
      title: eventObj.title,
      start: eventObj.start ? eventObj.start.toLocaleString("ko-KR") : "",
      end: eventObj.end ? eventObj.end.toLocaleString("ko-KR") : "",
      location: eventObj.extendedProps.location || "장소 미정",
      category: eventObj.extendedProps.category || "기타",
      description: eventObj.extendedProps.description || "",
    });
    setIsModalOpen(true);
  };

  // 모달 안에서 [상세 보기] 클릭 시 이동
  const handleGoToDetail = () => {
    if (selectedEvent) {
      // 팀원이 작성할 모임 상세 페이지 경로로 이동
      navigate(`/meeting/${selectedEvent.id}`);
      setIsModalOpen(false);
    }
  };

  // 📍 3) Kakao Map 로드 및 마커 생성 로직
  useEffect(() => {
    if (!window.kakao || !window.kakao.maps) return;

    const defaultLat = 37.4738; // 인천 동구 기준
    const defaultLng = 126.6432;

    const options = {
      center: new window.kakao.maps.LatLng(defaultLat, defaultLng),
      level: 5,
    };

    const map = new window.kakao.maps.Map(mapContainer.current, options);

    // 현재 위치 조회 (브라우저 권한 필요)
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition((position) => {
        const currentLat = position.coords.latitude;
        const currentLng = position.coords.longitude;
        map.setCenter(new window.kakao.maps.LatLng(currentLat, currentLng));
      });
    }

    // 저장된 장소 마커 표시
    savedPlaces.forEach((place) => {
      const position = new window.kakao.maps.LatLng(place.lat, place.lng);
      const marker = new window.kakao.maps.Marker({ position, map });
      const infowindow = new window.kakao.maps.InfoWindow({
        content: `<div style="padding:6px 10px;font-size:12px;font-weight:bold;color:#0f172a;">${place.name}</div>`,
      });

      window.kakao.maps.event.addListener(marker, 'click', () => {
        infowindow.open(map, marker);
      });
    });
  }, []);

  return (
    <div
      className="min-h-screen bg-slate-50 py-8 px-6 text-slate-800"
      style={{ fontFamily: 'sans-serif' }}
    >
      <div className="max-w-6xl mx-auto space-y-8">
        
        {/* 상단 헤더 Nav */}
        <div className="flex justify-between items-center bg-white p-6 rounded-3xl border border-gray-100 shadow-sm">
          <div
            className="flex items-center gap-3 cursor-pointer"
            onClick={() => navigate("/main")}
          >
            <div className="flex h-[38px] w-[38px] shrink-0 items-center justify-center rounded-xl bg-blue-500 font-black text-white shadow-[0_4px_12px_rgba(59,130,246,0.2)]">
              M
            </div>

            {/* 메인페이지와 동일한 font-black 및 tracking 적용 */}
            <h1 className="whitespace-nowrap text-[22px] font-black tracking-[-0.05em] text-slate-900">
              모여ON
            </h1>
          </div>
          <button
            onClick={() => navigate("/main")}
            className="text-sm font-semibold text-gray-500 hover:text-[#4F7DF3] transition"
          >
            ← 메인으로 돌아가기
          </button>
        </div>

        {/* [메인 영역] 2컬럼 레이아웃 */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* 1. [좌측 - 2개 컬럼] 마이 캘린더 */}
          <section className="lg:col-span-2 bg-white p-6 rounded-3xl border border-gray-100 shadow-sm">
            <div className="flex justify-between items-center mb-6">
              <div>
                <h2 className="text-2xl font-bold text-slate-900">
                  📅 개인 일정 캘린더
                </h2>
                <p className="text-xs text-gray-400 mt-1">
                  일정을 클릭하면 약속 상세 정보 팝업이 표시됩니다.
                </p>
              </div>
            </div>

            <div className="calendar-container text-slate-700">
              <FullCalendar
                plugins={[dayGridPlugin, interactionPlugin]}
                initialView="dayGridMonth"
                locale="ko"
                fixedWeekCount={false}          
                showNonCurrentDates={false}  
                dayCellContent={(arg) => arg.date.getDate().toString()}   
                headerToolbar={{
                  left: "prev",
                  center: "title today",
                  right: "next",
                }}
                events={events}
                eventClick={handleEventClick}
                eventColor="#4F7DF3"
                height="auto"
                />
            </div>
          </section>

          {/* 2. [우측 - 1개 컬럼] 프로필 요약 & 개인 취향 정보 */}
          <section className="space-y-6">
            
            {/* 프로필 카드 */}
            <div className="bg-white p-6 rounded-3xl border border-gray-100 shadow-sm">
              <div className="flex items-center gap-4 mb-4">
                <div className="w-14 h-14 bg-blue-100 rounded-full flex items-center justify-center text-2xl font-bold text-[#4F7DF3]">
                  👤
                </div>
                <div>
                  <h3 className="text-lg font-bold text-slate-900">
                    {profile.name}님
                  </h3>
                  <p className="text-xs text-gray-400">{profile.email}</p>
                </div>
              </div>
              <p className="text-xs text-[#4F7DF3] font-medium bg-blue-50 p-2.5 rounded-xl text-center">
                📍 {profile.address}
              </p>
            </div>

            

            {/* 계정 관리 */}
            {/* 2. 회원정보 수정 & 로그아웃 버튼 상자 */}
            <div className="bg-white rounded-3xl border border-slate-100 p-4 shadow-sm space-y-2">
              <button 
                onClick={() => navigate("/user/edit")} // 회원정보 수정 페이지 경로로 연결
                className="w-full py-2.5 text-slate-700 font-bold bg-slate-100 hover:bg-slate-200 rounded-2xl transition text-sm flex items-center justify-center gap-1.5"
              >
                회원정보 수정
              </button>
    
              <button 
                onClick={() => alert("로그아웃 되었습니다.")} // 로그아웃 처리 함수 연결
                className="w-full py-2.5 text-red-500 font-bold bg-red-50 hover:bg-red-100 rounded-2xl transition text-sm flex items-center justify-center gap-1.5"
              >
                로그아웃
              </button>
            </div>

            {/*
            <div className="bg-white p-6 rounded-3xl border border-gray-100 shadow-sm">
              <button
                onClick={() => {
                  if (confirm("로그아웃 하시겠습니까?")) navigate("/login");
                }}
                className="w-full py-2.5 bg-red-50 text-red-500 text-xs font-semibold rounded-xl hover:bg-red-100 transition"
              >
                로그아웃
              </button>
            </div>
            */}

            {/* 📍 3. 로그아웃 밑에 조그맣게 추가하는 지도 카드 */}
            <div className="bg-white rounded-3xl border border-slate-100 p-5 shadow-sm space-y-3">
              <div className="flex items-center justify-between">
                <h4 className="font-bold text-slate-800 text-sm flex items-center gap-1.5">
                  📍 내 모임 위치
                </h4>
                <span className="text-xs text-slate-400 font-medium">인천 동구</span>
              </div>

              {/* 사이드바 너비에 맞춰 높이를 220px 정도로 아담하게 설정 */}
              <div 
                ref={mapContainer} 
                className="w-full h-[220px] rounded-2xl border border-slate-100 overflow-hidden"
              />
            </div>
          </section>
        </div>

      </div>

      {/* 4. [신규 추가] 약속 일정 클릭 시 뜨는 미니 모달 팝업 */}
      {isModalOpen && selectedEvent && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm flex justify-center items-center z-50 p-4">
          <div className="bg-white w-full max-w-sm rounded-3xl p-6 shadow-xl border border-gray-100 animate-in fade-in zoom-in duration-150">
            <div className="flex justify-between items-start mb-4">
              <div>
                <span className="inline-block px-2.5 py-1 bg-blue-50 text-[#4F7DF3] text-xs font-bold rounded-lg mb-2">
                  {selectedEvent.category}
                </span>
                <h3 className="text-xl font-bold text-slate-900">
                  {selectedEvent.title}
                </h3>
              </div>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-gray-400 hover:text-slate-600 text-xl font-bold"
              >
                ✕
              </button>
            </div>

            <div className="space-y-3 text-xs text-slate-600 bg-slate-50 p-4 rounded-2xl mb-6">
              <div>
                <span className="font-bold text-slate-800 block mb-0.5">
                  🕒 모임 일시
                </span>
                <p>{selectedEvent.start}</p>
              </div>
              <div>
                <span className="font-bold text-slate-800 block mb-0.5">
                  📍 약속 장소
                </span>
                <p>{selectedEvent.location}</p>
              </div>
              {selectedEvent.description && (
                <div>
                  <span className="font-bold text-slate-800 block mb-0.5">
                    📝 메모/설명
                  </span>
                  <p className="text-gray-500">{selectedEvent.description}</p>
                </div>
              )}
            </div>

            <div className="flex gap-2">
              <button
                onClick={() => setIsModalOpen(false)}
                className="flex-1 py-3 bg-gray-100 text-gray-600 font-bold text-xs rounded-xl hover:bg-gray-200 transition"
              >
                닫기
              </button>
              <button
                onClick={handleGoToDetail}
                className="flex-1 py-3 bg-[#4F7DF3] text-white font-bold text-xs rounded-xl hover:bg-blue-600 shadow-md shadow-blue-200 transition"
              >
                상세 보기 →
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default MyPage;
