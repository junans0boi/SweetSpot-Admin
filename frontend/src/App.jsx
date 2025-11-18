import React from 'react';
import { Routes, Route } from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import UserListPage from './pages/UserListPage';
import PlaceListPage from './pages/PlaceListPage';
import ReviewListPage from './pages/ReviewListPage';
import AdminLayout from './components/AdminLayout'; 

function App() {
  return (
    <Routes>
      {/* 1. 로그인 페이지 (레이아웃 없음) */}
     
       <Route path="/" element={<LoginPage />} /> 
      {/* 2. ✅ [수정] AdminLayout이 모든 관리자 페이지를 감싸도록 변경 */}
      <Route element={<AdminLayout />}>
        <Route path="/admin/users" element={<UserListPage />} />
        <Route path="/admin/places" element={<PlaceListPage />} />
        <Route path="/admin/places/:placeId/reviews" element={<ReviewListPage />} />
      </Route>
    </Routes>
  );
}

export default App;