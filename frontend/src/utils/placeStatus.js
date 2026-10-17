// 마감 후 장소 결정 상태(서버 placeStatus)와 결정 방식(decisionType)을 화면에서 다루는 도우미.

export const PLACE_STATUS = {
  PENDING: "PENDING", // 후보를 분석하는 중
  AWAITING_HOST: "AWAITING_HOST", // 호스트가 후보 중에서 고르기를 기다리는 중
  DECIDED: "DECIDED", // 장소가 정해짐
  NONE: "NONE", // 정할 장소가 없어 추후 협의
};

// 이전 서버 응답처럼 placeStatus가 없으면 이미 정해진 것으로 본다.
export const placeStatusOf = (result) => result?.placeStatus || PLACE_STATUS.DECIDED;

const DECISION_LABELS = {
  MAJORITY: "최다 득표",
  TIE_BREAK: "동률 조정",
  SPATIAL: "지역 합의",
  HOST_CHOICE: "호스트 선택",
  AUTO_PICK: "자동 선정",
};

export const decisionLabel = (decisionType) => DECISION_LABELS[decisionType] || decisionType || "";

// 목록·캘린더에서 장소 칸에 보여 줄 문구. 정해지지 않았으면 상태를 알려 준다.
export const placeSummaryText = (result, isHost = true) => {
  const status = placeStatusOf(result);
  if (status === PLACE_STATUS.DECIDED) return result.selectedPlace;
  if (status === PLACE_STATUS.PENDING) return "후보 분석 중";
  if (status === PLACE_STATUS.AWAITING_HOST) return isHost ? "장소 선택 대기 중" : "장소 결정 중";
  return "추후 협의";
};

// 호스트 선택 시한까지 남은 시간 문구. 시한이 없으면 빈 문자열, 지났으면 "곧 자동 선정".
export const remainingText = (deadline, now) => {
  if (!deadline) return "";
  const minutes = Math.ceil((new Date(deadline).getTime() - now) / 60000);
  if (minutes <= 0) return "곧 자동 선정됩니다";
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  if (hours === 0) return `${rest}분 남음`;
  return rest === 0 ? `${hours}시간 남음` : `${hours}시간 ${rest}분 남음`;
};
