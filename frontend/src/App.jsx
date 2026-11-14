import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import LandingPage from "./pages/home/LandingPage";
import LoginPage from "./pages/auth/LoginPage";
import SignupPage from "./pages/auth/SignupPage";
import MainPage from "./pages/home/MainPage";
import CreateMeetingPage from "./pages/meeting/CreateMeetingPage";
import MeetingPlacesPage from "./pages/meeting/MeetingPlacesPage";
import MeetingTimeOptionsPage from "./pages/meeting/MeetingTimeOptionsPage";
import MeetingVotePage from "./pages/meeting/MeetingVotePage";
import MeetingResultPage from "./pages/meeting/MeetingResultPage";
import NicknamePage from "./pages/meeting/NicknamePage";
import MeetingStatusPage from "./pages/meeting/MeetingStatusPage";
import MyPage from "./pages/MyPage/MyPage";
import RequireAuth from "./components/RequireAuth";
import AuthenticatedLayout from "./components/AuthenticatedLayout";
import NotFoundPage from "./pages/NotFoundPage";


function App() {
  return (
    <BrowserRouter><Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route element={<RequireAuth><AuthenticatedLayout /></RequireAuth>}>
        <Route path="/main" element={<MainPage />} />
        <Route path="/mypage" element={<MyPage />} />
        <Route path="/ongoing-meetings" element={<MainPage />} />
        <Route path="/create-meeting" element={<CreateMeetingPage />} />
        <Route path="/meeting/setup/availability" element={<MeetingTimeOptionsPage />} />
        <Route path="/meeting/setup/place" element={<MeetingPlacesPage />} />
        <Route path="/meeting/setup/review" element={<MeetingVotePage />} />
        <Route path="/meeting/:meetingId/status" element={<MeetingStatusPage />} />
        <Route path="/meeting/:meetingId/result" element={<MeetingResultPage />} />
        <Route path="/meeting/setup/time-options" element={<Navigate to="/meeting/setup/availability" replace />} />
        <Route path="/meeting/setup/places" element={<Navigate to="/meeting/setup/place" replace />} />
        <Route path="/meeting/setup/vote" element={<Navigate to="/meeting/setup/review" replace />} />
        <Route path="/meeting/setup/result" element={<Navigate to="/meeting/setup/review" replace />} />
      </Route>

      <Route path="/invite/:inviteToken" element={<NicknamePage />} />
      <Route path="/invite/:inviteToken/nickname" element={<NicknamePage />} />
      <Route path="/invite/:inviteToken/availability" element={<MeetingTimeOptionsPage />} />
      <Route path="/invite/:inviteToken/place" element={<MeetingPlacesPage />} />
      <Route path="/invite/:inviteToken/review" element={<MeetingVotePage />} />
      <Route path="/invite/:inviteToken/status" element={<MeetingStatusPage />} />
      <Route path="/invite/:inviteToken/result" element={<MeetingResultPage />} />

      <Route path="*" element={<NotFoundPage />} />

    </Routes></BrowserRouter>
  );
}

export default App;

