import { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";


const navItems = [
  { to: "/main", label: "모임 현황", end: true },
  { to: "/mypage", label: "마이페이지", end: true },
];

function ArrowRightIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 20 20" className="h-4 w-4 shrink-0" fill="none">
      <path
        d="M3.75 10h12.5M10.5 4.25 16.25 10l-5.75 5.75"
        stroke="currentColor"
        strokeWidth="2.75"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function AppHeader() {
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const closeMenu = () => setMenuOpen(false);

  return (
    <header className="sticky top-0 z-50 border-b border-slate-100 bg-white shadow-[0_2px_8px_rgba(15,23,42,0.035)]">
      <div className="mx-auto flex h-[76px] max-w-6xl items-center justify-between px-5 sm:px-6">
        <button type="button" onClick={() => navigate("/main")} aria-label="모여ON 모임 현황으로 이동" title="모임 현황으로 이동" className="flex shrink-0 items-center gap-3 border-0 bg-transparent p-0 text-left">
          <span className="flex h-[42px] w-[42px] items-center justify-center rounded-xl bg-[#4F7DF3] text-2xl font-black leading-none text-white shadow-[0_4px_10px_rgba(79,125,243,0.2)]">
            M
          </span>
          <span className="text-2xl font-black leading-none tracking-[-0.06em] text-slate-900">
            모여ON
          </span>
        </button>

        <nav aria-label="주요 메뉴" className="hidden items-center gap-1 md:flex">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={({ isActive }) => "rounded-xl px-4 py-2.5 text-sm font-semibold transition " + (isActive ? "bg-blue-50 text-[#3D6BE5]" : "text-slate-600 hover:bg-slate-50 hover:text-slate-900")}>
              {item.label}
            </NavLink>
          ))}
          <button type="button" onClick={() => navigate("/create-meeting")} className="ml-2 inline-flex items-center gap-2 rounded-2xl bg-[#4F7DF3] px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#3D6BE5] focus:outline-none focus:ring-4 focus:ring-blue-100">
            모임 생성하기 <ArrowRightIcon />
          </button>
        </nav>

        <button type="button" aria-label={menuOpen ? "메뉴 닫기" : "메뉴 열기"} aria-expanded={menuOpen} aria-controls="mobile-main-menu" onClick={() => setMenuOpen((open) => !open)} className="flex h-11 w-11 flex-col items-center justify-center gap-[5px] rounded-xl border border-slate-200 bg-white text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 md:hidden">
          <span className={"h-0.5 w-5 rounded-full bg-current transition-transform " + (menuOpen ? "translate-y-[7px] rotate-45" : "")} />
          <span className={"h-0.5 w-5 rounded-full bg-current transition-opacity " + (menuOpen ? "opacity-0" : "opacity-100")} />
          <span className={"h-0.5 w-5 rounded-full bg-current transition-transform " + (menuOpen ? "-translate-y-[7px] -rotate-45" : "")} />
        </button>
      </div>

      {menuOpen && (
        <nav id="mobile-main-menu" aria-label="모바일 주요 메뉴" className="absolute left-0 right-0 top-full border-t border-slate-100 bg-white px-5 pb-5 pt-3 shadow-lg md:hidden">
          <div className="mx-auto grid max-w-6xl gap-1">
            {navItems.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} onClick={closeMenu} className={({ isActive }) => "rounded-xl px-4 py-3 text-sm font-semibold transition " + (isActive ? "bg-blue-50 text-[#3D6BE5]" : "text-slate-700 hover:bg-slate-50")}>
                {item.label}
              </NavLink>
            ))}
            <button type="button" onClick={() => { closeMenu(); navigate("/create-meeting"); }} className="mt-2 flex w-full items-center justify-center gap-2 rounded-2xl bg-[#4F7DF3] px-5 py-3.5 text-sm font-bold text-white shadow-sm transition hover:bg-[#3D6BE5]">
              모임 생성하기 <ArrowRightIcon />
            </button>
          </div>
        </nav>
      )}
    </header>
  );
}

export default AppHeader;
