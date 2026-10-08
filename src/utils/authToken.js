const TOKEN_KEY = "accessToken";

const isExpired = (token) => {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const { exp } = JSON.parse(atob(payload));
    return typeof exp === "number" && exp * 1000 <= Date.now();
  } catch {
    return true;
  }
};

export const setToken = (token) => {
  try { localStorage.setItem(TOKEN_KEY, token); } catch { /* 저장소를 쓸 수 없는 환경 */ }
};

export const clearToken = () => {
  try { localStorage.removeItem(TOKEN_KEY); } catch { /* 저장소를 쓸 수 없는 환경 */ }
};

// 만료된 토큰은 지우고 null을 돌려준다.
export const getToken = () => {
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return null;
    if (isExpired(token)) {
      clearToken();
      return null;
    }
    return token;
  } catch {
    return null;
  }
};

export const isLoggedIn = () => Boolean(getToken());
