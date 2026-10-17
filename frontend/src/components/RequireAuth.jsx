import { useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { checkSession } from "../services/authService";
import { clearAuthenticated, markAuthenticated } from "../utils/authSession";

function RequireAuth({ children }) {
  const location = useLocation();
  const [state, setState] = useState("checking");
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let active = true;
    checkSession()
      .then((authenticated) => {
        if (!active) return;
        if (authenticated) markAuthenticated(); else clearAuthenticated();
        setState(authenticated ? "authenticated" : "anonymous");
      })
      .catch(() => { if (active) setState("unavailable"); });
    return () => { active = false; };
  }, [retry]);

  if (state === "checking") {
    return <div className="flex min-h-screen items-center justify-center bg-slate-50"><p className="font-bold text-slate-500">로그인 상태를 확인하고 있습니다...</p></div>;
  }
  if (state === "unavailable") {
    return <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6"><section className="max-w-md rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black">서버에 연결할 수 없습니다.</h1><p className="mt-3 text-slate-500">로그인 정보는 지우지 않았습니다. 서버 상태를 확인한 뒤 다시 시도해 주세요.</p><button type="button" onClick={() => { setState("checking"); setRetry((value) => value + 1); }} className="mt-6 rounded-2xl bg-blue-600 px-6 py-3 font-bold text-white">다시 시도</button></section></div>;
  }
  if (state === "anonymous") return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return children;
}

export default RequireAuth;
