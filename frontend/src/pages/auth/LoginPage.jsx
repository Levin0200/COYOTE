import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { login } from "../../services/authService";
import { getErrorMessage } from "../../services/api";

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleLogin = async (event) => {
    event?.preventDefault();
    if (!email.trim() || !password) {
      alert("이메일과 비밀번호를 입력해주세요.");
      return;
    }

    setSubmitting(true);

    try {
      await login(email.trim(), password);

      alert("로그인되었습니다.");
      const requestedPath = typeof location.state?.from === "string" && location.state.from.startsWith("/")
        ? location.state.from
        : "/mypage";
      navigate(requestedPath, { replace: true });
    } catch (error) {
      alert(getErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-6">
      <form
        onSubmit={handleLogin}
        className="
          w-full
          max-w-md
          bg-white
          border
          border-gray-100
          rounded-3xl
          p-8
          shadow-sm
        "
      >
        <div className="text-center mb-8">
          <div className="text-[#4F7DF3] font-bold text-xl mb-2">
            모여ON
          </div>

          <h2 className="text-3xl font-bold">
            안녕하세요 👋
          </h2>

          <p className="text-gray-500 mt-2">
            모여ON에 로그인하세요
          </p>
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            이메일
          </label>

          <input
            type="email"
            autoComplete="email"
            required
            placeholder="example@email.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="
              w-full
              border
              border-gray-200
              rounded-xl
              p-3
              focus:outline-none
              focus:ring-2
              focus:ring-[#4F7DF3]
            "
          />
        </div>

        <div className="mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            비밀번호
          </label>

          <input
            type="password"
            autoComplete="current-password"
            required
            placeholder="비밀번호 입력"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="
              w-full
              border
              border-gray-200
              rounded-xl
              p-3
              focus:outline-none
              focus:ring-2
              focus:ring-[#4F7DF3]
            "
          />
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="
            w-full
            bg-[#4F7DF3]
            text-white
            py-3
            rounded-xl
            font-medium
            hover:bg-[#3D6BE5]
            disabled:opacity-60
            transition
          "
        >
          로그인
        </button>

        <div className="mt-5 text-sm text-center">
          <button
            type="button"
            onClick={() => navigate("/signup")}
            className="text-[#4F7DF3] hover:underline"
          >
            회원가입
          </button>

        </div>

        <div className="mt-8 pt-6 border-t border-gray-100 text-center">
          <p className="text-xs text-gray-400">
            © 2026 모여ON. All rights reserved.
          </p>
        </div>
      </form>
    </div>
  );
}

export default LoginPage; 
