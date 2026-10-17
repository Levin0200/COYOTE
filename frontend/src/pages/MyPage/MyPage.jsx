import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import AppHeader from "../../components/AppHeader";
import { deleteMe, fetchMe, logout, updateMe } from "../../services/authService";
import { getErrorMessage } from "../../services/api";
import { fetchMeetingVotes, fetchMyMeetings } from "../../services/meetingService";
import { getMeetingErrorMessage } from "../../utils/meetingErrors";
import { placeSummaryText } from "../../utils/placeStatus";

function MainPage() {
  const navigate = useNavigate();

  // --------------------------------------------------
  // 기본 상태
  // --------------------------------------------------

  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(null);

  // 같은 날 모임이 3개 이상일 때 펼쳐 둔 날짜
  const [expandedDate, setExpandedDate] = useState(null);

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

  const [user, setUser] = useState(null);
  const [userLoadFailed, setUserLoadFailed] = useState(false);
  const [profileEditOpen, setProfileEditOpen] = useState(false);
  const [profileName, setProfileName] = useState("");
  const [profileAddress, setProfileAddress] = useState("");
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileError, setProfileError] = useState("");
  const [withdrawOpen, setWithdrawOpen] = useState(false);
  const [withdrawConfirmText, setWithdrawConfirmText] = useState("");
  const [withdrawPassword, setWithdrawPassword] = useState("");
  const [withdrawing, setWithdrawing] = useState(false);
  const [withdrawError, setWithdrawError] = useState("");
  const [withdrawComplete, setWithdrawComplete] = useState(false);

  useEffect(() => {
    let cancelled = false;

    fetchMe()
      .then((me) => {
        if (!cancelled) setUser(me);
      })
      .catch(() => {
        if (!cancelled) setUserLoadFailed(true);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const userName = user
    ? user.name
    : userLoadFailed
      ? "회원"
      : "불러오는 중...";
  const userAddress = user
    ? user.address || "주소 미등록"
    : userLoadFailed
      ? "정보를 불러오지 못했습니다"
      : "";

  const openProfileEdit = () => {
    if (!user) return;
    setProfileName(user.name || "");
    setProfileAddress(user.address || "");
    setProfileError("");
    setProfileEditOpen(true);
  };

  const closeProfileEdit = () => {
    if (profileSaving) return;
    setProfileEditOpen(false);
    setProfileError("");
  };

  const saveProfile = async (event) => {
    event.preventDefault();

    const name = profileName.trim();
    const address = profileAddress.trim();

    if (!name) {
      setProfileError("이름을 입력해주세요.");
      return;
    }
    if (name.length > 50) {
      setProfileError("이름은 50자 이하로 입력해주세요.");
      return;
    }
    if (address.length > 255) {
      setProfileError("주소는 255자 이하로 입력해주세요.");
      return;
    }

    setProfileSaving(true);
    setProfileError("");

    try {
      const updatedUser = await updateMe({ name, address });
      setUser(updatedUser);
      setUserLoadFailed(false);
      setProfileEditOpen(false);
    } catch (error) {
      setProfileError(getErrorMessage(error));
    } finally {
      setProfileSaving(false);
    }
  };

  const openWithdraw = () => {
    setWithdrawConfirmText("");
    setWithdrawPassword("");
    setWithdrawError("");
    setWithdrawOpen(true);
  };

  const closeWithdraw = () => {
    if (withdrawing) return;
    setWithdrawOpen(false);
    setWithdrawConfirmText("");
    setWithdrawPassword("");
    setWithdrawError("");
  };

  const canWithdraw = withdrawConfirmText.trim() === "탈퇴" && withdrawPassword !== "";

  const withdrawAccount = async (event) => {
    event.preventDefault();
    if (withdrawConfirmText.trim() !== "탈퇴") {
      setWithdrawError("확인을 위해 ‘탈퇴’를 정확히 입력해주세요.");
      return;
    }
    if (withdrawPassword === "") {
      setWithdrawError("비밀번호를 입력해주세요.");
      return;
    }

    const confirmed = window.confirm(
      "정말로 회원 탈퇴하시겠습니까?\n탈퇴한 계정 정보는 복구할 수 없습니다."
    );
    if (!confirmed) return;

    setWithdrawing(true);
    setWithdrawError("");

    try {
      await deleteMe(withdrawPassword);
      logout();
      setWithdrawOpen(false);
      setWithdrawComplete(true);
    } catch (error) {
      setWithdrawError(getErrorMessage(error));
    } finally {
      setWithdrawing(false);
    }
  };

  // --------------------------------------------------
  // 확정된 모임 (서버)
  // 마감되어 결과가 있는 내 모임만 캘린더에 표시한다.
  // --------------------------------------------------

  const [meetings, setMeetings] = useState([]);
  const [meetingsLoading, setMeetingsLoading] = useState(true);
  const [meetingsError, setMeetingsError] = useState("");
  const [meetingsReloadKey, setMeetingsReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    fetchMyMeetings()
      .then((list) => {
        if (cancelled) return;
        setMeetings(list);
        setMeetingsError("");
      })
      .catch((error) => {
        if (!cancelled) setMeetingsError(getMeetingErrorMessage(error));
      })
      .finally(() => {
        if (!cancelled) setMeetingsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [meetingsReloadKey]);

  const reloadMeetings = () => {
    setMeetingsLoading(true);
    setMeetingsError("");
    setMeetingsReloadKey((value) => value + 1);
  };

  const confirmedMeetings = meetings
    .filter((meeting) => meeting.result)
    .map((meeting) => ({
      id: meeting.meetingId,
      date: meeting.result.selectedDate,
      title: meeting.title,
      description: meeting.description || "",
      location: meeting.placeRegion || "",
      // 장소가 아직 정해지지 않았으면 상태 문구("장소 선택 대기 중" 등)를 보여 준다.
      place: placeSummaryText(meeting.result),
      memberCount: meeting.participantCount,
    }));

  // 투표 상세(상세정보 팝업) 조회 상태
  const [votesState, setVotesState] = useState({ meetingId: null, loading: false, error: "", data: null });

  const loadVotes = (meetingId) => {
    setVotesState({ meetingId, loading: true, error: "", data: null });

    fetchMeetingVotes(meetingId)
      .then((data) => {
        setVotesState((current) => (current.meetingId === meetingId ? { meetingId, loading: false, error: "", data } : current));
      })
      .catch((error) => {
        setVotesState((current) => (current.meetingId === meetingId ? { meetingId, loading: false, error: getMeetingErrorMessage(error), data: null } : current));
      });
  };

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

  const getMeetingsByDate = (date) => {
    return confirmedMeetings.filter((meeting) => meeting.date === date);
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

    const [meeting] = getMeetingsByDate(date);

    if (meeting) {
      setSelectedMeeting(meeting);
      setShowMeetingDetail(false);
    } else {
      setSelectedMeeting(null);
    }
  };

  // --------------------------------------------------
  // 캘린더의 모임 칩 클릭
  // --------------------------------------------------

  const openMeetingFromChip = (meeting) => {
    setSelectedDate(meeting.date);
    setSelectedMeeting(meeting);
    setShowMeetingDetail(false);
  };

  // --------------------------------------------------
  // 상세정보 보기
  // --------------------------------------------------

  const openMeetingDetail = () => {
    setShowMeetingDetail(true);
    loadVotes(selectedMeeting.id);
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
    logout();

    navigate("/login");
  };

  // --------------------------------------------------
  // 투표 현황 계산
  // --------------------------------------------------

  const getVoteSummary = (votes) => {
    const voteCounts = {};

    (votes?.dateCounts || []).forEach(({ date, count }) => {
      voteCounts[date] = count;
    });

    return voteCounts;
  };

  const getMaxVoteCount = (votes) => {
    const voteCounts = getVoteSummary(votes);

    const values = Object.values(voteCounts);

    if (values.length === 0) {
      return 0;
    }

    return Math.max(...values);
  };

  const getMostVotedDates = (votes) => {
    const voteCounts = getVoteSummary(votes);

    const maxCount = getMaxVoteCount(votes);

    return Object.entries(voteCounts)
      .filter(([, count]) => count === maxCount)
      .map(([date]) => date);
  };

  // 현재 열린 상세정보 팝업에 해당하는 투표 조회 상태
  const votesView =
    selectedMeeting && votesState.meetingId === selectedMeeting.id
      ? votesState
      : null;

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

      {profileEditOpen && (
        <div
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeProfileEdit();
          }}
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 1000,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: "24px",
            backgroundColor: "rgba(15, 23, 42, 0.48)",
          }}
        >
          <form
            onSubmit={saveProfile}
            role="dialog"
            aria-modal="true"
            aria-labelledby="profile-edit-title"
            style={{
              width: "100%",
              maxWidth: "460px",
              borderRadius: "24px",
              backgroundColor: "#FFFFFF",
              padding: "28px",
              boxShadow: "0 24px 70px rgba(15, 23, 42, 0.24)",
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", gap: "16px" }}>
              <div>
                <p style={{ margin: 0, color: "#4F7DF3", fontSize: "13px", fontWeight: 800 }}>
                  PROFILE
                </p>
                <h2 id="profile-edit-title" style={{ margin: "6px 0 0", color: "#172033", fontSize: "26px" }}>
                  회원정보 수정
                </h2>
              </div>
              <button
                type="button"
                onClick={closeProfileEdit}
                disabled={profileSaving}
                aria-label="회원정보 수정 닫기"
                style={{
                  width: "38px",
                  height: "38px",
                  border: "none",
                  borderRadius: "12px",
                  backgroundColor: "#F1F5F9",
                  color: "#64748B",
                  cursor: profileSaving ? "not-allowed" : "pointer",
                  fontSize: "20px",
                }}
              >
                ×
              </button>
            </div>

            <label style={{ display: "block", marginTop: "24px", color: "#475569", fontSize: "13px", fontWeight: 700 }}>
              이메일
              <input
                value={user?.email || ""}
                readOnly
                style={{
                  width: "100%",
                  boxSizing: "border-box",
                  marginTop: "8px",
                  border: "1px solid #E2E8F0",
                  borderRadius: "14px",
                  padding: "13px 14px",
                  backgroundColor: "#F8FAFC",
                  color: "#94A3B8",
                }}
              />
            </label>

            <label style={{ display: "block", marginTop: "18px", color: "#475569", fontSize: "13px", fontWeight: 700 }}>
              이름 <span style={{ color: "#EF4444" }}>*</span>
              <input
                value={profileName}
                onChange={(event) => setProfileName(event.target.value)}
                maxLength={50}
                autoFocus
                disabled={profileSaving}
                style={{
                  width: "100%",
                  boxSizing: "border-box",
                  marginTop: "8px",
                  border: "1px solid #CBD5E1",
                  borderRadius: "14px",
                  padding: "13px 14px",
                  color: "#172033",
                  outlineColor: "#4F7DF3",
                }}
              />
            </label>

            <label style={{ display: "block", marginTop: "18px", color: "#475569", fontSize: "13px", fontWeight: 700 }}>
              주소 <span style={{ color: "#94A3B8", fontWeight: 500 }}>(선택)</span>
              <input
                value={profileAddress}
                onChange={(event) => setProfileAddress(event.target.value)}
                maxLength={255}
                disabled={profileSaving}
                placeholder="예) 인천광역시 동구"
                style={{
                  width: "100%",
                  boxSizing: "border-box",
                  marginTop: "8px",
                  border: "1px solid #CBD5E1",
                  borderRadius: "14px",
                  padding: "13px 14px",
                  color: "#172033",
                  outlineColor: "#4F7DF3",
                }}
              />
            </label>

            {profileError && (
              <p
                role="alert"
                style={{
                  margin: "16px 0 0",
                  borderRadius: "12px",
                  padding: "11px 13px",
                  backgroundColor: "#FFF1F2",
                  color: "#E11D48",
                  fontSize: "13px",
                  fontWeight: 700,
                }}
              >
                {profileError}
              </p>
            )}

            <div style={{ display: "flex", gap: "10px", marginTop: "24px" }}>
              <button
                type="button"
                onClick={closeProfileEdit}
                disabled={profileSaving}
                style={{
                  flex: 1,
                  border: "1px solid #E2E8F0",
                  borderRadius: "14px",
                  padding: "13px",
                  backgroundColor: "#FFFFFF",
                  color: "#475569",
                  fontWeight: 800,
                  cursor: profileSaving ? "not-allowed" : "pointer",
                }}
              >
                취소
              </button>
              <button
                type="submit"
                disabled={profileSaving}
                style={{
                  flex: 2,
                  border: "none",
                  borderRadius: "14px",
                  padding: "13px",
                  backgroundColor: profileSaving ? "#94A3B8" : "#4F7DF3",
                  color: "#FFFFFF",
                  fontWeight: 800,
                  cursor: profileSaving ? "wait" : "pointer",
                }}
              >
                {profileSaving ? "저장 중..." : "변경사항 저장"}
              </button>
            </div>
          </form>
        </div>
      )}

      {withdrawOpen && (
        <div
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeWithdraw();
          }}
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 1000,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: "24px",
            backgroundColor: "rgba(15, 23, 42, 0.48)",
          }}
        >
          <form
            onSubmit={withdrawAccount}
            role="dialog"
            aria-modal="true"
            aria-labelledby="withdraw-title"
            style={{
              width: "100%",
              maxWidth: "430px",
              borderRadius: "24px",
              backgroundColor: "#FFFFFF",
              padding: "28px",
              boxShadow: "0 24px 70px rgba(15, 23, 42, 0.24)",
            }}
          >
            <p style={{ margin: 0, color: "#94A3B8", fontSize: "12px", fontWeight: 800 }}>
              ACCOUNT
            </p>
            <h2 id="withdraw-title" style={{ margin: "7px 0 0", color: "#172033", fontSize: "25px" }}>
              회원 탈퇴
            </h2>
            <p style={{ margin: "14px 0 0", color: "#64748B", fontSize: "14px", lineHeight: 1.7 }}>
              탈퇴하면 계정 정보와 직접 만든 모임, 참여 응답 기록이 모두 삭제되며 복구할 수 없습니다.
            </p>

            <label style={{ display: "block", marginTop: "22px", color: "#475569", fontSize: "13px", fontWeight: 700 }}>
              계속하려면 <strong style={{ color: "#E11D48" }}>탈퇴</strong>를 입력해주세요.
              <input
                value={withdrawConfirmText}
                onChange={(event) => setWithdrawConfirmText(event.target.value)}
                disabled={withdrawing}
                autoFocus
                autoComplete="off"
                placeholder="탈퇴"
                style={{
                  width: "100%",
                  boxSizing: "border-box",
                  marginTop: "9px",
                  border: "1px solid #CBD5E1",
                  borderRadius: "14px",
                  padding: "13px 14px",
                  color: "#172033",
                  outlineColor: "#E11D48",
                }}
              />
            </label>

            <label style={{ display: "block", marginTop: "16px", color: "#475569", fontSize: "13px", fontWeight: 700 }}>
              본인 확인을 위해 현재 비밀번호를 입력해주세요.
              <input
                type="password"
                value={withdrawPassword}
                onChange={(event) => setWithdrawPassword(event.target.value)}
                disabled={withdrawing}
                autoComplete="current-password"
                placeholder="비밀번호"
                style={{
                  width: "100%",
                  boxSizing: "border-box",
                  marginTop: "9px",
                  border: "1px solid #CBD5E1",
                  borderRadius: "14px",
                  padding: "13px 14px",
                  color: "#172033",
                  outlineColor: "#E11D48",
                }}
              />
            </label>

            {withdrawError && (
              <p
                role="alert"
                style={{
                  margin: "15px 0 0",
                  borderRadius: "12px",
                  padding: "11px 13px",
                  backgroundColor: "#FFF1F2",
                  color: "#E11D48",
                  fontSize: "13px",
                  fontWeight: 700,
                }}
              >
                {withdrawError}
              </p>
            )}

            <div style={{ display: "flex", gap: "10px", marginTop: "24px" }}>
              <button
                type="button"
                onClick={closeWithdraw}
                disabled={withdrawing}
                style={{
                  flex: 1,
                  border: "1px solid #E2E8F0",
                  borderRadius: "14px",
                  padding: "13px",
                  backgroundColor: "#FFFFFF",
                  color: "#475569",
                  fontWeight: 800,
                  cursor: withdrawing ? "not-allowed" : "pointer",
                }}
              >
                취소
              </button>
              <button
                type="submit"
                disabled={withdrawing || !canWithdraw}
                style={{
                  flex: 1.4,
                  border: "none",
                  borderRadius: "14px",
                  padding: "13px",
                  backgroundColor: withdrawing || !canWithdraw ? "#CBD5E1" : "#E11D48",
                  color: "#FFFFFF",
                  fontWeight: 800,
                  cursor: withdrawing ? "wait" : "pointer",
                }}
              >
                {withdrawing ? "처리 중..." : "회원 탈퇴"}
              </button>
            </div>
          </form>
        </div>
      )}

      {withdrawComplete && (
        <div
          role="presentation"
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 1100,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: "24px",
            backgroundColor: "rgba(15, 23, 42, 0.52)",
          }}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="withdraw-complete-title"
            style={{
              width: "100%",
              maxWidth: "430px",
              borderRadius: "28px",
              backgroundColor: "#FFFFFF",
              padding: "34px 30px 30px",
              textAlign: "center",
              boxShadow: "0 28px 80px rgba(15, 23, 42, 0.28)",
            }}
          >
            <div
              aria-hidden="true"
              style={{
                display: "flex",
                width: "68px",
                height: "68px",
                margin: "0 auto",
                alignItems: "center",
                justifyContent: "center",
                borderRadius: "22px",
                background: "linear-gradient(135deg, #EEF4FF 0%, #F3E8FF 100%)",
                color: "#4F7DF3",
                fontSize: "30px",
                fontWeight: 900,
              }}
            >
              M
            </div>
            <p style={{ margin: "20px 0 0", color: "#4F7DF3", fontSize: "12px", fontWeight: 900, letterSpacing: "0.08em" }}>
              GOODBYE
            </p>
            <h2 id="withdraw-complete-title" style={{ margin: "8px 0 0", color: "#172033", fontSize: "27px" }}>
              이용해 주셔서 감사합니다
            </h2>
            <p style={{ margin: "14px auto 0", maxWidth: "330px", color: "#64748B", fontSize: "14px", lineHeight: 1.75 }}>
              회원 탈퇴가 완료되었습니다.<br />모여ON과 함께한 시간이 좋은 기억으로 남기를 바랍니다.
            </p>
            <button
              type="button"
              autoFocus
              onClick={() => navigate("/", { replace: true })}
              style={{
                width: "100%",
                marginTop: "26px",
                border: "none",
                borderRadius: "15px",
                padding: "14px",
                backgroundColor: "#4F7DF3",
                color: "#FFFFFF",
                fontSize: "15px",
                fontWeight: 900,
                cursor: "pointer",
                boxShadow: "0 10px 24px rgba(79, 125, 243, 0.24)",
              }}
            >
              확인
            </button>
          </div>
        </div>
      )}


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


            {(meetingsLoading || meetingsError) && (
              <div
                role="status"
                style={{
                  marginTop: "18px",
                  padding: "11px 14px",
                  borderRadius: "12px",
                  backgroundColor: meetingsError ? "#FEF2F2" : "#F1F5F9",
                  color: meetingsError ? "#DC2626" : "#64748B",
                  fontSize: "13px",
                  fontWeight: "600",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  gap: "12px",
                }}
              >
                <span>{meetingsError || "모임 일정을 불러오는 중..."}</span>

                {meetingsError && (
                  <button
                    onClick={reloadMeetings}
                    style={{
                      border: "1px solid #FECACA",
                      backgroundColor: "#FFFFFF",
                      color: "#DC2626",
                      borderRadius: "8px",
                      padding: "6px 10px",
                      fontSize: "12px",
                      fontWeight: "700",
                      cursor: "pointer",
                    }}
                  >
                    다시 불러오기
                  </button>
                )}
              </div>
            )}


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

                const dayMeetings = item.date
                  ? getMeetingsByDate(item.date)
                  : [];

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
                      overflowY: expandedDate === item.date ? "auto" : "visible",
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

                    {(expandedDate === item.date ? dayMeetings : dayMeetings.slice(0, 2)).map((meeting, chipIndex) => (
                      <div
                        key={meeting.id}
                        onClick={(event) => {
                          event.stopPropagation();
                          openMeetingFromChip(meeting);
                        }}
                        style={{
                          marginTop: chipIndex === 0 ? "12px" : "5px",
                          padding: "5px 8px",
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
                          cursor: "pointer",
                        }}
                      >
                        ● {meeting.title}
                      </div>
                    ))}

                    {dayMeetings.length > 2 && (
                      <div
                        onClick={(event) => {
                          event.stopPropagation();
                          setExpandedDate(expandedDate === item.date ? null : item.date);
                        }}
                        style={{
                          marginTop: "4px",
                          fontSize: "11px",
                          fontWeight: "700",
                          color: "#94A3B8",
                          cursor: "pointer",
                        }}
                      >
                        {expandedDate === item.date ? "접기" : `+${dayMeetings.length - 2}개`}
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
                onClick={openProfileEdit}
                disabled={!user}
                style={{
                  width: "100%",
                  border: "none",
                  backgroundColor: "#F1F5F9",
                  borderRadius: "14px",
                  padding: "14px",
                  fontSize: "14px",
                  fontWeight: "700",
                  color: "#475569",
                  cursor: user ? "pointer" : "not-allowed",
                  opacity: user ? 1 : 0.6,
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

              <div
                style={{
                  marginTop: "14px",
                  paddingTop: "12px",
                  borderTop: "1px solid #F1F5F9",
                  textAlign: "center",
                }}
              >
                <button
                  type="button"
                  onClick={openWithdraw}
                  style={{
                    border: "none",
                    padding: "4px 8px",
                    backgroundColor: "transparent",
                    color: "#94A3B8",
                    fontSize: "12px",
                    textDecoration: "underline",
                    textUnderlineOffset: "3px",
                    cursor: "pointer",
                  }}
                >
                  회원 탈퇴
                </button>
              </div>

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
      {formatDate(nextMeeting.date)}
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

              {selectedMeeting.location && (
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
              )}

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
                👥 참여 인원 {selectedMeeting.memberCount}명
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
                {selectedMeeting.description || "작성된 설명이 없습니다."}
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

            {!votesView?.data ? (
              <div
                style={{
                  padding: "40px 0",
                  textAlign: "center",
                  fontSize: "13px",
                  color: "#64748B",
                }}
              >
                {votesView?.error ? (
                  <>
                    <p style={{ margin: "0 0 14px" }}>{votesView.error}</p>

                    <button
                      onClick={() => loadVotes(selectedMeeting.id)}
                      style={{
                        border: "1px solid #E2E8F0",
                        backgroundColor: "#FFFFFF",
                        color: "#475569",
                        borderRadius: "10px",
                        padding: "9px 14px",
                        fontSize: "12px",
                        fontWeight: "700",
                        cursor: "pointer",
                      }}
                    >
                      다시 불러오기
                    </button>
                  </>
                ) : (
                  "투표 현황을 불러오는 중..."
                )}
              </div>
            ) : (
              <>
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
                🏆 가장 많은 투표를 받은 날짜
              </p>

              <div
                style={{
                  fontSize: "18px",
                  fontWeight: "800",
                  color: "#172033",
                }}
              >
                {getMostVotedDates(votesView.data)
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
                총 {getMaxVoteCount(votesView.data)}명이 선택했습니다.
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


              {Object.entries(getVoteSummary(votesView.data)).map(
                ([date, count]) => {

                  const maxVote = getMaxVoteCount(votesView.data);

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

                {votesView.data.members.map((vote) => {

                  const hasVoted = vote.dates.length > 0;

                  return (
                    <div
                      key={vote.nickname}
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
                          {vote.nickname}
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


              </>
            )}


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
              📅 {formatDate(nextMeeting.date)}
            </div>

            <div
              style={{
                marginTop: "3px",
                fontSize: "12px",
                color: "#64748B",
              }}
            >
              📍 {nextMeeting.location ? `${nextMeeting.location} · ` : ""}{nextMeeting.place}
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
