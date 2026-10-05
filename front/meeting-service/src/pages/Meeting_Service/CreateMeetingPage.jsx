import { useState } from "react";
import { useNavigate } from "react-router-dom";

const DRAFT_KEY = "coyoteMeetingDraft";
const readDraft = () => { try { return JSON.parse(sessionStorage.getItem(DRAFT_KEY) || "{}"); } catch { return {}; } };

function CreateMeetingPage() {
  const navigate = useNavigate();
  const savedDraft = readDraft();
  const [title, setTitle] = useState(savedDraft.title || "");
  const [description, setDescription] = useState(savedDraft.description || "");
  const [candidateStartDate, setCandidateStartDate] = useState(savedDraft.candidateStartDate || "");
  const [candidateEndDate, setCandidateEndDate] = useState(savedDraft.candidateEndDate || "");
  const [responseDeadline, setResponseDeadline] = useState(savedDraft.responseDeadline || savedDraft.responseDueDate || "");
  const [linkExpirationDate, setLinkExpirationDate] = useState(savedDraft.linkExpirationDate || "");

  const saveDraftAndNext = () => {
    if (!title.trim() || !responseDeadline) return alert("모임 제목과 응답 마감일시를 입력해주세요.");
    if (!candidateStartDate || !candidateEndDate) return alert("후보 날짜 범위를 입력해주세요.");
    if (candidateStartDate > candidateEndDate) return alert("후보 시작일은 종료일보다 늦을 수 없습니다.");
    const due = new Date(responseDeadline);
    const expiration = linkExpirationDate ? new Date(linkExpirationDate) : null;
    if (due <= new Date()) return alert("응답 마감일시는 현재 이후로 설정해주세요.");
    if (expiration && expiration <= due) return alert("링크 만료일시는 응답 마감일시 이후로 설정해주세요.");
    sessionStorage.setItem(DRAFT_KEY, JSON.stringify({ ...savedDraft, title: title.trim(), description: description.trim(), candidateStartDate, candidateEndDate, responseDeadline, linkExpirationDate }));
    navigate("/meeting/setup/availability");
  };

  return (
    <div className="min-h-screen bg-white px-6 py-12 text-gray-900">
      <div className="mx-auto max-w-5xl">
        <header className="mb-12"><p className="mb-3 font-semibold text-[#4F7DF3]">Create new meeting</p><h1 className="mb-4 text-5xl font-black tracking-tight">모임 생성</h1><p className="text-lg text-gray-500">기본 정보와 응답 기간을 설정해 주세요.</p></header>
        <main className="grid gap-8 lg:grid-cols-3">
          <section className="rounded-[2rem] border border-gray-100 bg-white p-8 shadow-xl lg:col-span-2">
            <div className="space-y-5">
              <label className="block"><span className="mb-2 block text-sm font-bold text-slate-700">모임 제목 *</span><input value={title} onChange={(e) => setTitle(e.target.value)} className="w-full rounded-2xl border border-gray-200 bg-slate-50 px-5 py-4 outline-none focus:ring-2 focus:ring-[#4F7DF3]" placeholder="예) 캡스톤 프로젝트 회식" /></label>
              <label className="block"><span className="mb-2 block text-sm font-bold text-slate-700">모임 설명</span><textarea value={description} onChange={(e) => setDescription(e.target.value)} className="h-32 w-full resize-none rounded-2xl border border-gray-200 bg-slate-50 px-5 py-4 outline-none focus:ring-2 focus:ring-[#4F7DF3]" placeholder="모임 목적이나 안내사항을 적어주세요." /></label>
              <div className="grid gap-4 sm:grid-cols-2">
                <label><span className="mb-2 block text-sm font-bold text-slate-700">후보 시작일 *</span><input type="date" value={candidateStartDate} max={candidateEndDate || undefined} onChange={(e) => setCandidateStartDate(e.target.value)} className="w-full rounded-2xl border border-gray-200 bg-slate-50 px-4 py-4 outline-none focus:ring-2 focus:ring-[#4F7DF3]" /></label>
                <label><span className="mb-2 block text-sm font-bold text-slate-700">후보 종료일 *</span><input type="date" value={candidateEndDate} min={candidateStartDate || undefined} onChange={(e) => setCandidateEndDate(e.target.value)} className="w-full rounded-2xl border border-gray-200 bg-slate-50 px-4 py-4 outline-none focus:ring-2 focus:ring-[#4F7DF3]" /></label>
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <label><span className="mb-2 block text-sm font-bold text-slate-700">응답 마감일시 *</span><input type="datetime-local" step="60" value={responseDeadline} onChange={(e) => setResponseDeadline(e.target.value)} className="w-full rounded-2xl border border-gray-200 bg-slate-50 px-4 py-4 outline-none focus:ring-2 focus:ring-[#4F7DF3]" /></label>
                <label><span className="mb-2 block text-sm font-bold text-slate-700">링크 만료일시</span><input type="datetime-local" step="60" value={linkExpirationDate} onChange={(e) => setLinkExpirationDate(e.target.value)} className="w-full rounded-2xl border border-gray-200 bg-slate-50 px-4 py-4 outline-none focus:ring-2 focus:ring-[#4F7DF3]" /></label>
              </div>
              <button onClick={saveDraftAndNext} className="w-full rounded-2xl bg-[#4F7DF3] py-4 font-bold text-white shadow-lg shadow-blue-200 transition hover:bg-[#3D6BE5]">가능한 날짜 선택하기</button>
            </div>
          </section>
          <aside className="rounded-[2rem] border border-gray-100 bg-slate-50 p-6 shadow-sm"><h2 className="mb-5 text-lg font-black">생성 흐름</h2>{['기본 정보', '가능한 날짜', '장소 응답', '최종 확인'].map((text, index) => <div key={text} className="mb-4 flex items-center gap-3"><div className={`flex h-8 w-8 items-center justify-center rounded-xl font-black ${index === 0 ? 'bg-blue-600 text-white' : 'bg-blue-50 text-[#4F7DF3]'}`}>{index + 1}</div><p className="font-bold text-gray-700">{text}</p></div>)}</aside>
        </main>
      </div>
    </div>
  );
}

export default CreateMeetingPage;
