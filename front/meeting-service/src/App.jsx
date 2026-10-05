import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import LandingPage from "./pages/front_prj/LandingPage";
import LoginPage from "./pages/front_prj/LoginPage";
import SignupPage from "./pages/front_prj/signupPage";
import OnboardingPage from "./pages/front_prj/onboardingPage";
import UserPreferencesPage from "./pages/UserPreferences/UserPreferencesPage";
import MainPage from "./pages/Main_Service/MainPage";
import CreateMeetingPage from "./pages/Meeting_Service/CreateMeetingPage";
import MeetingDetailPage from "./pages/Meeting_Service/MeetingDetailPage";
import MeetingPlacesPage from "./pages/Meeting_Service/MeetingPlacesPage";
import MeetingTimeOptionsPage from "./pages/Meeting_Service/MeetingTimeOptionsPage";
import MeetingVotePage from "./pages/Meeting_Service/MeetingVotePage";
import MeetingResultPage from "./pages/Meeting_Service/MeetingResultPage";
import NicknamePage from "./pages/Meeting_Service/NicknamePage";
import MeetingStatusPage from "./pages/Meeting_Service/MeetingStatusPage";

function App() {
  return (
    <BrowserRouter><Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/onboarding" element={<OnboardingPage />} />
      <Route path="/preferences" element={<UserPreferencesPage />} />
      <Route path="/main" element={<MainPage />} />

      <Route path="/create-meeting" element={<CreateMeetingPage />} />
      <Route path="/meeting/setup/availability" element={<MeetingTimeOptionsPage />} />
      <Route path="/meeting/setup/place" element={<MeetingPlacesPage />} />
      <Route path="/meeting/setup/review" element={<MeetingVotePage />} />
      <Route path="/meeting/:meetingId/status" element={<MeetingStatusPage />} />
      <Route path="/meeting/:meetingId/result" element={<MeetingResultPage />} />
      <Route path="/meeting/:meetingId" element={<MeetingDetailPage />} />

      <Route path="/invite/:inviteToken" element={<NicknamePage />} />
      <Route path="/invite/:inviteToken/nickname" element={<NicknamePage />} />
      <Route path="/invite/:inviteToken/availability" element={<MeetingTimeOptionsPage />} />
      <Route path="/invite/:inviteToken/place" element={<MeetingPlacesPage />} />
      <Route path="/invite/:inviteToken/review" element={<MeetingVotePage />} />
      <Route path="/invite/:inviteToken/status" element={<MeetingStatusPage />} />
      <Route path="/invite/:inviteToken/result" element={<MeetingResultPage />} />

      <Route path="/meeting/setup/time-options" element={<Navigate to="/meeting/setup/availability" replace />} />
      <Route path="/meeting/setup/places" element={<Navigate to="/meeting/setup/place" replace />} />
      <Route path="/meeting/setup/vote" element={<Navigate to="/meeting/setup/review" replace />} />
      <Route path="/meeting/setup/result" element={<Navigate to="/meeting/setup/review" replace />} />
    </Routes></BrowserRouter>
  );
}

export default App;

