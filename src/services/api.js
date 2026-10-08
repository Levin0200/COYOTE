import axios from "axios";
import { clearToken, getToken } from "../utils/authToken";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 토큰을 들고 있는데 401이 오면 만료/무효 토큰이므로 로그인 화면으로 보낸다.
// (로그인 실패 401은 토큰이 없는 상태라 여기에 걸리지 않는다.)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && getToken()) {
      clearToken();
      window.location.assign("/login");
    }
    return Promise.reject(error);
  }
);

// 서버 에러 응답은 { code, message } 형태다.
export const getErrorCode = (error) => error.response?.data?.code;

export const getErrorMessage = (error, fallback = "요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.") => {
  if (!error.response) return "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.";
  return error.response.data?.message || fallback;
};

export default api;
