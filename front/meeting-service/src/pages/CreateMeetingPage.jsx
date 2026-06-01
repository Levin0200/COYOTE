import { useState } from "react";

function CreateMeetingPage() {

  // const [현재값, 값을 변경하는 함수] = useState(초기값); 얘가 화면을 다시 그리는 애임
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("");
  const [description, setDescription] = useState("");

  //함수 선언 및 asyc 비동기 함수로 내부에서 await 사용이 가능함 / fetch()는 서버에 http 요청 보내는 애임, 주소는 내가 만든 서버 주소
  //method 방식이 POST임 CRUD = POST, GET, PUT, DLETE
  //headers: 내가 보내는 데이터가 JSON 형식이다 라는걸 알려주는 역할
  //리액트는 jsx를 씀 그리고 얘 구조를 대충 보면 펑선 안에 js 구사하고 리턴부에 html을 사용함 물론 jsx니까 html+js 같이 사용 가능함

  const createMeeting = async () => {
    const response = await fetch("http://localhost:8080/api/meetings", {
      method: "POST",
      headers: {
        "Content-Type":"application/json",
      },
      // JSON 데이터 -> 문자열로 변환 = stringify

      body: JSON.stringify({
        hostId: 1,
        title,
        category,
        description,
      }),
    });

    const data = await response.json();
    console.log(data);
    alert("모임 생성 완료!");
  }
  


  return (
    <div className="min-h-screen bg-[#0B1020] text-white overflow-hidden">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(59,130,246,0.25),transparent_35%),radial-gradient(circle_at_bottom_right,rgba(168,85,247,0.18),transparent_35%)]" />

      <div className="relative max-w-5xl mx-auto px-6 py-8">
        <header className="mb-14">
          <p className="text-sm text-blue-300 font-semibold mb-3">
            Create new meeting
          </p>
          <h1 className="text-4xl font-black tracking-tight mb-3">
            모임 생성
          </h1>
          <p className="text-slate-300">
            모임 정보를 입력하고 초대 링크 생성을 시작하세요.
          </p>
        </header>

        <main className="grid lg:grid-cols-3 gap-8">
          <section className="lg:col-span-2 bg-white/10 backdrop-blur-2xl border border-white/10 rounded-[2rem] p-8 shadow-2xl">
            <div className="space-y-5">
              <input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 outline-none focus:ring-2 focus:ring-blue-400 placeholder:text-slate-400"
                placeholder="모임 제목"
                
              />

              <input
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 outline-none focus:ring-2 focus:ring-blue-400 placeholder:text-slate-400"
                placeholder="카테고리"
              />

              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 h-40 outline-none focus:ring-2 focus:ring-blue-400 resize-none placeholder:text-slate-400"
                placeholder="모임 설명"
              />

              <button 
              className="w-full bg-blue-500 hover:bg-blue-400 transition py-4 rounded-2xl font-bold shadow-lg shadow-blue-500/25"
              onClick={createMeeting}
              >
                생성하기
              </button>
            </div>
          </section>

          <aside className="bg-white/5 border border-white/10 rounded-[2rem] p-6">
            <h2 className="text-lg font-bold mb-5">생성 흐름</h2>

            <div className="space-y-4">
              {["정보 입력", "초대 링크 공유", "AI 추천 준비"].map(
                (text, index) => (
                  <div key={text} className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-xl bg-blue-500/20 text-blue-300 flex items-center justify-center font-bold">
                      {index + 1}
                    </div>
                    <p className="text-slate-200">{text}</p>
                  </div>
                )
              )}
            </div>
          </aside>
        </main>
      </div>
    </div>
  );
}

export default CreateMeetingPage;