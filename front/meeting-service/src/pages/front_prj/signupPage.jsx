import { useState } from "react";
import { useNavigate } from "react-router-dom";

function SignupPage() {
  const navigate = useNavigate();

  const [step, setStep] = useState(1);
  const [openTerm, setOpenTerm] = useState(null);

  const [email, setEmail] = useState("");
  const [verificationCode, setVerificationCode] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [address, setAddress] = useState("");

  const [serviceAgree, setServiceAgree] = useState(false);
  const [privacyAgree, setPrivacyAgree] = useState(false);

  const handleAllAgree = (checked) => {
    setServiceAgree(checked);
    setPrivacyAgree(checked);
  };

  const handleSendVerificationCode = () => {
    if (!email.trim()) {
      alert("이메일을 입력해주세요.");
      return;
    }

    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailPattern.test(email)) {
      alert("올바른 이메일 형식을 입력해주세요.");
      return;
    }

    const code = Math.floor(
      100000 + Math.random() * 900000
    ).toString();

    localStorage.setItem("verificationCode", code);

    alert(`프로토타입 인증번호: ${code}`);

    setStep(3);
  };

  const handleVerifyCode = () => {
    const savedCode = localStorage.getItem("verificationCode");

    if (!verificationCode.trim()) {
      alert("인증번호를 입력해주세요.");
      return;
    }

    if (verificationCode === savedCode) {
      alert("이메일 인증이 완료되었습니다.");

      localStorage.removeItem("verificationCode");

      setStep(4);
    } else {
      alert("인증번호가 일치하지 않습니다.");
    }
  };

  const handleSignup = () => {
    if (!name.trim()) {
      alert("이름을 입력해주세요.");
      return;
    }

    if (!password) {
      alert("비밀번호를 입력해주세요.");
      return;
    }

    if (!passwordConfirm) {
      alert("비밀번호 확인을 입력해주세요.");
      return;
    }

    if (password !== passwordConfirm) {
      alert("비밀번호가 일치하지 않습니다.");
      return;
    }

    const savedUser = localStorage.getItem("user");

    if (savedUser) {
      const user = JSON.parse(savedUser);

      if (user.email === email) {
        alert("이미 가입된 이메일입니다.");
        return;
      }
    }

    const user = {
      email,
      name,
      password,
      address,
    };

    localStorage.setItem("user", JSON.stringify(user));

    alert("회원가입이 완료되었습니다.");

    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-6 py-10">
      <div
        className="
          w-full
          max-w-md
          bg-white
          rounded-3xl
          border
          border-gray-100
          p-8
          shadow-sm
        "
      >
        <div className="text-center mb-8">
          <div className="text-[#4F7DF3] font-bold text-xl mb-2">
            모여ON
          </div>

          <h2 className="text-3xl font-bold mb-2">
            회원가입
          </h2>

          <p className="text-gray-500 text-sm">
            몇 단계만 거치면 바로 시작할 수 있어요.
          </p>
        </div>

        <div className="mb-8 flex justify-center">
          <div className="w-40 bg-slate-100 h-1.5 rounded-full overflow-hidden">
            <div
              className="
                bg-[#4F7DF3]
                h-1.5
                rounded-full
                transition-all
                duration-500
              "
              style={{
                width: `${(step / 4) * 100}%`,
              }}
            />
          </div>
        </div>

        {step === 1 && (
          <>
            <div className="mb-6">
              <h3 className="font-semibold text-lg">
                약관 동의
              </h3>

              <p className="text-sm text-gray-500 mt-1">
                모여ON 서비스 이용을 위해 약관에 동의해주세요.
              </p>
            </div>

            <div className="border border-gray-200 rounded-2xl overflow-hidden">
              <label className="flex items-center gap-3 p-4 bg-slate-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={serviceAgree && privacyAgree}
                  onChange={(e) =>
                    handleAllAgree(e.target.checked)
                  }
                  className="w-5 h-5 accent-[#4F7DF3]"
                />

                <div className="flex-1">
                  <p className="font-semibold text-gray-800">
                    전체 동의하기
                  </p>

                  <p className="text-xs text-gray-400 mt-1">
                    필수 약관에 모두 동의합니다.
                  </p>
                </div>
              </label>

              <div className="border-t border-gray-200">
                <div className="flex items-center gap-3 px-4 py-4">
                  <input
                    type="checkbox"
                    checked={serviceAgree}
                    onChange={(e) =>
                      setServiceAgree(e.target.checked)
                    }
                    className="w-5 h-5 accent-[#4F7DF3]"
                  />

                  <div className="flex-1">
                    <span className="text-sm text-gray-800">
                      서비스 이용약관 동의
                    </span>

                    <span className="ml-1 text-xs text-[#4F7DF3]">
                      (필수)
                    </span>
                  </div>

                  <button
                    type="button"
                    onClick={() =>
                      setOpenTerm(
                        openTerm === "service"
                          ? null
                          : "service"
                      )
                    }
                    className="text-sm text-gray-400 hover:text-[#4F7DF3]"
                  >
                    {openTerm === "service"
                      ? "닫기"
                      : "보기"}
                  </button>
                </div>

                {openTerm === "service" && (
                  <div className="mx-4 mb-4 p-4 bg-slate-50 border border-gray-100 rounded-xl">
                    <h4 className="font-semibold text-sm mb-3">
                      서비스 이용약관
                    </h4>

                    <div className="h-40 overflow-y-auto text-xs text-gray-500 leading-6 pr-2">
                      <p className="font-medium text-gray-700 mb-1">
                        제1조 (목적)
                      </p>

                      <p className="mb-3">
                        본 약관은 모여ON이 제공하는 서비스의
                        이용과 관련하여 서비스 이용 조건 및
                        절차, 이용자와 서비스 제공자의
                        권리와 의무를 규정하는 것을 목적으로
                        합니다.
                      </p>

                      <p className="font-medium text-gray-700 mb-1">
                        제2조 (서비스의 내용)
                      </p>

                      <p className="mb-3">
                        모여ON은 여러 사용자의 일정과 장소를
                        효율적으로 조율할 수 있도록 일정
                        조정, 장소 추천 및 관련 편의 기능을
                        제공합니다.
                      </p>

                      <p className="font-medium text-gray-700 mb-1">
                        제3조 (이용자의 의무)
                      </p>

                      <p className="mb-3">
                        이용자는 서비스를 이용하면서 타인의
                        권리를 침해하거나 서비스 운영을
                        방해하는 행위를 해서는 안 됩니다.
                      </p>

                      <p className="font-medium text-gray-700 mb-1">
                        제4조 (서비스 이용)
                      </p>

                      <p>
                        서비스의 일부 기능은 서비스 운영
                        상황에 따라 변경되거나 추가될 수
                        있습니다.
                      </p>
                    </div>
                  </div>
                )}

                <div className="flex items-center gap-3 px-4 py-4 border-t border-gray-100">
                  <input
                    type="checkbox"
                    checked={privacyAgree}
                    onChange={(e) =>
                      setPrivacyAgree(e.target.checked)
                    }
                    className="w-5 h-5 accent-[#4F7DF3]"
                  />

                  <div className="flex-1">
                    <span className="text-sm text-gray-800">
                      개인정보 수집 및 이용 동의
                    </span>

                    <span className="ml-1 text-xs text-[#4F7DF3]">
                      (필수)
                    </span>
                  </div>

                  <button
                    type="button"
                    onClick={() =>
                      setOpenTerm(
                        openTerm === "privacy"
                          ? null
                          : "privacy"
                      )
                    }
                    className="text-sm text-gray-400 hover:text-[#4F7DF3]"
                  >
                    {openTerm === "privacy"
                      ? "닫기"
                      : "보기"}
                  </button>
                </div>

                {openTerm === "privacy" && (
                  <div className="mx-4 mb-4 p-4 bg-slate-50 border border-gray-100 rounded-xl">
                    <h4 className="font-semibold text-sm mb-3">
                      개인정보 수집 및 이용
                    </h4>

                    <div className="h-40 overflow-y-auto text-xs text-gray-500 leading-6 pr-2">
                      <p className="font-medium text-gray-700 mb-1">
                        1. 수집하는 개인정보
                      </p>

                      <p className="mb-3">
                        회원가입 및 서비스 이용을 위해 이메일,
                        이름, 비밀번호 및 선택적으로 주소
                        정보를 입력받을 수 있습니다.
                      </p>

                      <p className="font-medium text-gray-700 mb-1">
                        2. 개인정보 이용 목적
                      </p>

                      <p className="mb-3">
                        수집된 정보는 회원 식별, 서비스 제공,
                        일정 및 장소 추천 등 서비스 운영을
                        위해 이용됩니다.
                      </p>

                      <p className="font-medium text-gray-700 mb-1">
                        3. 개인정보 보관
                      </p>

                      <p className="mb-3">
                        개인정보는 서비스 이용 목적을 달성하기
                        위해 필요한 기간 동안 보관하며,
                        관련 법령에 따라 필요한 경우 해당
                        기간 동안 보관할 수 있습니다.
                      </p>

                      <p className="font-medium text-gray-700 mb-1">
                        4. 이용자의 권리
                      </p>

                      <p>
                        이용자는 자신의 개인정보에 대한
                        열람 및 수정 등을 요청할 수 있습니다.
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>

            <button
              onClick={() => {
                if (!serviceAgree || !privacyAgree) {
                  alert("필수 약관에 모두 동의해주세요.");
                  return;
                }

                setStep(2);
              }}
              className="
                w-full
                bg-[#4F7DF3]
                text-white
                py-3
                rounded-xl
                mt-6
                hover:bg-[#3D6BE5]
                transition
                font-medium
              "
            >
              다음 단계
            </button>
          </>
        )}

        {step === 2 && (
          <>
            <div className="mb-6">
              <h3 className="font-semibold text-lg">
                이메일 입력
              </h3>

              <p className="text-sm text-gray-500 mt-1">
                인증번호를 받을 이메일 주소를 입력해주세요.
              </p>
            </div>

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
                mb-4
                focus:outline-none
                focus:ring-2
                focus:ring-[#4F7DF3]
              "
            />

            <button
              onClick={handleSendVerificationCode}
              className="
                w-full
                bg-[#4F7DF3]
                text-white
                py-3
                rounded-xl
                hover:bg-[#3D6BE5]
                transition
                font-medium
              "
            >
              인증번호 발송
            </button>
          </>
        )}

        {step === 3 && (
          <>
            <div className="mb-6">
              <h3 className="font-semibold text-lg">
                이메일 인증
              </h3>

              <p className="text-sm text-gray-500 mt-1">
                이메일로 받은 인증번호를 입력해주세요.
              </p>
            </div>

            <input
              type="text"
              placeholder="인증번호 6자리"
              value={verificationCode}
              onChange={(e) =>
                setVerificationCode(e.target.value)
              }
              className="
                w-full
                border
                border-gray-200
                rounded-xl
                p-3
                mb-4
                focus:outline-none
                focus:ring-2
                focus:ring-[#4F7DF3]
              "
            />

            <button
              onClick={handleVerifyCode}
              className="
                w-full
                bg-[#4F7DF3]
                text-white
                py-3
                rounded-xl
                hover:bg-[#3D6BE5]
                transition
                font-medium
              "
            >
              인증 완료
            </button>
          </>
        )}

        {step === 4 && (
          <>
            <div className="mb-6">
              <h3 className="font-semibold text-lg">
                개인정보 입력
              </h3>

              <p className="text-sm text-gray-500 mt-1">
                서비스 이용에 필요한 정보를 입력해주세요.
              </p>
            </div>

            <input
              type="text"
              placeholder="이름"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="
                w-full
                border
                border-gray-200
                rounded-xl
                p-3
                mb-4
                focus:outline-none
                focus:ring-2
                focus:ring-[#4F7DF3]
              "
            />

            <input
              type="password"
              placeholder="비밀번호"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="
                w-full
                border
                border-gray-200
                rounded-xl
                p-3
                mb-4
                focus:outline-none
                focus:ring-2
                focus:ring-[#4F7DF3]
              "
            />

            <input
              type="password"
              placeholder="비밀번호 확인"
              value={passwordConfirm}
              onChange={(e) =>
                setPasswordConfirm(e.target.value)
              }
              className="
                w-full
                border
                border-gray-200
                rounded-xl
                p-3
                mb-4
                focus:outline-none
                focus:ring-2
                focus:ring-[#4F7DF3]
              "
            />

            <input
              type="text"
              placeholder="집주소 (선택)"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="
                w-full
                border
                border-gray-200
                rounded-xl
                p-3
                mb-6
                focus:outline-none
                focus:ring-2
                focus:ring-[#4F7DF3]
              "
            />

            <button
              onClick={handleSignup}
              className="
                w-full
                bg-[#4F7DF3]
                text-white
                py-3
                rounded-xl
                hover:bg-[#3D6BE5]
                transition
                font-medium
              "
            >
              가입 완료
            </button>
          </>
        )}

        <div className="text-center mt-6">
          <button
            onClick={() => navigate("/login")}
            className="
              text-sm
              text-gray-500
              hover:text-[#4F7DF3]
            "
          >
            이미 계정이 있으신가요? 로그인
          </button>
        </div>
      </div>
    </div>
  );
}

export default SignupPage;
