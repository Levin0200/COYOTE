import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import AppHeader from "../../components/AppHeader";

function MainPage() {
  const navigate = useNavigate();

  // --------------------------------------------------
  // 기본 상태
  // --------------------------------------------------

  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(null);

  // 간단한 모임 정보 팝업
  const [selectedMeeting, setSelectedMeeting] = useState(null);

  // 상세정보 팝업
  const [showMeetingDetail, setShowMeetingDetail] = useState(false);

  // 투표 요청을 보낸 사람

  // 지도 확대 팝업
  const [showMapModal, setShowMapModal] = useState(false);

  // --------------------------------------------------
  // 사용자 정보
  // --------------------------------------------------

  const savedUser = JSON.parse(localStorage.getItem("user") || "null");

  const userName = savedUser?.name || "김모여";
  const userAddress = savedUser?.address || "인천광역시 동구";

  // --------------------------------------------------
  // 확정된 모임 임시 데이터
  // 나중에 백엔드 API 데이터로 교체
  // --------------------------------------------------

  const confirmedMeetings = [
  {
    id: 1,
    date: "2026-10-08",
    title: "팀 프로젝트 회의",
    description:
      "팀 프로젝트 진행 상황을 공유하고 다음 작업을 정하기 위한 모임입니다.",
    location: "인천광역시 동구",
    place: "학교 근처 카페",
    time: "15:00",

    members: [
      "김모여",
      "박모여",
      "이모여",
      "남모여",
      "최모여",
    ],

    votes: [
      {
        member: "김모여",
        dates: ["2026-10-07", "2026-10-08"],
      },
      {
        member: "박모여",
        dates: ["2026-10-08"],
      },
      {
        member: "이모여",
        dates: ["2026-10-08"],
      },
      {
        member: "남모여",
        dates: ["2026-10-07", "2026-10-08"],
      },
      {
        member: "최모여",
        dates: [],
      },
    ],
  },

  {
    id: 2,
    date: "2026-10-12",
    title: "캡스톤 프로젝트 회의",
    description:
      "캡스톤 프로젝트 진행 상황을 공유하고 다음 작업을 정하기 위한 모임입니다.",
    location: "인천광역시 동구",
    place: "학교 근처 카페",
    time: "14:00",

    members: [
      "김모여",
      "박모여",
      "이모여",
      "남모여",
      "최모여",
    ],

    votes: [
      {
        member: "김모여",
        dates: ["2026-10-10", "2026-10-12"],
      },
      {
        member: "박모여",
        dates: ["2026-10-12"],
      },
      {
        member: "이모여",
        dates: ["2026-10-11", "2026-10-12"],
      },
      {
        member: "남모여",
        dates: ["2026-10-10"],
      },
      {
        member: "최모여",
        dates: [],
      },
    ],
  },

  {
    id: 3,
    date: "2026-10-20",
    title: "동아리 정기 모임",
    description:
      "동아리 활동 내용을 공유하고 다음 활동을 계획하기 위한 정기 모임입니다.",
    location: "인천광역시 남동구",
    place: "학교 앞 카페",
    time: "18:30",

    members: [
      "김모여",
      "박모여",
      "이모여",
      "남모여",
      "최모여",
    ],

    votes: [
      {
        member: "김모여",
        dates: ["2026-10-19", "2026-10-20"],
      },
      {
        member: "박모여",
        dates: ["2026-10-20"],
      },
      {
        member: "이모여",
        dates: ["2026-10-20"],
      },
      {
        member: "남모여",
        dates: ["2026-10-19", "2026-10-20"],
      },
      {
        member: "최모여",
        dates: ["2026-10-20"],
      },
    ],
  },
];

  // --------------------------------------------------
  // 현재 달
  // --------------------------------------------------

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  const firstDay = new Date(year, month, 1).getDay();

  const lastDate = new Date(year, month + 1, 0).getDate();

  const previousMonthLastDate = new Date(year, month, 0).getDate();

  // --------------------------------------------------
  // 캘린더 날짜 생성
  // --------------------------------------------------

  const calendarDays = [];

  // 이전 달
  for (let i = firstDay - 1; i >= 0; i--) {
    calendarDays.push({
      day: previousMonthLastDate - i,
      currentMonth: false,
      date: null,
    });
  }

  // 현재 달
  for (let day = 1; day <= lastDate; day++) {
    const dateString =
      `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(
        2,
        "0"
      )}`;

    calendarDays.push({
      day,
      currentMonth: true,
      date: dateString,
    });
  }

  // 다음 달
  let nextMonthDay = 1;

  while (calendarDays.length < 42) {
    calendarDays.push({
      day: nextMonthDay,
      currentMonth: false,
      date: null,
    });

    nextMonthDay++;
  }

  // --------------------------------------------------
  // 오늘
  // --------------------------------------------------

  const today = new Date();

  const todayString =
    `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(
      2,
      "0"
    )}-${String(today.getDate()).padStart(2, "0")}`;

    // --------------------------------------------------
// 가장 가까운 예정 모임
// --------------------------------------------------

const upcomingMeetings = confirmedMeetings
  .filter((meeting) => meeting.date >= todayString)
  .sort((a, b) => a.date.localeCompare(b.date));

const nextMeeting =
  upcomingMeetings.length > 0
    ? upcomingMeetings[0]
    : null;

  // --------------------------------------------------
  // 특정 날짜의 확정 모임 찾기
  // --------------------------------------------------

  const getMeetingByDate = (date) => {
    return confirmedMeetings.find((meeting) => meeting.date === date);
  };

  // --------------------------------------------------
  // 날짜 포맷
  // --------------------------------------------------

  const formatDate = (dateString) => {
    const [year, month, day] = dateString.split("-");

    return `${year}년 ${Number(month)}월 ${Number(day)}일`;
  };

  // --------------------------------------------------
  // 이전 / 다음 달
  // --------------------------------------------------

  const moveMonth = (amount) => {
    setCurrentDate(new Date(year, month + amount, 1));
    setSelectedDate(null);
  };

  // --------------------------------------------------
  // 오늘
  // --------------------------------------------------

  const goToday = () => {
    setCurrentDate(new Date());
    setSelectedDate(todayString);
  };

  // --------------------------------------------------
  // 캘린더 날짜 클릭
  // --------------------------------------------------

  const handleDateClick = (date) => {
    if (!date) {
      return;
    }

    setSelectedDate(date);

    const meeting = getMeetingByDate(date);

    if (meeting) {
      setSelectedMeeting(meeting);
      setShowMeetingDetail(false);
    } else {
      setSelectedMeeting(null);
    }
  };

  // --------------------------------------------------
  // 상세정보 보기
  // --------------------------------------------------

  const openMeetingDetail = () => {
    setShowMeetingDetail(true);
  };

  // --------------------------------------------------
  // 상세정보 닫기
  // --------------------------------------------------

  const closeMeetingDetail = () => {
    setShowMeetingDetail(false);
  };

  // --------------------------------------------------
  // 모임 팝업 닫기
  // --------------------------------------------------

  const closeMeetingPopup = () => {
    setSelectedMeeting(null);
    setShowMeetingDetail(false);
  };

  // --------------------------------------------------
  // 로그아웃
  // --------------------------------------------------

  const handleLogout = () => {
    localStorage.removeItem("user");
    localStorage.removeItem("isLoggedIn");

    navigate("/login");
  };

  // --------------------------------------------------
  // 투표 현황 계산
  // --------------------------------------------------

  const getVoteSummary = (meeting) => {
    const voteCounts = {};

    meeting.votes.forEach((vote) => {
      vote.dates.forEach((date) => {
        voteCounts[date] = (voteCounts[date] || 0) + 1;
      });
    });

    return voteCounts;
  };

  const getMaxVoteCount = (meeting) => {
    const voteCounts = getVoteSummary(meeting);

    const values = Object.values(voteCounts);

    if (values.length === 0) {
      return 0;
    }

    return Math.max(...values);
  };

  const getMostVotedDates = (meeting) => {
    const voteCounts = getVoteSummary(meeting);

    const maxCount = getMaxVoteCount(meeting);

    return Object.entries(voteCounts)
      .filter(([_, count]) => count === maxCount)
      .map(([date]) => date);
  };

  // ==================================================
  // RENDER
  // ==================================================

  return (
    <div
      style={{
        minHeight: "100vh",
        backgroundColor: "#F8FAFC",
        color: "#1E293B",
        fontFamily: "Arial, sans-serif",
      }}
    >

      {/* ==================================================
          HEADER
      ================================================== */}

      <AppHeader />


      {/* ==================================================
          MAIN
      ================================================== */}

      <main
        style={{
          maxWidth: "1280px",
          margin: "0 auto",
          padding: "34px 24px 60px",
        }}
      >

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "minmax(0, 1fr) 330px",
            gap: "28px",
            alignItems: "start",
          }}
        >

          {/* ==================================================
              LEFT : CALENDAR
          ================================================== */}

          <section
            style={{
              backgroundColor: "#FFFFFF",
              border: "1px solid #E8EDF3",
              borderRadius: "24px",
              padding: "28px",
              boxShadow: "0 3px 12px rgba(15,23,42,0.05)",
            }}
          >

            {/* 캘린더 제목 */}

            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "12px",
                marginBottom: "4px",
              }}
            >

              <div
                style={{
                  width: "42px",
                  height: "42px",
                  borderRadius: "12px",
                  backgroundColor: "#EEF5FF",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: "22px",
                }}
              >
                📅
              </div>

              <div>

                <h2
                  style={{
                    margin: 0,
                    fontSize: "25px",
                    fontWeight: "800",
                    color: "#172033",
                  }}
                >
                  개인 일정 캘린더
                </h2>

                <p
                  style={{
                    margin: "4px 0 0",
                    fontSize: "13px",
                    color: "#94A3B8",
                  }}
                >
                  확정된 모임 일정을 한눈에 확인할 수 있습니다.
                </p>

              </div>

            </div>


            {/* 달 이동 */}

            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginTop: "32px",
                marginBottom: "22px",
              }}
            >

              <button
                onClick={() => moveMonth(-1)}
                style={{
                  width: "52px",
                  height: "44px",
                  border: "none",
                  borderRadius: "12px",
                  backgroundColor: "#F1F5F9",
                  color: "#475569",
                  fontSize: "26px",
                  cursor: "pointer",
                }}
              >
                ‹
              </button>


              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "5px",
                }}
              >

                <h3
                  style={{
                    margin: 0,
                    fontSize: "24px",
                    fontWeight: "800",
                    color: "#172033",
                  }}
                >
                  {year}년 {month + 1}월
                </h3>

                <button
                  onClick={goToday}
                  style={{
                    border: "none",
                    backgroundColor: "#F1F5F9",
                    color: "#64748B",
                    borderRadius: "10px",
                    padding: "5px 12px",
                    fontSize: "12px",
                    fontWeight: "700",
                    cursor: "pointer",
                  }}
                >
                  today
                </button>

              </div>


              <button
                onClick={() => moveMonth(1)}
                style={{
                  width: "52px",
                  height: "44px",
                  border: "none",
                  borderRadius: "12px",
                  backgroundColor: "#F1F5F9",
                  color: "#475569",
                  fontSize: "26px",
                  cursor: "pointer",
                }}
              >
                ›
              </button>

            </div>


            {/* ==================================================
                요일
            ================================================== */}

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(7, 1fr)",
                borderTop: "1px solid #E6ECF2",
                borderLeft: "1px solid #E6ECF2",
              }}
            >

              {["일", "월", "화", "수", "목", "금", "토"].map(
                (day, index) => (
                  <div
                    key={day}
                    style={{
                      height: "48px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      backgroundColor: "#F8FAFC",
                      borderRight: "1px solid #E6ECF2",
                      borderBottom: "1px solid #E6ECF2",
                      fontSize: "14px",
                      fontWeight: "700",
                      color:
                        index === 0
                          ? "#EF4444"
                          : index === 6
                          ? "#3B82F6"
                          : "#64748B",
                    }}
                  >
                    {day}
                  </div>
                )
              )}

            </div>


            {/* ==================================================
                날짜
            ================================================== */}

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(7, minmax(0, 1fr))",
                gridAutoRows: "125px",
                borderLeft: "1px solid #E6ECF2",
              }}
            >

              {calendarDays.map((item, index) => {

                const isToday = item.date === todayString;
                const isSelected = item.date === selectedDate;

                const meeting = item.date
                  ? getMeetingByDate(item.date)
                  : null;

                return (
                  <div
                    key={index}
                    onClick={() => {
                      if (item.currentMonth) {
                        handleDateClick(item.date);
                      }
                    }}
                    style={{
                      height: "100%",
                      minWidth: 0,
                      boxSizing: "border-box",
                      padding: "10px",
                      borderRight: "1px solid #E6ECF2",
                      borderBottom: "1px solid #E6ECF2",
                      backgroundColor: !item.currentMonth
                        ? "#F1F3F5"
                        : isSelected
                        ? "#EEF5FF"
                        : "#FFFFFF",
                      cursor: item.currentMonth
                        ? "pointer"
                        : "default",
                    }}
                  >

                    {/* 날짜 */}

                    <div
                      style={{
                        display: "flex",
                        justifyContent: "flex-end",
                      }}
                    >

                      <span
                        style={{
                          width: "28px",
                          height: "28px",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          borderRadius: "50%",
                          backgroundColor: isToday
                            ? "#E0EDFF"
                            : "transparent",
                          color: !item.currentMonth
                            ? "#CBD5E1"
                            : isToday
                            ? "#2563EB"
                            : "#334155",
                          fontSize: "13px",
                          fontWeight: "700",
                        }}
                      >
                        {item.day}
                      </span>

                    </div>


                    {/* 확정 모임 표시 */}

                    {meeting && (
                      <div
                        style={{
                          marginTop: "12px",
                          padding: "7px 8px",
                          borderRadius: "8px",
                          backgroundColor: "#EEF5FF",
                          color: "#3B82F6",
                          fontSize: "11px",
                          fontWeight: "700",
                          display: "block",
                          maxWidth: "100%",
                          minWidth: 0,
                          overflow: "hidden",
                          whiteSpace: "nowrap",
                          textOverflow: "ellipsis",
                        }}
                      >
                        ● {meeting.title}
                      </div>
                    )}

                  </div>
                );
              })}

            </div>

          </section>


          {/* ==================================================
              RIGHT SIDEBAR
          ================================================== */}

          <aside
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "20px",
            }}
          >

            {/* ==================================================
                PROFILE
            ================================================== */}

            <section
              style={{
                backgroundColor: "#FFFFFF",
                border: "1px solid #E8EDF3",
                borderRadius: "24px",
                padding: "26px",
                boxShadow: "0 3px 12px rgba(15,23,42,0.05)",
              }}
            >

              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "14px",
                }}
              >

                <div
                  style={{
                    width: "60px",
                    height: "60px",
                    borderRadius: "50%",
                    backgroundColor: "#E8F1FF",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: "27px",
                  }}
                >
                  👤
                </div>

                <div>

                  <h3
                    style={{
                      margin: "0 0 6px",
                      fontSize: "18px",
                      fontWeight: "800",
                      color: "#172033",
                    }}
                  >
                    {userName}님
                  </h3>

                  <p
                    style={{
                      margin: 0,
                      fontSize: "13px",
                      color: "#94A3B8",
                    }}
                  >
                    {userAddress}
                  </p>

                </div>

              </div>

            </section>


            {/* ==================================================
                회원정보 / 로그아웃
            ================================================== */}

            <section
              style={{
                backgroundColor: "#FFFFFF",
                border: "1px solid #E8EDF3",
                borderRadius: "24px",
                padding: "16px",
                boxShadow: "0 3px 12px rgba(15,23,42,0.05)",
              }}
            >

              <button
                onClick={() => navigate("/mypage")}
                style={{
                  width: "100%",
                  border: "none",
                  backgroundColor: "#F1F5F9",
                  borderRadius: "14px",
                  padding: "14px",
                  fontSize: "14px",
                  fontWeight: "700",
                  color: "#475569",
                  cursor: "pointer",
                }}
              >
                회원정보 수정
              </button>


              <button
                onClick={handleLogout}
                style={{
                  width: "100%",
                  marginTop: "10px",
                  border: "none",
                  backgroundColor: "#FFF1F2",
                  borderRadius: "14px",
                  padding: "14px",
                  fontSize: "14px",
                  fontWeight: "700",
                  color: "#EF4444",
                  cursor: "pointer",
                }}
              >
                로그아웃
              </button>

            </section>


            {/* ==================================================
                내 모임 위치
            ================================================== */}

            <section
              style={{
                backgroundColor: "#FFFFFF",
                border: "1px solid #E8EDF3",
                borderRadius: "24px",
                padding: "22px",
                boxShadow: "0 3px 12px rgba(15,23,42,0.05)",
              }}
            >

              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "14px",
                }}
              >

                <h3
                  style={{
                    margin: 0,
                    fontSize: "16px",
                    fontWeight: "800",
                    color: "#172033",
                  }}
                >
                  📍 내 모임 위치
                </h3>

                <span
                  style={{
                    fontSize: "12px",
                    color: "#94A3B8",
                  }}
                >
                  {userAddress}
                </span>

              </div>


              {/* 지도 영역 */}

