import { Navigate } from "react-router-dom";
import { isLoggedIn } from "../utils/authToken";

function RequireAuth({ children }) {
  if (!isLoggedIn()) return <Navigate to="/login" replace />;
  return children;
}

export default RequireAuth;
