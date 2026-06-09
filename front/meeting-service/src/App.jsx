import { BrowserRouter, Routes, Route } from "react-router-dom";

// front_prj
import LandingPage from "./pages/front_prj/LandingPage";
import LoginPage from "./pages/front_prj/LoginPage";
import SignupPage from "./pages/front_prj/signupPage";
import OnboardingPage from "./pages/front_prj/onboardingPage";

// UserPreferences
import UserPreferencesPage from "./pages/UserPreferences/App";

// Main_Service
import MainPage from "./pages/Main_Service/MainPage";

// Meeting_Service
import CreateMeetingPage from "./pages/Meeting_Service/CreateMeetingPage";
import MeetingDetailPage from "./pages/Meeting_Service/MeetingDetailPage";
import MeetingPlacesPage from "./pages/Meeting_Service/MeetingPlacesPage";
import MeetingTimeOptionsPage from "./pages/Meeting_Service/MeetingTimeOptionsPage";
import MeetingVotePage from "./pages/Meeting_Service/MeetingVotePage";
import MeetingResultPage from "./pages/Meeting_Service/MeetingResultPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* front_prj */}
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/onboarding" element={<OnboardingPage />} />

        {/* UserPreferences */}
        <Route path="/preferences" element={<UserPreferencesPage />} />

        {/* Main_Service */}
        <Route path="/main" element={<MainPage />} />

        {/* Meeting_Service */}
        <Route path="/create-meeting" element={<CreateMeetingPage />} />
        <Route path="/meeting/:meetingId" element={<MeetingDetailPage />} />
        <Route path="/meeting/setup/time-options" element={<MeetingTimeOptionsPage />} />
        <Route path="/meeting/setup/places" element={<MeetingPlacesPage />} />
        <Route path="/meeting/setup/vote" element={<MeetingVotePage />} />
        <Route path="/meeting/setup/result" element={<MeetingResultPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;