import axios from "axios";
import { clearAuthenticated } from "../utils/authSession";

const api = axios.create({
  // 배포 환경은 같은 Worker의 /api 프록시를 통해 백엔드에 연결한다.
  baseURL: import.meta.env.PROD ? "" : import.meta.env.VITE_API_BASE_URL || "",
  // 백엔드의 외부 장소/AI 호출 제한(최대 25초)보다 여유 있게 둔다.
  timeout: 35000,
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && !window.location.pathname.startsWith("/invite/")) {
      clearAuthenticated();
      if (window.location.pathname !== "/login") {
        window.location.assign("/login");
      }
    }
    return Promise.reject(error);
  }
);

export const getErrorCode = (error) => error.response?.data?.code;

export const getErrorMessage = (
  error,
  fallback = "요청을 처리하지 못했습니다. 잠시 후 다시 시도해주세요."
) => {
  if (error.code === "ECONNABORTED") {
    return "서버 응답이 오래 걸려 요청을 중단했습니다. 잠시 후 다시 시도해주세요.";
  }
  if (!error.response) {
    return "서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.";
  }
  return error.response.data?.message || fallback;
};

export default api;
