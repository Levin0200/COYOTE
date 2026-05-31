import { BrowserRouter, Routes, Route } from "react-router-dom";

import MainPage from "./pages/MainPage";
import CreateMeetingPage from "./pages/CreateMeetingPage";

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<MainPage />} />
        <Route path="/create-meeting" element={<CreateMeetingPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;