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
  try {
    localStorage.setItem(TOKEN_KEY, token);
  } catch {
    // 저장소를 사용할 수 없는 환경에서는 로그인 상태를 유지하지 않는다.
  }
};

export const clearToken = () => {
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch {
    // 저장소를 사용할 수 없는 환경에서는 별도 처리가 필요하지 않다.
  }
};

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
