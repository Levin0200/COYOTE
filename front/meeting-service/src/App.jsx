import { BrowserRouter, Routes, Route } from "react-router-dom";

import MainPage from "./pages/MainPage";
import CreateMeetingPage from "./pages/CreateMeetingPage";
import MeetingDetailPage from "./pages/MeetingDetailPage";
import MeetingPlacesPage from "./pages/MeetingPlacesPage";
import MeetingTimeOptionsPage from "./pages/MeetingTimeOptionsPage";
import MeetingVotePage from "./pages/MeetingVotePage";
import MeetingResultPage from "./pages/MeetingResultPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainPage />} />
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