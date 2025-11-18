import React from 'react';
import { Navigate, Outlet, useNavigate } from 'react-router-dom';
import Sidebar from './Sidebar'; // 1. 사이드바 임포트
import Swal from 'sweetalert2';

// 이 레이아웃이 ProtectedRoute 역할을 겸합니다.
export default function AdminLayout() {
  const token = localStorage.getItem('accessToken');
  const navigate = useNavigate();

  // 2. 토큰 없으면 로그인 페이지로
  if (!token) {
    return <Navigate to="/" replace />;
  }

  // 3. [수정] 로그아웃 핸들러를 레이아웃으로 이동
  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    Swal.fire({
      icon: 'success',
      title: '로그아웃',
      text: '성공적으로 로그아웃되었습니다.',
      timer: 1500,
      showConfirmButton: false,
    }).then(() => {
      navigate('/'); // 로그인 페이지로 이동
    });
  };

  return (
    <div className="flex h-screen bg-gray-100">
      {/* 1. 사이드바 렌더링 */}
      <Sidebar />

      {/* 2. 메인 콘텐츠 영역 (헤더 + 실제 페이지) */}
      <div className="flex-1 flex flex-col">
        {/* Outlet은 App.jsx의 자식 라우트(UserList, PlaceList 등)가 
          렌더링될 위치입니다. 
          자식 컴포넌트에게 공통 함수(handleLogout)를 전달합니다.
        */}
        <Outlet context={{ handleLogout }} /> 
      </div>
    </div>
  );
}