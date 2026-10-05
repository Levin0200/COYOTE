import React, { useState } from 'react';
import { useNavigate } from "react-router-dom";

function MainPage() {
  const [isDarkMode, setIsDarkMode] = useState(false); 
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('all');
  const [selectedMeeting, setSelectedMeeting] = useState(null);
  const [activeNotifyTab, setActiveNotifyTab] = useState('calendar');
  
  let navigate;
  try {
    navigate = useNavigate();
  } catch (e) {
    navigate = (path) => console.log("이동 경로:", path);
  }

  const theme = {
    bg: isDarkMode ? '#0B1020' : '#FFFFFF',
    cardBg: isDarkMode ? 'rgba(255,255,255,0.03)' : '#F8FAFC',
    cardBorder: isDarkMode ? 'rgba(255,255,255,0.08)' : '#E2E8F0',
    innerBoxBg: isDarkMode ? '#0a0d1a' : '#F1F5F9',
    popupBg: isDarkMode ? '#0f1425' : '#FFFFFF',
    textMain: isDarkMode ? '#ffffff' : '#334155',
    textSub: isDarkMode ? '#94a3b8' : '#64748b',
    textTitle: isDarkMode ? '#f8fafc' : '#0F172A',
    gradient: isDarkMode 
      ? 'radial-gradient(circle at top left, rgba(59,130,246,0.18), transparent 45%), radial-gradient(circle at bottom right, rgba(168,85,247,0.12), transparent 45%)'
      : 'radial-gradient(circle at top left, rgba(59,130,246,0.05), transparent 50%), radial-gradient(circle at bottom right, rgba(168,85,247,0.03), transparent 50%)',
    shadow: isDarkMode 
      ? '0 20px 45px rgba(0,0,0,0.25)' 
      : '0 12px 30px rgba(148,163,184,0.08)',
  };

  const notifyDetails = {
    calendar: [
      { id: 11, title: "📅 캘린더 연동 알림", text: "멤버 5명의 구글 캘린더 공백 시간대 분석이 완료되었습니다.", time: "방금 전" },
      { id: 12, title: "📅 일정 리마인더", text: "이번 주 토요일 '대학 동창 모임' 일정이 캘린더에 등록되었습니다.", time: "2시간 전" }
    ],
    vote: [
      { id: 21, title: "🔥 투표 진행 중", text: "'개발팀 주말 회식' 메뉴 선호도 투표 마감이 3시간 남았습니다.", time: "10분 전" },
      { id: 22, title: "🗳️ 새로운 투표", text: "성수동 번개 모임 장소 선정을 위한 2차 투표가 생성되었습니다.", time: "1일 전" }
    ],
    promise: [
      { id: 31, title: "✨ 약속 확정 알림", text: "'금요 러닝 크루 번개'의 시간과 최종 장소가 확정되었습니다.", time: "30분 전" },
      { id: 32, title: "⚠️ 보증금 미입금 안내", text: "신년회 모임 보증금 패널티 정산 기한이 오늘 자정까지입니다.", time: "4시간 전" }
    ]
  };

  const myMeetings = [
    { id: 1, title: "개발팀 주말 정기 회식", status: "진행 중", type: "AI 분석 중", date: "조율 중 (토요일 유력)", location: "강남역 인근", color: "#3b82f6", bg: "rgba(59,130,246,0.08)", img: "📊", members: 5, progress: 75, details: "현재 5명 중 4명 일정 연동 완료. 고기류/조용한 룸 선호도가 가장 높게 분석됨." },
    { id: 2, title: "대학 동창 신년회 모임", status: "확정", type: "최적화 완료", date: "2026년 06월 06일 (토) 18:00", location: "홍대 스페이스 룸", color: "#10b981", bg: "rgba(16,185,129,0.08)", img: "✨", members: 8, progress: 100, details: "출발지 분석 결과 합정/홍대 중간지점 매핑 성공. 노쇼 방지 보증금 인당 10,000원 전원 납부 완료." },
    { id: 3, title: "COYOTE 네트워킹 데이", status: "진행 중", type: "투표 수집 중", date: "조율 중 (평일 저녁)", location: "성수동 공유오피스", color: "#a855f7", bg: "rgba(168,85,247,0.08)", img: "🚀", members: 12, progress: 40, details: "메뉴 및 선호 플레이스 투표 참여율 40% 진행 중. 트렌디한 라운지 바 위주 추천 중." },
    { id: 4, title: "금요 러닝 크루 번개", status: "확정", type: "정산 완료", date: "2026년 06월 12일 (금) 20:00", location: "여의도 한강공원", color: "#f59e0b", bg: "rgba(245,158,11,0.08)", img: "🏃‍♂️", members: 6, progress: 100, details: "러닝 코스 및 러닝 직후 방문할 이자카야 예약 완료. 우천 시 실내 액티비티로 대체 플랜 B 작동." }
  ];

  const filteredMeetings = myMeetings.filter(m => {
    if (activeTab === 'all') return true;
    if (activeTab === 'progress') return m.status === '진행 중';
    if (activeTab === 'confirmed') return m.status === '확정';
    return true;
  });

  return (
    <div style={{ minHeight: '100vh', backgroundColor: theme.bg, color: theme.textMain, position: 'relative', overflow: 'hidden', fontFamily: 'sans-serif', transition: 'background-color 0.3s, color 0.3s' }}>
      
      <div style={{ position: 'absolute', inset: 0, backgroundImage: theme.gradient, pointerEvents: 'none' }} />

      <div style={{ position: 'relative', maxWidth: '1200px', margin: '0 auto', padding: '32px 24px' }}>
        
        <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '48px', position: 'relative', zIndex: 100 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer' }} onClick={() => navigate("/")}>
            <div className="flex h-[38px] w-[38px] shrink-0 items-center justify-center rounded-xl bg-blue-500 font-black text-white shadow-[0_4px_12px_rgba(59,130,246,0.2)]">
              C
            </div>

            <h1
              className={`whitespace-nowrap text-[22px] font-black tracking-[-0.05em] ${
                isDarkMode ? "text-white" : "text-slate-900"
              }`}
            >
              COYOTE
            </h1>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '14px', position: 'relative' }}>
            <button
              onClick={() => setIsDarkMode(!isDarkMode)}
              style={{
                backgroundColor: theme.cardBg,
                border: `1px solid ${theme.cardBorder}`,
                color: theme.textMain,
                padding: '10px 16px',
                borderRadius: '14px',
                fontWeight: '600',
                fontSize: '14px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                boxShadow: theme.shadow,
                backdropFilter: 'blur(10px)'
              }}
            >
              {isDarkMode ? '☀️ 라이트 모드' : '🌙 다크 모드'}
            </button>

            <button 
              onClick={() => setIsNotificationOpen(!isNotificationOpen)}
              style={{ position: 'relative', backgroundColor: theme.cardBg, border: `1px solid ${theme.cardBorder}`, color: theme.textMain, width: '44px', height: '44px', borderRadius: '14px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '18px', cursor: 'pointer' }}
            >
              🔔
              <span style={{ position: 'absolute', top: '12px', right: '12px', width: '7px', height: '7px', backgroundColor: '#ef4444', borderRadius: '50%', boxShadow: '0 0 8px #ef4444' }} />
            </button>

            {isNotificationOpen && (
              <div style={{ position: 'absolute', top: '56px', right: 0, width: '360px', backgroundColor: theme.popupBg, border: `1px solid ${theme.cardBorder}`, borderRadius: '24px', padding: '20px', boxShadow: '0 20px 40px rgba(15,23,42,0.08)', zIndex: 110, backdropFilter: 'blur(20px)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                  <h4 style={{ margin: 0, fontSize: '16px', fontWeight: 'bold', color: theme.textTitle }}>🔔 알림 인텔리전스</h4>
                  <span style={{ fontSize: '12px', color: theme.textSub, cursor: 'pointer' }} onClick={() => setIsNotificationOpen(false)}>✕ 닫기</span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '6px', backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : '#E2E8F0', padding: '4px', borderRadius: '12px', marginBottom: '16px' }}>
                  {['calendar', 'vote', 'promise'].map((tab) => (
                    <button
                      key={tab}
                      onClick={() => setActiveNotifyTab(tab)}
                      style={{
                        backgroundColor: activeNotifyTab === tab ? '#3b82f6' : 'transparent',
                        border: 'none',
                        color: activeNotifyTab === tab ? '#ffffff' : theme.textSub,
                        padding: '6px 0',
                        borderRadius: '8px',
                        fontSize: '12px',
                        fontWeight: '600',
                        cursor: 'pointer'
                      }}
                    >
                      {tab === 'calendar' ? '캘린더' : tab === 'vote' ? '투표' : '약속'}
                    </button>
                  ))}
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', maxHeight: '240px', overflowY: 'auto' }}>
                  {notifyDetails[activeNotifyTab].map(item => (
                    <div key={item.id} style={{ backgroundColor: isDarkMode ? 'rgba(255,255,255,0.03)' : '#FFFFFF', padding: '12px', borderRadius: '14px', border: `1px solid ${theme.cardBorder}` }}>
                      <p style={{ margin: '0 0 4px 0', fontSize: '13px', fontWeight: '700', color: '#3b82f6' }}>{item.title}</p>
                      <p style={{ margin: '0 0 6px 0', fontSize: '12px', color: theme.textMain, lineHeight: '1.4' }}>{item.text}</p>
                      <span style={{ fontSize: '10px', color: theme.textSub }}>{item.time}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <button 
              onClick={() => navigate("/mypage")}
              style={{ backgroundColor: theme.cardBg, border: `1px solid ${theme.cardBorder}`, color: theme.textMain, padding: '10px 18px', borderRadius: '14px', fontWeight: '600', fontSize: '13px', cursor: 'pointer' }}
            >
              👤 마이페이지
            </button>
          </div>
        </header>

        <section style={{ marginBottom: '48px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: '20px' }}>
            <div>
              <h2 style={{ fontSize: '32px', fontWeight: '900', letterSpacing: '-0.02em', margin: '0 0 8px 0', color: theme.textTitle }}>모임 컨트롤 타워</h2>
              <p style={{ color: theme.textSub, margin: 0, fontSize: '15px' }}>초대 기반 멤버들의 스케줄 동기화 및 위치 최적화 보드입니다.</p>
            </div>
            <button
              onClick={() => navigate("/create-meeting")}
              style={{ backgroundColor: '#3b82f6', border: 'none', color: '#ffffff', padding: '14px 28px', borderRadius: '14px', fontWeight: 'bold', fontSize: '15px', cursor: 'pointer', boxShadow: '0 6px 16px rgba(59,130,246,0.25)' }}
            >
              ➕ 새로운 모임 예약하기
            </button>
          </div>
        </section>

        <section style={{ marginBottom: '28px' }}>
          <div style={{ display: 'flex', gap: '12px', borderBottom: `1px solid ${theme.cardBorder}`, paddingBottom: '12px' }}>
            {[
              { id: 'all', label: '전체 보기' },
              { id: 'progress', label: '🔄 진행 중' },
              { id: 'confirmed', label: '✅ 확정' }
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                style={{
                  backgroundColor: activeTab === tab.id ? 'rgba(59,130,246,0.06)' : 'transparent',
                  border: 'none',
                  borderBottom: activeTab === tab.id ? '2px solid #3b82f6' : '2px solid transparent',
                  color: activeTab === tab.id ? '#3b82f6' : theme.textSub,
                  padding: '10px 20px',
                  fontSize: '14px',
                  fontWeight: '700',
                  cursor: 'pointer',
                  borderRadius: '8px 8px 0 0'
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </section>

        <section style={{ marginBottom: '64px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '28px' }}>
            {filteredMeetings.map((meeting) => (
              <div 
                key={meeting.id}
                onClick={() => setSelectedMeeting(meeting)}
                style={{
                  backgroundColor: theme.cardBg,
                  backdropFilter: 'blur(20px)',
                  border: `1px solid ${theme.cardBorder}`,
                  borderRadius: '28px',
                  padding: '24px',
                  cursor: 'pointer',
                  boxShadow: theme.shadow,
                  transition: 'transform 0.2s, border-color 0.2s, background-color 0.3s',
                  position: 'relative'
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                  <span style={{ fontSize: '11px', fontWeight: 'bold', color: theme.textSub, letterSpacing: '0.05em' }}>{meeting.type}</span>
                  <span style={{ fontSize: '12px', fontWeight: 'bold', color: meeting.color, backgroundColor: meeting.bg, padding: '4px 12px', borderRadius: '9999px' }}>
                    {meeting.status}
                  </span>
                </div>

                <h3 style={{ fontSize: '20px', fontWeight: '800', margin: '0 0 16px 0', color: theme.textTitle }}>{meeting.title}</h3>

                <div style={{ 
                  height: '140px', 
                  backgroundColor: theme.innerBoxBg, 
                  borderRadius: '20px', 
                  marginBottom: '20px',
                  position: 'relative',
                  overflow: 'hidden',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: `1px solid ${theme.cardBorder}`,
                  transition: 'background-color 0.3s'
                }}>
                  <div style={{ position: 'absolute', width: '80px', height: '80px', backgroundColor: meeting.color, opacity: 0.12, filter: 'blur(25px)', borderRadius: '50%' }} />
                  <span style={{ fontSize: '56px', zIndex: 10 }}>{meeting.img}</span>
                  
                  <div style={{ position: 'absolute', bottom: '16px', left: '16px', right: '16px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '10px', color: theme.textSub, marginBottom: '4px' }}>
                      <span>AI 데이터 분석 정밀도</span>
                      <span style={{ color: meeting.color, fontWeight: 'bold' }}>{meeting.progress}%</span>
                    </div>
                    <div style={{ width: '100%', height: '6px', backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.06)', borderRadius: '9999px', overflow: 'hidden' }}>
                      <div style={{ width: `${meeting.progress}%`, height: '100%', backgroundColor: meeting.color, borderRadius: '9999px' }} />
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', backgroundColor: isDarkMode ? 'rgba(0,0,0,0.15)' : '#FFFFFF', padding: '14px', borderRadius: '16px', border: `1px solid ${theme.cardBorder}`, transition: 'background-color 0.3s' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                    <span style={{ color: theme.textSub }}>🗓️ 만날 날짜</span>
                    <span style={{ color: theme.textMain, fontWeight: '600' }}>{meeting.date}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                    <span style={{ color: theme.textSub }}>📍 모임 장소</span>
                    <span style={{ color: theme.textMain, fontWeight: '600' }}>{meeting.location}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                    <span style={{ color: theme.textSub }}>👥 참여 인원</span>
                    <span style={{ color: theme.textMain, fontWeight: '600' }}>{meeting.members}명 동기화 중</span>
                  </div>
                </div>

                <div style={{ textAlign: 'center', marginTop: '14px', fontSize: '12px', color: theme.textSub }}>
                  상세 가이드 및 분석 로그 보기 →
                </div>
              </div>
            ))}
          </div>
        </section>

        {selectedMeeting && (
          <div style={{ position: 'fixed', bottom: '24px', right: '24px', maxWidth: 'calc(100vw - 48px)', width: '380px', backgroundColor: theme.popupBg, border: '2px solid #3b82f6', borderRadius: '24px', padding: '24px', boxShadow: '0 20px 40px rgba(15,23,42,0.15)', zIndex: 200 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <span style={{ fontSize: '11px', fontWeight: 'bold', color: '#3b82f6', backgroundColor: 'rgba(59,130,246,0.08)', padding: '4px 10px', borderRadius: '8px' }}>DETAILED SYNC</span>
              <button style={{ backgroundColor: 'transparent', border: 'none', color: theme.textSub, cursor: 'pointer', fontSize: '16px' }} onClick={() => setSelectedMeeting(null)}>✕</button>
            </div>
            <h4 style={{ margin: '0 0 8px 0', fontSize: '18px', fontWeight: '800', color: theme.textTitle }}>{selectedMeeting.title}</h4>
            <p style={{ fontSize: '13px', color: theme.textMain, lineHeight: '1.5', margin: '0 0 16px 0', backgroundColor: theme.innerBoxBg, padding: '12px', borderRadius: '12px', border: `1px solid ${theme.cardBorder}` }}>
              {selectedMeeting.details}
            </p>
            <div style={{ display: 'flex', gap: '10px' }}>
              <button style={{ flex: 1, backgroundColor: '#3b82f6', border: 'none', color: '#fff', padding: '12px', borderRadius: '12px', fontWeight: 'bold', fontSize: '13px', cursor: 'pointer' }} onClick={() => alert('Map API 데이터 스캔 가동')}>
                🗺️ 추천 장소 맵 연동
              </button>
              <button style={{ backgroundColor: theme.innerBoxBg, border: `1px solid ${theme.cardBorder}`, color: theme.textMain, padding: '12px', borderRadius: '12px', fontSize: '13px', cursor: 'pointer' }} onClick={() => setSelectedMeeting(null)}>
                닫기
              </button>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}

export default MainPage;