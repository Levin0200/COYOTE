import { isFinalResult, placeSummaryText } from "../../utils/placeStatus";

export const toConfirmedMeetings = (meetings) => meetings
  .filter((meeting) => isFinalResult(meeting.result))
  .map((meeting) => ({
    id: meeting.meetingId,
    date: meeting.result.selectedDate,
    title: meeting.title,
    description: meeting.description || "",
    location: meeting.placeRegion || "",
    place: placeSummaryText(meeting.result),
    memberCount: meeting.participantCount,
  }));

export const dateKey = (date = new Date()) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;

export const buildCalendarDays = (date) => {
  const year = date.getFullYear();
  const month = date.getMonth();
  const firstDay = new Date(year, month, 1).getDay();
  const lastDate = new Date(year, month + 1, 0).getDate();
  const previousMonthLastDate = new Date(year, month, 0).getDate();
  const days = [];
  for (let i = firstDay - 1; i >= 0; i -= 1) days.push({ day: previousMonthLastDate - i, currentMonth: false, date: null });
  for (let day = 1; day <= lastDate; day += 1) days.push({ day, currentMonth: true, date: `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}` });
  for (let day = 1; days.length < 42; day += 1) days.push({ day, currentMonth: false, date: null });
  return days;
};

export const formatCalendarDate = (value) => {
  const [year, month, day] = value.split("-");
  return `${year}년 ${Number(month)}월 ${Number(day)}일`;
};

export const voteSummary = (votes) => Object.fromEntries((votes?.dateCounts || []).map(({ date, count }) => [date, count]));

export const maxVoteCount = (votes) => {
  const values = Object.values(voteSummary(votes));
  return values.length ? Math.max(...values) : 0;
};

export const mostVotedDates = (votes) => {
  const counts = voteSummary(votes);
  const max = maxVoteCount(votes);
  return Object.entries(counts).filter(([, count]) => count === max).map(([date]) => date);
};
