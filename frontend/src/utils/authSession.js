const SESSION_HINT_KEY = "moyeonAuthenticated";

// 인증 수단은 HttpOnly 쿠키이므로 JavaScript에서 토큰을 읽거나 저장하지 않는다.
// 이 값은 화면 전환을 부드럽게 하기 위한 비민감 힌트일 뿐, 권한 판단은 항상 서버가 한다.
export const markAuthenticated = () => {
  try {
    localStorage.removeItem("accessToken");
    localStorage.setItem(SESSION_HINT_KEY, "1");
  } catch { /* 서버의 HttpOnly 쿠키가 실제 인증 상태를 결정한다. */ }
};

export const clearAuthenticated = () => {
  try {
    localStorage.removeItem("accessToken");
    localStorage.removeItem(SESSION_HINT_KEY);
  } catch { /* 저장소를 사용할 수 없어도 서버 쿠키 삭제를 계속한다. */ }
};
