import { Link } from "react-router-dom";

function NotFoundPage() {
  return <main className="flex min-h-screen items-center justify-center bg-slate-50 px-6"><section className="max-w-md rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm"><p className="text-sm font-black text-blue-600">404</p><h1 className="mt-2 text-3xl font-black text-slate-950">페이지를 찾을 수 없습니다.</h1><p className="mt-3 text-slate-500">주소가 잘못되었거나 더 이상 사용하지 않는 화면입니다.</p><Link to="/" className="mt-6 inline-flex rounded-2xl bg-blue-600 px-6 py-3 font-bold text-white">처음으로</Link></section></main>;
}

export default NotFoundPage;
