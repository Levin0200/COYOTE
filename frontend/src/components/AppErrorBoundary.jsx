import { Component } from "react";

class AppErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { failed: false };
  }

  static getDerivedStateFromError() {
    return { failed: true };
  }

  componentDidCatch(error, info) {
    console.error("Unhandled UI error", error, info);
  }

  render() {
    if (!this.state.failed) return this.props.children;
    return <main className="flex min-h-screen items-center justify-center bg-slate-50 px-6"><section className="max-w-md rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-sm"><h1 className="text-2xl font-black text-slate-950">화면을 표시하지 못했습니다.</h1><p className="mt-3 text-slate-500">입력 중인 내용은 브라우저에 남아 있습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.</p><button type="button" onClick={() => window.location.reload()} className="mt-6 rounded-2xl bg-blue-600 px-6 py-3 font-bold text-white">새로고침</button></section></main>;
  }
}

export default AppErrorBoundary;
