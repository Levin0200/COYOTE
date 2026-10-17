import api from "./api";
import { clearAuthenticated, markAuthenticated } from "../utils/authSession";

export const sendVerificationCode = (email) =>
  api.post("/api/auth/email-verification", { email });

export const confirmVerificationCode = (email, code) =>
  api.post("/api/auth/email-verification/confirm", { email, code });

export const signup = ({
  email,
  name,
  password,
  address,
  serviceAgree,
  privacyAgree,
}) =>
  api
    .post("/api/auth/signup", {
      email,
      name,
      password,
      address,
      serviceAgree,
      privacyAgree,
    })
    .then((response) => response.data);

export const login = async (email, password) => {
  const { data } = await api.post("/api/auth/login", { email, password });
  markAuthenticated();
  return data;
};

export const logout = async () => {
  try {
    await api.post("/api/auth/logout");
  } finally {
    clearAuthenticated();
  }
};

export const checkSession = () =>
  api.get("/api/auth/session").then((response) => Boolean(response.data?.authenticated));

export const fetchMe = () =>
  api.get("/api/users/me").then((response) => response.data);

export const updateMe = ({ name, address }) =>
  api
    .patch("/api/users/me", { name, address })
    .then((response) => response.data);

// 탈퇴 시 현재 비밀번호를 한 번 더 확인한다(틀리면 403 INVALID_PASSWORD).
export const deleteMe = (password) =>
  api.delete("/api/users/me", { data: { password } });
