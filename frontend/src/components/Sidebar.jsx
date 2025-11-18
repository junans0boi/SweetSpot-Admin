import React from 'react';
import { useLocation } from 'react-router-dom';

export default function Sidebar() {
  const location = useLocation(); // 현재 URL 경로를 가져옵니다.

  // 현재 경로가 path로 시작하는지 확인 (예: /admin/places/1/reviews도 /admin/places를 활성화)
  const isActive = (path) => {
    return location.pathname.startsWith(path);
  };

  return (
    <aside className="w-64 bg-gray-800 text-white flex flex-col flex-shrink-0">
      <div className="text-2xl font-bold p-5 text-center border-b border-gray-700">
        SweetSpot
        <span className="block text-sm font-normal text-gray-400">Admin Panel</span>
      </div>
      <nav className="flex-1 p-4">
        <ul>
          {/* 회원 관리 메뉴 */}
          <li className="mb-2">
            <a 
              href="/admin/users" 
              className={`flex items-center p-3 rounded ${
                isActive('/admin/users') 
                  ? 'bg-gray-700 text-white' // 활성화 스타일
                  : 'text-gray-400 hover:bg-gray-700 hover:text-white' // 비활성화 스타일
              }`}
            >
              <svg className="w-5 h-5 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"></path></svg>
              회원 관리
            </a>
          </li>
          
          {/* 장소 관리 메뉴 */}
          <li className="mb-2">
            <a 
              href="/admin/places" 
              className={`flex items-center p-3 rounded ${
                isActive('/admin/places') // '/admin/places' 또는 '/admin/places/...'일 때
                  ? 'bg-gray-700 text-white' // 활성화 스타일
                  : 'text-gray-400 hover:bg-gray-700 hover:text-white' // 비활성화 스타일
              }`}
            >
              <svg className="w-5 h-5 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"></path><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>
              장소 관리
            </a>
          </li>
        </ul>
      </nav>
    </aside>
  );
}