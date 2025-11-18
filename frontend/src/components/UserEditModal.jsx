import React, { useState, useEffect } from 'react';

export default function UserEditModal({ user, onClose, onSave }) {
  // 모달 내부에서 수정 중인 데이터를 관리할 상태
  const [formData, setFormData] = useState({
    id: '',
    email: '',
    name: '',
    roles: '', // roles를 콤마로 구분된 문자열로 관리
  });

  // prop으로 받은 user 데이터가 변경될 때마다 모달 내부 상태를 업데이트
  useEffect(() => {
    if (user) {
      setFormData({
        id: user.id,
        email: user.email,
        name: user.name,
        roles: user.roles.join(', '), // 배열을 콤마로 구분된 문자열로 변환
      });
    }
  }, [user]);

  // 폼 입력 변경 핸들러
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  // 저장 버튼 클릭 핸들러
  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData); // 부모 컴포넌트(UserListPage)의 onSave 함수 호출
  };

  if (!user) return null;

  return (
    // 모달 배경 (어둡게)
    <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
      {/* 모달 본체 */}
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg p-6 m-4">
        <h2 className="text-2xl font-bold text-gray-800 mb-4">회원 정보 수정</h2>
        
        <form onSubmit={handleSubmit}>
          {/* ID (읽기 전용) */}
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">User ID</label>
            <input
              type="text"
              value={formData.id}
              readOnly
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 bg-gray-100"
            />
          </div>

          {/* Email (읽기 전용) */}
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">Email</label>
            <input
              type="email"
              value={formData.email}
              readOnly
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 bg-gray-100"
            />
          </div>

          {/* Name (읽기 전용) */}
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">Name</label>
            <input
              type="text"
              value={formData.name}
              readOnly
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 bg-gray-100"
            />
          </div>

          {/* Roles (수정 가능) */}
          <div className="mb-6">
            <label htmlFor="roles" className="block text-gray-700 text-sm font-bold mb-2">
              Roles (콤마로 구분)
            </label>
            <input
              type="text"
              id="roles"
              name="roles"
              value={formData.roles}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
              placeholder="ROLE_USER, ROLE_ADMIN"
            />
            <p className="text-xs text-gray-500 mt-1">예: ROLE_USER, ROLE_ADMIN</p>
          </div>

          {/* 버튼 영역 */}
          <div className="flex justify-end space-x-4">
            <button
              type="button"
              onClick={onClose}
              className="bg-gray-500 hover:bg-gray-600 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline transition duration-200"
            >
              취소
            </button>
            <button
              type="submit"
              className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline transition duration-200"
            >
              저장
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}