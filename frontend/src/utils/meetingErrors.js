import { getErrorCode, getErrorMessage } from "../services/api";

// 화면에 표시할 문구를 따로 정해야 하는 서버 에러 코드. 그 외에는 서버가 준 message를 그대로 쓴다.
const MESSAGES = {
  POLICY_LOCKED: "게스트 응답이 있어 장소 선택은 수정 불가합니다.",
  MEETING_NOT_FOUND: "유효하지 않은 초대 링크입니다.",
  INVITE_EXPIRED: "만료된 초대 링크입니다.",
  MEETING_NOT_CLOSED: "아직 응답이 마감되지 않았습니다.",
  RESULT_NOT_READY: "결과를 준비하고 있습니다. 잠시 후 다시 확인해주세요.",
  TOO_MANY_REQUESTS: "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
  PLACE_NOT_FOUND: "존재하지 않는 장소입니다. 다시 검색해서 선택해주세요.",
  PLACE_SEARCH_UNAVAILABLE: "지금은 장소 검색을 사용할 수 없습니다. 잠시 후 다시 시도해주세요.",
  PLACE_SEARCH_FAILED: "장소 검색 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.",
  PLACE_SEARCH_NOT_ALLOWED: "이 모임에서는 장소를 검색해 제안할 수 없습니다.",
};

export const getMeetingErrorMessage = (error, fallback) =>
  MESSAGES[getErrorCode(error)] || getErrorMessage(error, fallback);
