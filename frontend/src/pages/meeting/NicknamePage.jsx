import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { fetchInviteInfo, fetchMyGuestResponse, joinMeeting } from "../../services/meetingService";
import { getErrorCode } from "../../services/api";
import { getMeetingErrorMessage } from "../../utils/meetingErrors";
import { getGuestId, getOrCreateGuestId } from "../../utils/guestId";

function NicknamePage() {
  const navigate = useNavigate();
  const { inviteToken } = useParams();
  const [invite, setInvite] = useState(null);
  const [existingResponse, setExistingResponse] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [nickname, setNickname] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const info = await fetchInviteInfo(inviteToken);
        if (cancelled) return;
        setInvite(info);
        const guestId = getGuestId(inviteToken);
        if (!guestId) return;
        try {
          const mine = await fetchMyGuestResponse(inviteToken, guestId);
          if (cancelled) return;
          setExistingResponse(mine);
          setNickname(mine.nickname || "");
        } catch (error) {
          if (getErrorCode(error) !== "RESPONSE_NOT_FOUND") throw error;
        }
      } catch (error) {
        if (!cancelled) setLoadError({ code: getErrorCode(error), message: getMeetingErrorMessage(error) });
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [inviteToken]);

  const expired = loadError?.code === "INVITE_EXPIRED";
  const blocked = loading || Boolean(loadError) || submitting;

  const continueFlow = async () => {
    if (!invite) return;
    if (invite.closed) return navigate(`/invite/${inviteToken}/status`);
    const trimmed = nickname.trim();
    if (!trimmed) return alert("닉네임을 입력해주세요.");
    const guestId = getOrCreateGuestId(inviteToken);
    setSubmitting(true);
    try {
      await joinMeeting(inviteToken, guestId, trimmed);
      sessionStorage.setItem(`coyoteGuestDraft_${inviteToken}`, JSON.stringify({ ...(existingResponse || {}), guestId, inviteToken, nickname: trimmed, candidateStartDate: invite.candidateStartDate, candidateEndDate: invite.candidateEndDate, placePolicy: invite.placePolicy }));
      navigate(`/invite/${inviteToken}/availability`);
    } catch (error) {
      alert(getMeetingErrorMessage(error));
      if (getErrorCode(error) === "MEETING_CLOSED") navigate(`/invite/${inviteToken}/status`);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-6"><div className="w-full max-w-lg rounded-[2rem] border border-slate-200 bg-white p-8 text-center shadow-xl sm:p-10"><div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-600 text-2xl font-black text-white shadow-lg shadow-blue-200">C</div><p className="mt-6 text-sm font-bold text-blue-600">COYOTE INVITATION</p><h1 className="mt-2 text-3xl font-black text-slate-950">{invite?.title || "초대받은 모임"}</h1><p className="mt-3 text-slate-500">참여할 때 사용할 닉네임을 입력해 주세요.</p>{existingResponse && <div className="mt-5 rounded-2xl border border-blue-100 bg-blue-50 p-4 text-sm font-semibold text-blue-700">기존 참여 정보를 찾았습니다. 같은 응답을 이어서 확인하거나 수정합니다.</div>}{expired && <div className="mt-5 rounded-2xl border border-red-100 bg-red-50 p-4 text-sm font-semibold text-red-600">이 초대 링크는 만료되었습니다.</div>}{loadError && !expired && <div className="mt-5 rounded-2xl border border-red-100 bg-red-50 p-4 text-sm font-semibold text-red-600">{loadError.message}</div>}<input value={nickname} onChange={(e) => setNickname(e.target.value)} onKeyDown={(e) => e.key === "Enter" && !blocked && continueFlow()} className="mt-7 w-full rounded-2xl border border-slate-200 bg-slate-50 px-5 py-4 text-center text-lg font-bold outline-none focus:ring-2 focus:ring-blue-500" placeholder="닉네임" maxLength={20} disabled={loading || Boolean(loadError)} /><button onClick={continueFlow} disabled={blocked} className="mt-4 w-full rounded-2xl bg-blue-600 py-4 font-bold text-white shadow-md hover:bg-blue-700 disabled:bg-slate-300">{existingResponse ? "내 응답으로 이동" : "참여하기"}</button><p className="mt-5 text-xs leading-5 text-slate-400">로그인 없이 참여할 수 있습니다.<br />현재 브라우저에만 임시 참여 정보가 저장됩니다.</p></div></div>
  );
}

export default NicknamePage;
