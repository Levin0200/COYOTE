import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { FaGoogle, FaApple } from "react-icons/fa";
import { SiNaver, SiKakaotalk } from "react-icons/si";

function LoginPage() {
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = () => {
    const savedUser = localStorage.getItem("user");

    if (!savedUser) {
      alert("가입된 회원정보가 없습니다. 먼저 회원가입을 해주세요.");
      return;
    }

    const user = JSON.parse(savedUser);

    if (email === user.email && password === user.password) {
      localStorage.setItem("isLoggedIn", "true");

      alert("로그인되었습니다.");

      navigate("/main");
    } else {
      alert("이메일 또는 비밀번호가 일치하지 않습니다.");
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-6">
      <div
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
          onClick={handleLogin}
          className="
            w-full
            bg-[#4F7DF3]
            text-white
            py-3
            rounded-xl
            font-medium
            hover:bg-[#3D6BE5]
            transition
          "
        >
          로그인
        </button>

        <div className="mt-5 text-sm text-center">
          <button
            onClick={() => navigate("/signup")}
            className="text-[#4F7DF3] hover:underline"
          >
            회원가입
          </button>

          <span className="mx-2 text-gray-300">|</span>

          <button
            onClick={() =>
              alert("비밀번호 재설정 기능은 준비 중입니다.")
            }
            className="text-gray-500 hover:text-gray-700"
          >
            비밀번호 재설정
          </button>

          <span className="mx-2 text-gray-300">|</span>

          <button
            onClick={() =>
              alert("문의하기 기능은 준비 중입니다.")
            }
            className="text-gray-500 hover:text-gray-700"
          >
            문의하기
          </button>
        </div>

        <div className="flex items-center my-8">
          <div className="flex-1 h-px bg-gray-200" />

          <span className="px-4 text-sm text-gray-400">
            SNS로 로그인하기
          </span>

          <div className="flex-1 h-px bg-gray-200" />
        </div>

        <div className="flex justify-center gap-4">
          <button
            onClick={() =>
              alert("네이버 로그인은 준비 중입니다.")
            }
            className="
              w-12
              h-12
              rounded-full
              border
              border-gray-200
              flex
              items-center
              justify-center
              hover:bg-gray-50
              transition
            "
          >
            <SiNaver size={20} />
          </button>

          <button
            onClick={() =>
              alert("카카오 로그인은 준비 중입니다.")
            }
            className="
              w-12
              h-12
              rounded-full
              border
              border-gray-200
              flex
              items-center
              justify-center
              hover:bg-gray-50
              transition
            "
          >
            <SiKakaotalk size={20} />
          </button>

          <button
            onClick={() =>
              alert("Google 로그인은 준비 중입니다.")
            }
            className="
              w-12
              h-12
              rounded-full
              border
              border-gray-200
              flex
              items-center
              justify-center
              hover:bg-gray-50
              transition
            "
          >
            <FaGoogle size={20} />
          </button>

          <button
            onClick={() =>
              alert("Apple 로그인은 준비 중입니다.")
            }
            className="
              w-12
              h-12
              rounded-full
              border
              border-gray-200
              flex
              items-center
              justify-center
              hover:bg-gray-50
              transition
            "
          >
            <FaApple size={20} />
          </button>
        </div>

        <div className="mt-8 pt-6 border-t border-gray-100 text-center">
          <p className="text-xs text-gray-400">
            © 2026 모여ON. All rights reserved.
          </p>
        </div>
      </div>
    </div>
  );
}

export default LoginPage; 
