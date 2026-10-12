import { getErrorCode, getErrorMessage } from "../services/api";

// 화면에 표시할 문구를 따로 정해야 하는 서버 에러 코드. 그 외에는 서버가 준 message를 그대로 쓴다.
const MESSAGES = {
  POLICY_LOCKED: "게스트 응답이 있어 장소 선택은 수정 불가합니다.",
  MEETING_NOT_FOUND: "유효하지 않은 초대 링크입니다.",
  INVITE_EXPIRED: "만료된 초대 링크입니다.",
  MEETING_NOT_CLOSED: "아직 응답이 마감되지 않았습니다.",
  RESULT_NOT_READY: "결과를 준비하고 있습니다. 잠시 후 다시 확인해주세요.",
};

export const getMeetingErrorMessage = (error, fallback) =>
  MESSAGES[getErrorCode(error)] || getErrorMessage(error, fallback);
