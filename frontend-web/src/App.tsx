import {Routes, Route, useLocation} from 'react-router-dom';
import { useState } from 'react';
import Navbar from './components/Navbar';
import Login from './components/Login';
import ProfileSetup from './components/ProfileSetup';
import StaffProfile from "./components/StaffProfile";
import PatientDashboard from "./components/PatientDashboard";
import PatientDetails from "./components/PatientDetails.tsx";

function App() {
    const [token, setToken] = useState(localStorage.getItem('access_token'));
    const [refreshToken, setRefreshToken] = useState(localStorage.getItem('refresh_token'));

    const location = useLocation();

    // Navbar nie pojawi się, gdy ścieżka to '/setup' LUB '/' (ekran logowania)
    const showNavbar = location.pathname !== '/setup' && location.pathname !== '/';

    return (
        <div className="app-wrapper">
            {showNavbar && (
                <Navbar
                    token={token}
                    setToken={setToken}
                    refreshToken={refreshToken}
                    setRefreshToken={setRefreshToken}
                />
            )}

            <main className="main-content">
                <Routes>
                    <Route path="/" element={<Login setToken={setToken} setRefreshToken={setRefreshToken} />} />
                    <Route path="/setup" element={<ProfileSetup />} />
                    <Route path="/patients" element={<PatientDashboard />} />
                    <Route path="/profile/:id" element={<StaffProfile />} />
                    <Route path="/patient/:id" element={<PatientDetails />} />
                </Routes>
            </main>
        </div>
    );
}


export default App;