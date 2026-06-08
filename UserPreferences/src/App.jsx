import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css'; // 스타일에 맞춘 CSS 연동

function App() {
  const [isLogin, setIsLogin] = useState(true);
  const userId = 1; 

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const mode = params.get('mode');
    if (mode === 'guest') {
      setIsLogin(false);
    } else {
      setIsLogin(true);
    }
  }, []);

  const [preferences, setPreferences] = useState({
    TIME: '',
    PLACE: '',
    DISTANCE: '',
    CATEGORY: '',
    BUDGET: '',
    TRANSPORT: '',
  });

  const handleSelect = (type, value) => {
    setPreferences({
      ...preferences,
      [type]: value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (isLogin) {
      if (!preferences.TIME || !preferences.PLACE || !preferences.DISTANCE || 
          !preferences.CATEGORY || !preferences.BUDGET || !preferences.TRANSPORT) {
        alert('회원 맞춤 추천을 위해 6가지 항목을 모두 선택해주세요! 📋');
        return;
      }

      const requestData = [
        { preferenceType: 'TIME', preferenceValue: preferences.TIME },
        { preferenceType: 'PLACE', preferenceValue: preferences.PLACE },
        { preferenceType: 'DISTANCE', preferenceValue: preferences.DISTANCE },
        { preferenceType: 'CATEGORY', preferenceValue: preferences.CATEGORY },
        { preferenceType: 'BUDGET', preferenceValue: preferences.BUDGET },
        { preferenceType: 'TRANSPORT', preferenceValue: preferences.TRANSPORT }
      ];

      try {
        const response = await axios.post(
          `http://localhost:8080/api/preferences/register?userId=${userId}`, 
          requestData
        );
        alert('🎉 회원 맞춤 데이터가 안전하게 DB에 저장되었습니다!'); 
      } catch (error) {
        console.error('서버 저장 에러:', error);
        alert('서버 에러가 발생했습니다. 백엔드가 켜져있는지 확인해주세요!');
      }

    } else {
      if (!preferences.TIME || !preferences.PLACE || !preferences.DISTANCE) {
        alert('기본 피드 추천을 위해 3가지 항목을 모두 선택해주세요! 💡');
        return;
      }

      const guestData = {
        TIME: preferences.TIME,
        PLACE: preferences.PLACE,
        DISTANCE: preferences.DISTANCE
      };

      sessionStorage.setItem('guestPreferences', JSON.stringify(guestData));
      alert('🎈 비회원 임시 선호도가 브라우저 세션에 성공적으로 적용되었습니다!');
    }
  };

  const renderOptionButtons = (type, options) => {
    return (
      <div className="custom-btn-group">
        {options.map((opt) => {
          const isActive = preferences[type] === opt.value;
          return (
            <button
              key={opt.value}
              type="button"
              onClick={() => handleSelect(type, opt.value)}
              className={`option-btn ${isActive ? 'active' : ''}`}
            >
              {opt.label}
            </button>
          );
        })}
      </div>
    );
  };

  return (
    <div className="login-page-bg">
      <div className="login-card">
        
        {/* 상단 배지 영역 */}
        <div className="text-center">
          <div className="badge-wrapper">
            {isLogin ? (
              <span className="badge-member">🔑 회원 맞춤 사전조사</span>
            ) : (
              <span className="badge-guest">✉️ 비회원 초대 링크 접속</span>
            )}
          </div>

          <div className="brand-logo">모여ON</div>
          <h2 className="main-title">취향 분석 사전 조사 🎯</h2>
          <p className="description-text">
            {isLogin 
              ? "회원가입을 환영합니다! 정밀한 피드 분석을 위해 6가지 항목을 선택해 주세요." 
              : "친구에게 초대받으셨군요! 3가지 기본 정보로 임시 취향을 매칭합니다."}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="survey-form">
          
          {/* 1. 선호 시간대 */}
          <div className="form-group">
            <label className="group-label">⏰ 선호 시간대</label>
            {renderOptionButtons('TIME', [
              { value: '오전', label: '🌅 오전' },
              { value: '오후', label: '☀️ 오후' },
              { value: '저녁', label: '🌙 저녁' }
            ])}
          </div>

          {/* 2. 활동 공간 */}
          <div className="form-group">
            <label className="group-label">📍 활동 공간</label>
            {renderOptionButtons('PLACE', [
              { value: '실내', label: '🏠 실내형' },
              { value: '실외', label: '🌳 실외형' },
              { value: '상관없음', label: '🔄 올라운더' }
            ])}
          </div>

          {/* 3. 활동 반경 */}
          <div className="form-group">
            <label className="group-label">🚗 활동 반경</label>
            {renderOptionButtons('DISTANCE', [
              { value: '5km 이내', label: '🏃 가깝게' },
              { value: '10km 이내', label: '🚌 적당히' },
              { value: '30km 이내', label: '✈️ 멀리멀리' }
            ])}
          </div>

          {/* ✨ 회원 전용 추가 항목 (구분선 없이 자연스럽게 배치) */}
          {isLogin && (
            <div className="member-only-section">
              {/* 4. 관심사 카테고리 */}
              <div className="form-group">
                <label className="group-label">🎭 관심사 카테고리</label>
                {renderOptionButtons('CATEGORY', [
                  { value: '문화', label: '🎬 문화·예술' },
                  { value: '맛집', label: '🍕 맛집 탐방' },
                  { value: '힐링', label: '🌿 자연 힐링' }
                ])}
              </div>

              {/* 5. 소비 성향 */}
              <div className="form-group">
                <label className="group-label">💰 소비 성향</label>
                {renderOptionButtons('BUDGET', [
                  { value: '가성비', label: '🪙 알뜰 지향' },
                  { value: '평범', label: '💵 일반 기본' },
                  { value: '럭셔리', label: '💎 플렉스형' }
                ])}
              </div>

              {/* 6. 주요 교통수단 */}
              <div className="form-group">
                <label className="group-label">🚲 주요 교통수단</label>
                {renderOptionButtons('TRANSPORT', [
                  { value: '도보', label: '🚶 도보/자전거' },
                  { value: '대중교통', label: '🚌 대중교통' },
                  { value: '자차', label: '🚗 자가용' }
                ])}
              </div>
            </div>
          )}

          {/* 메인 제출 버튼 */}
          <button type="submit" className="main-submit-btn">
            {isLogin ? "취향 정보 등록 및 회원가입 완료" : "비회원 임시 취향 적용하기"}
          </button>
        </form>

        {/* 푸터 영역 */}
        <div className="card-footer">
          <p className="copyright">© 2026 모여ON. All rights reserved.</p>
        </div>

      </div>
    </div>
  );
}

export default App;