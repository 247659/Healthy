// frontend/web/src/main.tsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.tsx';
import { BrowserRouter } from 'react-router-dom'; // <--- DODAJ IMPORT

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <BrowserRouter>  {/* <--- OWIŃ APP W BROWSER ROUTER */}
            <App />
        </BrowserRouter>
    </React.StrictMode>,
);