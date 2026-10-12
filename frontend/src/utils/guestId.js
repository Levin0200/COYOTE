// 게스트 식별자(UUID)를 초대 링크별로 브라우저(localStorage)에 보관한다.
// 서버는 이 값을 X-Guest-Id 헤더로 받아 같은 게스트의 응답을 수정(upsert)한다.
const keyOf = (inviteToken) => `coyote_guest_${inviteToken}`;

export const getGuestId = (inviteToken) => {
  try {
    return localStorage.getItem(keyOf(inviteToken));
  } catch {
    return null;
  }
};

export const getOrCreateGuestId = (inviteToken) => {
  const saved = getGuestId(inviteToken);
  if (saved) return saved;
  const guestId = globalThis.crypto.randomUUID();
  try {
    localStorage.setItem(keyOf(inviteToken), guestId);
  } catch {
    // 저장소를 쓸 수 없으면 이번 방문에만 유효한 식별자가 된다.
  }
  return guestId;
};