<div
  onClick={() => setShowMapModal(true)}
  style={{
    height: "260px",
    borderRadius: "18px",
    border: "1px solid #E5EAF0",
    background:
      "linear-gradient(135deg, #EDF3EA 0%, #F8F6ED 50%, #EAF1F5 100%)",
    position: "relative",
    overflow: "hidden",
    cursor: "pointer",
  }}
>
  {/* 지도 도로 */}

  <div
    style={{
      position: "absolute",
      width: "130%",
      height: "22px",
      backgroundColor: "#FFFFFF",
      top: "55%",
      left: "-15%",
      transform: "rotate(-12deg)",
      boxShadow: "0 0 0 1px #E5E7EB",
    }}
  />

  <div
    style={{
      position: "absolute",
      width: "120%",
      height: "17px",
      backgroundColor: "#FFFFFF",
      top: "22%",
      left: "-10%",
      transform: "rotate(20deg)",
      boxShadow: "0 0 0 1px #E5E7EB",
    }}
  />

  <div
    style={{
      position: "absolute",
      width: "15px",
      height: "130%",
      backgroundColor: "#FFFFFF",
      top: "-15%",
      left: "63%",
      transform: "rotate(15deg)",
      boxShadow: "0 0 0 1px #E5E7EB",
    }}
  />

  {/* 공원 */}

  <div
    style={{
      position: "absolute",
      width: "75px",
      height: "55px",
      borderRadius: "50%",
      backgroundColor: "#DCEBD8",
      left: "18px",
      bottom: "25px",
    }}
  />

  <div
    style={{
      position: "absolute",
      width: "55px",
      height: "55px",
      borderRadius: "50%",
      backgroundColor: "#DCEBD8",
      right: "18px",
      top: "20px",
    }}
  />

  {/* 내 위치 */}

  <div
    style={{
      position: "absolute",
      left: "27%",
      top: "60%",
      transform: "translate(-50%, -50%)",
    }}
  >
    <div
      style={{
        width: "18px",
        height: "18px",
        borderRadius: "50%",
        backgroundColor: "#4F7DF3",
        border: "4px solid #FFFFFF",
        boxShadow: "0 2px 8px rgba(15,23,42,0.2)",
      }}
    />

    <div
      style={{
        position: "absolute",
        top: "26px",
        left: "50%",
        transform: "translateX(-50%)",
        whiteSpace: "nowrap",
        padding: "4px 7px",
        borderRadius: "6px",
        backgroundColor: "#FFFFFF",
        color: "#64748B",
        fontSize: "9px",
        fontWeight: "700",
        boxShadow: "0 2px 6px rgba(15,23,42,0.08)",
      }}
    >
      내 위치
    </div>
  </div>

  {/* 가장 가까운 모임 핀 */}

  {nextMeeting && (
    <div
      style={{
        position: "absolute",
        right: "25%",
        top: "38%",
        transform: "translate(50%, -50%)",
      }}
    >
      {/* 모임 제목 */}

      <div
        style={{
          position: "absolute",
          bottom: "34px",
          left: "50%",
          transform: "translateX(-50%)",
          whiteSpace: "nowrap",
          padding: "6px 9px",
          borderRadius: "8px",
          backgroundColor: "#4F7DF3",
          color: "#FFFFFF",
          fontSize: "10px",
          fontWeight: "700",
          boxShadow: "0 3px 8px rgba(59,130,246,0.25)",
        }}
      >
        {nextMeeting.title}
      </div>

      {/* 핀 */}

      <div
        style={{
          width: "30px",
          height: "30px",
          borderRadius: "50% 50% 50% 0",
          backgroundColor: "#4F7DF3",
          transform: "rotate(-45deg)",
          border: "3px solid #FFFFFF",
          boxShadow: "0 3px 8px rgba(15,23,42,0.18)",
          position: "relative",
        }}
      >
        <div
          style={{
            width: "8px",
            height: "8px",
            borderRadius: "50%",
            backgroundColor: "#FFFFFF",
            position: "absolute",
            top: "8px",
            left: "8px",
          }}
        />
      </div>
    </div>
  )}

  {/* 지도 클릭 안내 */}

  <div
    style={{
      position: "absolute",
      bottom: "12px",
      left: "50%",
      transform: "translateX(-50%)",
      whiteSpace: "nowrap",
      padding: "6px 10px",
      borderRadius: "12px",
      backgroundColor: "rgba(255,255,255,0.94)",
      color: "#64748B",
      fontSize: "10px",
      boxShadow: "0 2px 8px rgba(15,23,42,0.08)",
    }}
  >
    지도를 클릭하면 크게 볼 수 있어요
  </div>
