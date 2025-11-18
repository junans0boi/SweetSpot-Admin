// main.jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom'; // ✅ 1. import
import App from './App.jsx';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter> {/* ✅ 2. <App>을 <BrowserRouter>로 감싸기 */}
      <App />
    </BrowserRouter>
  </React.StrictMode>
);