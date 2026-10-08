import { useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import AvailabilityCalendar from "../../components/meeting/AvailabilityCalendar";
import { formatSelectedDate } from "../../utils/dateFormat";
import { getMeetingByInviteToken } from "../../services/meetingStorage";

const readJson = (key) => { try { return JSON.parse(sessionStorage.getItem(key) || "{}"); } catch { return {}; } };

function MeetingTimeOptionsPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { inviteToken } = useParams();
  const isGuest = location.pathname.startsWith("/invite/");
  const key = isGuest ? `coyoteGuestDraft_${inviteToken}` : "coyoteMeetingDraft";
  const draft = readJson(key);
  const meeting = isGuest ? getMeetingByInviteToken(inviteToken) : null;
  const candidateStartDate = draft.candidateStartDate || meeting?.candidateStartDate;
  const candidateEndDate = draft.candidateEndDate || meeting?.candidateEndDate;
  const [selectedDates, setSelectedDates] = useState(() => (draft.selectedDates || []).filter((date) =>
    (!candidateStartDate || date >= candidateStartDate) &&
    (!candidateEndDate || date <= candidateEndDate)
  ));

  const handleSelectedDatesChange = (dates) => {
    const datesInRange = dates.filter((date) =>
      (!candidateStartDate || date >= candidateStartDate) &&
      (!candidateEndDate || date <= candidateEndDate)
    );
    setSelectedDates(datesInRange);
  };

  const saveAndNext = () => {
    if (!selectedDates.length) return alert("가능한 날짜를 한 개 이상 선택해주세요.");
    sessionStorage.setItem(key, JSON.stringify({ ...draft, candidateStartDate, candidateEndDate, selectedDates }));
    navigate(isGuest ? `/invite/${inviteToken}/place` : "/meeting/setup/place");
  };

  return (
    <div className="min-h-screen bg-slate-50 px-6 py-10 text-slate-800">
      <div className="mx-auto max-w-5xl">
        <header className="mb-10"><p className="mb-3 text-sm font-semibold text-blue-600">Availability</p><h1 className="mb-3 text-4xl font-black text-slate-950">가능한 날짜 선택</h1><p className="text-slate-500">참여할 수 있는 날짜를 모두 눌러주세요. 다시 누르면 선택이 해제됩니다.</p></header>
        <main className="grid gap-8 lg:grid-cols-3">
          <section className="lg:col-span-2">
            {candidateStartDate && candidateEndDate && <p className="mb-4 text-sm font-semibold text-slate-600">선택 가능 기간: {candidateStartDate} ~ {candidateEndDate}</p>}
            <AvailabilityCalendar selectedDates={selectedDates} onChange={handleSelectedDatesChange} minDate={candidateStartDate} maxDate={candidateEndDate} />
          </section>
          <aside className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
            <div className="mb-5 flex items-center justify-between"><h2 className="text-lg font-bold text-slate-900">선택한 날짜</h2><span className="rounded-full bg-blue-50 px-3 py-1 text-sm font-bold text-blue-600">{selectedDates.length}일</span></div>
            <div className="space-y-3">{selectedDates.length ? selectedDates.map((date) => <div key={date} className="rounded-2xl border border-blue-100 bg-blue-50 p-4 font-bold text-blue-800">{formatSelectedDate(date)}</div>) : <p className="text-sm leading-6 text-slate-400">아직 선택한 날짜가 없습니다.<br />캘린더에서 가능한 날짜를 선택하세요.</p>}</div>
          </aside>
        </main>
        <div className="mt-8 flex gap-4"><button onClick={() => navigate(-1)} className="w-1/3 rounded-2xl border border-slate-200 bg-white py-4 font-bold text-slate-700 hover:bg-slate-100">이전으로</button><button onClick={saveAndNext} className="w-2/3 rounded-2xl bg-blue-600 py-4 font-bold text-white shadow-sm hover:bg-blue-700">장소 응답하기</button></div>
      </div>
    </div>
  );
}

export default MeetingTimeOptionsPage;
