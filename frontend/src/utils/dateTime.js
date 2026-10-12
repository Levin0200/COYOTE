// datetime-local 값(예: "2026-10-12T18:30", 시간대 없음)을 서버가 요구하는 UTC ISO 문자열로 바꾼다.
// 값이 비어 있으면 null을 돌려준다(서버는 빈 문자열을 400으로 거절한다).
export const toApiInstant = (value) => {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toISOString();
};