</div>

{/* 가장 가까운 모임 */}

{nextMeeting && (
  <div
    style={{
      marginTop: "14px",
      padding: "13px 14px",
      borderRadius: "12px",
      backgroundColor: "#F8FAFC",
      border: "1px solid #EEF2F6",
    }}
  >
    <div
      style={{
        fontSize: "12px",
        color: "#94A3B8",
        marginBottom: "5px",
      }}
    >
      가장 가까운 모임
    </div>

    <div
      style={{
        fontSize: "14px",
        fontWeight: "800",
        color: "#172033",
      }}
    >
      {nextMeeting.title}
    </div>

    <div
      style={{
        marginTop: "5px",
        fontSize: "11px",
        color: "#64748B",
      }}
    >
      {formatDate(nextMeeting.date)} · {nextMeeting.time}
    </div>

    <div
      style={{
        marginTop: "3px",
        fontSize: "11px",
        color: "#64748B",
      }}
    >
      📍 {nextMeeting.place}
    </div>
  </div>
)}


            </section>

          </aside>

        </div>

      </main>


      {/* ==================================================
          모임 간단 정보 팝업
      ================================================== */}

      {selectedMeeting && !showMeetingDetail && (
        <div
          onClick={closeMeetingPopup}
          style={{
            position: "fixed",
            inset: 0,
            backgroundColor: "rgba(15,23,42,0.35)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            padding: "20px",
          }}
        >

          <div
            onClick={(event) => event.stopPropagation()}
            style={{
              width: "100%",
              maxWidth: "460px",
              backgroundColor: "#FFFFFF",
              borderRadius: "24px",
              padding: "28px",
              boxShadow: "0 20px 50px rgba(15,23,42,0.18)",
            }}
          >

            {/* 제목 */}

            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "12px",
                marginBottom: "24px",
              }}
            >

              <div
                style={{
                  width: "46px",
                  height: "46px",
                  borderRadius: "14px",
                  backgroundColor: "#EEF5FF",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: "22px",
                }}
              >
                📌
              </div>

              <div>

                <h2
                  style={{
                    margin: 0,
                    fontSize: "20px",
                    fontWeight: "800",
                    color: "#172033",
                  }}
                >
                  {selectedMeeting.title}
                </h2>

                <p
                  style={{
                    margin: "4px 0 0",
                    fontSize: "12px",
                    color: "#94A3B8",
                  }}
                >
                  확정된 모임
                </p>

              </div>

            </div>


            {/* 모임 정보 */}

            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "10px",
              }}
            >

              <div
                style={{
                  padding: "13px 15px",
                  backgroundColor: "#F8FAFC",
                  borderRadius: "12px",
                  fontSize: "13px",
                  color: "#475569",
                }}
              >
                📅 {formatDate(selectedMeeting.date)}
              </div>

              <div
                style={{
                  padding: "13px 15px",
                  backgroundColor: "#F8FAFC",
                  borderRadius: "12px",
                  fontSize: "13px",
                  color: "#475569",
                }}
              >
                🕐 {selectedMeeting.time}
              </div>

              <div
                style={{
                  padding: "13px 15px",
                  backgroundColor: "#F8FAFC",
                  borderRadius: "12px",
                  fontSize: "13px",
                  color: "#475569",
                }}
              >
                📍 {selectedMeeting.location}
              </div>

              <div
                style={{
                  padding: "13px 15px",
                  backgroundColor: "#F8FAFC",
                  borderRadius: "12px",
                  fontSize: "13px",
                  color: "#475569",
                }}
              >
                ☕ {selectedMeeting.place}
              </div>

              <div
                style={{
                  padding: "13px 15px",
                  backgroundColor: "#F8FAFC",
                  borderRadius: "12px",
                  fontSize: "13px",
                  color: "#475569",
                }}
              >
                👥 참여 인원 {selectedMeeting.members.length}명
              </div>

            </div>


            {/* 설명 */}

            <div
              style={{
                marginTop: "18px",
                padding: "15px",
                backgroundColor: "#F8FAFC",
                borderRadius: "14px",
              }}
            >

              <p
                style={{
                  margin: 0,
                  fontSize: "13px",
                  lineHeight: "1.6",
                  color: "#64748B",
                }}
              >
                {selectedMeeting.description}
              </p>

            </div>


            {/* 버튼 */}

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1.5fr",
                gap: "10px",
                marginTop: "24px",
              }}
            >

              <button
                onClick={closeMeetingPopup}
                style={{
                  border: "none",
                  backgroundColor: "#F1F5F9",
                  color: "#475569",
                  borderRadius: "13px",
                  padding: "13px",
                  fontSize: "13px",
                  fontWeight: "700",
                  cursor: "pointer",
                }}
              >
                닫기
              </button>

              <button
                onClick={openMeetingDetail}
                style={{
                  border: "none",
                  backgroundColor: "#4F7DF3",
                  color: "#FFFFFF",
                  borderRadius: "13px",
                  padding: "13px",
                  fontSize: "13px",
                  fontWeight: "700",
                  cursor: "pointer",
                }}
              >
                상세정보 보러가기
              </button>

            </div>

          </div>

        </div>
      )}


      {/* ==================================================
          모임 상세정보 / 투표 현황
      ================================================== */}

      {selectedMeeting && showMeetingDetail && (
        <div
          onClick={closeMeetingDetail}
          style={{
            position: "fixed",
            inset: 0,
            backgroundColor: "rgba(15,23,42,0.35)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            padding: "20px",
            overflowY: "auto",
          }}
        >

          <div
            onClick={(event) => event.stopPropagation()}
            style={{
              width: "100%",
              maxWidth: "650px",
              maxHeight: "90vh",
              overflowY: "auto",
              backgroundColor: "#FFFFFF",
              borderRadius: "24px",
              padding: "30px",
              boxShadow: "0 20px 50px rgba(15,23,42,0.18)",
            }}
          >

            {/* 헤더 */}

            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "flex-start",
                marginBottom: "26px",
              }}
            >

              <div>

                <p
                  style={{
                    margin: "0 0 6px",
                    fontSize: "12px",
                    color: "#3B82F6",
                    fontWeight: "700",
                  }}
                >
                  모임 상세정보
                </p>

                <h2
                  style={{
                    margin: 0,
                    fontSize: "22px",
                    fontWeight: "800",
                    color: "#172033",
                  }}
                >
                  {selectedMeeting.title}
                </h2>

              </div>

              <button
                onClick={closeMeetingDetail}
                style={{
                  width: "36px",
                  height: "36px",
                  border: "none",
                  borderRadius: "10px",
                  backgroundColor: "#F1F5F9",
                  color: "#64748B",
                  fontSize: "18px",
                  cursor: "pointer",
                }}
              >
                ×
              </button>

            </div>

            {/* ==================================================
                최다 투표 날짜
            ================================================== */}

            <div
              style={{
                padding: "18px",
                borderRadius: "16px",
                backgroundColor: "#EEF5FF",
                marginBottom: "24px",
              }}
            >

              <p
                style={{
                  margin: "0 0 7px",
                  fontSize: "12px",
                  color: "#3B82F6",
                  fontWeight: "700",
                }}
              >
                🏆 현재 가장 많은 투표를 받은 날짜
              </p>

              <div
                style={{
                  fontSize: "18px",
                  fontWeight: "800",
                  color: "#172033",
                }}
              >
                {getMostVotedDates(selectedMeeting)
                  .map((date) => formatDate(date))
                  .join(", ")}
              </div>

              <p
                style={{
                  margin: "6px 0 0",
                  fontSize: "12px",
                  color: "#64748B",
                }}
              >
                총 {getMaxVoteCount(selectedMeeting)}명이 선택했습니다.
              </p>

            </div>


            {/* ==================================================
                날짜별 투표 현황
            ================================================== */}

            <section>

              <h3
                style={{
                  margin: "0 0 14px",
                  fontSize: "16px",
                  fontWeight: "800",
                  color: "#172033",
                }}
              >
                📊 날짜별 투표 현황
              </h3>


              {Object.entries(getVoteSummary(selectedMeeting)).map(
                ([date, count]) => {

                  const maxVote = getMaxVoteCount(selectedMeeting);

                  const percentage =
                    maxVote === 0
                      ? 0
                      : (count / maxVote) * 100;

                  const isMostVoted = count === maxVote;

                  return (
                    <div
                      key={date}
                      style={{
                        marginBottom: "16px",
                      }}
                    >

                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                          marginBottom: "7px",
                        }}
                      >

                        <span
                          style={{
                            fontSize: "13px",
                            fontWeight: "700",
                            color: "#334155",
                          }}
                        >
                          {formatDate(date)}
                        </span>

                        <span
                          style={{
                            fontSize: "12px",
                            color: isMostVoted
                              ? "#3B82F6"
                              : "#94A3B8",
                            fontWeight: "700",
                          }}
                        >
                          {count}표
                        </span>

                      </div>


                      <div
                        style={{
                          width: "100%",
                          height: "12px",
                          backgroundColor: "#EEF2F7",
                          borderRadius: "20px",
                          overflow: "hidden",
                        }}
                      >

                        <div
                          style={{
                            width: `${percentage}%`,
                            height: "100%",
                            backgroundColor: isMostVoted
                              ? "#3B82F6"
                              : "#93C5FD",
                            borderRadius: "20px",
                            transition: "width 0.3s",
                          }}
                        />

                      </div>

                    </div>
                  );
                }
              )}

            </section>


            {/* ==================================================
                사람별 투표 현황
            ================================================== */}

            <section
              style={{
                marginTop: "28px",
              }}
            >

              <h3
                style={{
                  margin: "0 0 14px",
                  fontSize: "16px",
                  fontWeight: "800",
                  color: "#172033",
                }}
              >
                👥 멤버별 투표 현황
              </h3>


              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                }}
              >

                {selectedMeeting.votes.map((vote) => {

                  const hasVoted = vote.dates.length > 0;

                  return (
                    <div
                      key={vote.member}
                      style={{
                        padding: "14px",
                        borderRadius: "14px",
                        backgroundColor: "#F8FAFC",
                        border: "1px solid #E8EDF3",
                      }}
                    >

                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                          marginBottom: hasVoted
                            ? "8px"
                            : "0",
                        }}
                      >

                        <span
                          style={{
                            fontSize: "13px",
                            fontWeight: "700",
                            color: "#334155",
                          }}
                        >
                          {vote.member}
                        </span>

                        {!hasVoted && (
                          <span
                            style={{
                              fontSize: "11px",
                              color: "#EF4444",
                              fontWeight: "700",
                            }}
                          >
                            미투표
                          </span>
                        )}

                      </div>


                      {hasVoted && (
                        <div
                          style={{
                            display: "flex",
                            gap: "6px",
                            flexWrap: "wrap",
                          }}
                        >

                          {vote.dates.map((date) => (
                            <span
                              key={date}
                              style={{
                                padding: "5px 8px",
                                borderRadius: "8px",
                                backgroundColor: "#E8F1FF",
                                color: "#3B82F6",
                                fontSize: "11px",
                                fontWeight: "600",
                              }}
                            >
                              {formatDate(date)}
                            </span>
                          ))}

                        </div>
                      )}

                    </div>
                  );
                })}

              </div>

            </section>


            {/* 닫기 버튼 */}

            <button
              onClick={closeMeetingDetail}
              style={{
                width: "100%",
                marginTop: "26px",
                border: "none",
                backgroundColor: "#F1F5F9",
                color: "#475569",
                borderRadius: "13px",
                padding: "13px",
                fontSize: "13px",
                fontWeight: "700",
                cursor: "pointer",
              }}
            >
              닫기
            </button>

          </div>

        </div>
      )}


            {/* ==================================================
    지도 확대 팝업
================================================== */}

