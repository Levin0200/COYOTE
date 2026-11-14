import { Outlet } from "react-router-dom";
import AppHeader from "./AppHeader";

// 로그인한 사용자가 보는 모든 화면에 동일한 상단 내비게이션을 제공한다.
// 게스트 초대 링크 화면은 로그인 없이 접근하므로 이 레이아웃을 사용하지 않는다.
function AuthenticatedLayout() {
  return (
    <div className="min-h-screen bg-slate-50">
      <AppHeader />
      <Outlet />
    </div>
  );
}

export default AuthenticatedLayout;
