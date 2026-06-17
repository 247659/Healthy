// frontend/web/src/App.tsx
import { Routes, Route, Navigate } from 'react-router-dom';
import { useState } from 'react';
import PatientList from './components/PatientList';
import Navbar from './components/Navbar';
import Login from './components/Login';
import ProfileSetup from './components/ProfileSetup'; // <-- DODANY IMPORT

// Jeśli masz zrobioną rejestrację, możesz ją tu odkomentować:
// import Register from './components/Register';

function App() {
    // Leniwa inicjalizacja - funkcja wewnątrz useState wykona się tylko raz przy starcie.
    // Dzięki temu unikamy błędu ESLint i podwójnego renderowania.
    const [token, setToken] = useState<string | null>(() => {
        return localStorage.getItem('access_token');
    });
    const [refreshToken, setRefreshToken] = useState<string | null>(() => {
        return localStorage.getItem('refresh_token');
    })

    return (
        <>
        {token && <Navbar token={token} setToken={setToken} refreshToken={refreshToken} setRefreshToken={setRefreshToken} />}
            <div style={{ padding: '20px' }}>
                <Routes>
                    {/* Ścieżka Logowania */}
                    <Route
                        path="/"
                        element={token ? <Navigate to="/patients" /> : <Login setToken={setToken}
                                                                              setRefreshToken={setRefreshToken}/>}
                    />

                    {/* NOWA TRASA: Konfiguracja profilu (chroniona) */}
                    <Route
                        path="/profile-setup"
                        element={token ? <ProfileSetup /> : <Navigate to="/" />}
                    />

                    {/* Odkomentuj poniższe, jeśli masz komponent Register */}
                    {/* <Route
                        path="/register"
                        element={token ? <Navigate to="/patients" /> : <Register />}
                    /> */}

                    {/* Chroniona ścieżka Panelu Pacjentów */}
                    <Route
                        path="/patients"
                        element={token ? <PatientList /> : <Navigate to="/" />}
                    />

                    {/* Catch-all: jeśli użytkownik wpisze zły adres, wracamy do głównego widoku */}
                    <Route path="*" element={<Navigate to="/" />} />
                </Routes>
            </div>
        </>
    );
}

export default App;