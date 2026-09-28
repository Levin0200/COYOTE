import { useMemo, useState } from "react";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];
const toDateKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;

function AvailabilityCalendar({ selectedDates = [], onChange }) {
  const [visibleMonth, setVisibleMonth] = useState(() => {
    const initial = selectedDates[0] ? new Date(`${selectedDates[0]}T00:00:00`) : new Date();
    return new Date(initial.getFullYear(), initial.getMonth(), 1);
  });
  const days = useMemo(() => {
    const year = visibleMonth.getFullYear();
    const month = visibleMonth.getMonth();
    return [...Array.from({ length: new Date(year, month, 1).getDay() }, () => null), ...Array.from({ length: new Date(year, month + 1, 0).getDate() }, (_, index) => new Date(year, month, index + 1))];
  }, [visibleMonth]);
  const todayKey = toDateKey(new Date());
  const toggleDate = (date) => {
    const key = toDateKey(date);
    if (key < todayKey) return;
    onChange(selectedDates.includes(key) ? selectedDates.filter((item) => item !== key) : [...selectedDates, key].sort());
  };

  return (
    <div className="rounded-[2rem] border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
      <div className="mb-6 flex items-center justify-between">
        <button type="button" onClick={() => setVisibleMonth((d) => new Date(d.getFullYear(), d.getMonth() - 1, 1))} className="h-10 w-10 rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-50" aria-label="이전 달">←</button>
        <h2 className="text-xl font-black text-slate-900">{visibleMonth.getFullYear()}년 {visibleMonth.getMonth() + 1}월</h2>
        <button type="button" onClick={() => setVisibleMonth((d) => new Date(d.getFullYear(), d.getMonth() + 1, 1))} className="h-10 w-10 rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-50" aria-label="다음 달">→</button>
      </div>
      <div className="grid grid-cols-7 gap-2 text-center">
        {WEEKDAYS.map((day) => <div key={day} className="py-2 text-xs font-bold text-slate-400">{day}</div>)}
        {days.map((date, index) => {
          if (!date) return <div key={`empty-${index}`} />;
          const key = toDateKey(date);
          const selected = selectedDates.includes(key);
          const disabled = key < todayKey;
          return <button key={key} type="button" disabled={disabled} onClick={() => toggleDate(date)} aria-pressed={selected} className={`aspect-square rounded-2xl text-sm font-bold transition ${selected ? "bg-blue-600 text-white shadow-md shadow-blue-200" : disabled ? "cursor-not-allowed text-slate-300" : "bg-slate-50 text-slate-700 hover:bg-blue-50 hover:text-blue-600"}`}>{date.getDate()}</button>;
        })}
      </div>
    </div>
  );
}

export default AvailabilityCalendar;