{showMapModal && (
  <div
    onClick={() => setShowMapModal(false)}
    style={{
      position: "fixed",
      inset: 0,
      backgroundColor: "rgba(15,23,42,0.45)",
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      zIndex: 1100,
      padding: "20px",
    }}
  >
    <div
      onClick={(event) => event.stopPropagation()}
      style={{
        width: "100%",
        maxWidth: "850px",
        height: "650px",
        maxHeight: "90vh",
        backgroundColor: "#FFFFFF",
        borderRadius: "24px",
        overflow: "hidden",
        boxShadow: "0 20px 50px rgba(15,23,42,0.2)",
        display: "flex",
        flexDirection: "column",
      }}
    >
      {/* 지도 팝업 헤더 */}

      <div
        style={{
          height: "72px",
          padding: "0 24px",
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          borderBottom: "1px solid #E8EDF3",
          flexShrink: 0,
        }}
      >
        <div>
          <div
            style={{
              fontSize: "18px",
              fontWeight: "800",
              color: "#172033",
            }}
          >
            내 모임 위치
          </div>

          {nextMeeting && (
            <div
              style={{
                marginTop: "4px",
                fontSize: "12px",
                color: "#94A3B8",
              }}
            >
              {nextMeeting.title}
            </div>
          )}
        </div>

        <button
          onClick={() => setShowMapModal(false)}
          style={{
            width: "36px",
            height: "36px",
            border: "none",
            borderRadius: "10px",
            backgroundColor: "#F1F5F9",
            color: "#64748B",
            fontSize: "18px",
            cursor: "pointer",
          }}
        >
          ×
        </button>
      </div>

      {/* 큰 지도 */}

      <div
        style={{
          flex: 1,
          position: "relative",
          overflow: "hidden",
          background:
            "linear-gradient(135deg, #EDF3EA 0%, #F8F6ED 50%, #EAF1F5 100%)",
        }}
      >
        {/* 도로 */}

        <div
          style={{
            position: "absolute",
            width: "130%",
            height: "45px",
            backgroundColor: "#FFFFFF",
            top: "45%",
            left: "-15%",
            transform: "rotate(-8deg)",
            boxShadow: "0 0 0 1px #E5E7EB",
          }}
        />

        <div
          style={{
            position: "absolute",
            width: "130%",
            height: "30px",
            backgroundColor: "#FFFFFF",
            top: "17%",
            left: "-15%",
            transform: "rotate(18deg)",
            boxShadow: "0 0 0 1px #E5E7EB",
          }}
        />

        <div
          style={{
            position: "absolute",
            width: "28px",
            height: "130%",
            backgroundColor: "#FFFFFF",
            top: "-15%",
            left: "57%",
            transform: "rotate(18deg)",
            boxShadow: "0 0 0 1px #E5E7EB",
          }}
        />

        <div
          style={{
            position: "absolute",
            width: "24px",
            height: "130%",
            backgroundColor: "#FFFFFF",
            top: "-15%",
            left: "23%",
            transform: "rotate(-23deg)",
            boxShadow: "0 0 0 1px #E5E7EB",
          }}
        />

        {/* 공원 */}

        <div
          style={{
            position: "absolute",
            width: "150px",
            height: "100px",
            borderRadius: "50%",
            backgroundColor: "#DCEBD8",
            left: "6%",
            bottom: "12%",
          }}
        />

        <div
          style={{
            position: "absolute",
            width: "110px",
            height: "110px",
            borderRadius: "50%",
            backgroundColor: "#DCEBD8",
            right: "8%",
            top: "10%",
          }}
        />

        {/* 내 위치 */}

        <div
          style={{
            position: "absolute",
            left: "28%",
            top: "56%",
            transform: "translate(-50%, -50%)",
          }}
        >
          <div
            style={{
              width: "28px",
              height: "28px",
              borderRadius: "50%",
              backgroundColor: "#4F7DF3",
              border: "5px solid #FFFFFF",
              boxShadow:
                "0 4px 12px rgba(15,23,42,0.2)",
            }}
          />

          <div
            style={{
              position: "absolute",
              top: "42px",
              left: "50%",
              transform: "translateX(-50%)",
              whiteSpace: "nowrap",
              padding: "6px 10px",
              borderRadius: "8px",
              backgroundColor: "#FFFFFF",
              color: "#64748B",
              fontSize: "11px",
              fontWeight: "700",
              boxShadow:
                "0 2px 8px rgba(15,23,42,0.1)",
            }}
          >
            내 위치
          </div>
        </div>

        {/* 모임 위치 */}

        {nextMeeting && (
          <div
            style={{
              position: "absolute",
              right: "28%",
              top: "38%",
              transform: "translate(50%, -50%)",
            }}
          >
            {/* 모임 제목 */}

            <div
              style={{
                position: "absolute",
                bottom: "48px",
                left: "50%",
                transform: "translateX(-50%)",
                whiteSpace: "nowrap",
                padding: "8px 12px",
                borderRadius: "9px",
                backgroundColor: "#4F7DF3",
                color: "#FFFFFF",
                fontSize: "12px",
                fontWeight: "800",
                boxShadow:
                  "0 4px 12px rgba(59,130,246,0.25)",
              }}
            >
              {nextMeeting.title}
            </div>

            {/* 핀 */}

            <div
              style={{
                width: "42px",
                height: "42px",
                borderRadius: "50% 50% 50% 0",
                backgroundColor: "#4F7DF3",
                transform: "rotate(-45deg)",
                border: "4px solid #FFFFFF",
                boxShadow:
                  "0 4px 12px rgba(15,23,42,0.2)",
                position: "relative",
              }}
            >
              <div
                style={{
                  width: "11px",
                  height: "11px",
                  borderRadius: "50%",
                  backgroundColor: "#FFFFFF",
                  position: "absolute",
                  top: "11px",
                  left: "11px",
                }}
              />
            </div>
          </div>
        )}

        {/* 하단 모임 정보 */}

        {nextMeeting && (
          <div
            style={{
              position: "absolute",
              left: "24px",
              right: "24px",
              bottom: "24px",
              backgroundColor: "rgba(255,255,255,0.96)",
              borderRadius: "16px",
              padding: "16px 18px",
              boxShadow:
                "0 4px 15px rgba(15,23,42,0.1)",
            }}
          >
            <div
              style={{
                fontSize: "14px",
                fontWeight: "800",
                color: "#172033",
              }}
            >
              {nextMeeting.title}
            </div>

            <div
              style={{
                marginTop: "5px",
                fontSize: "12px",
                color: "#64748B",
              }}
            >
              📅 {formatDate(nextMeeting.date)} ·{" "}
              {nextMeeting.time}
            </div>

            <div
              style={{
                marginTop: "3px",
                fontSize: "12px",
                color: "#64748B",
              }}
            >
              📍 {nextMeeting.location} ·{" "}
              {nextMeeting.place}
            </div>
          </div>
        )}
      </div>
    </div>
  </div>
)}

    </div>
  );
}

export default MainPage;
