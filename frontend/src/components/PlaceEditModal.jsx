import React, { useState, useEffect } from 'react';

export default function PlaceEditModal({ place, onClose, onSave }) {
  // 모달 내부에서 수정 중인 데이터를 관리할 상태
  const [formData, setFormData] = useState({
    id: '',
    name: '',
    address: '',
    mainCategory: '',
    subCategory: '',
  });

  // prop으로 받은 place 데이터가 변경될 때마다 모달 내부 상태를 업데이트
  useEffect(() => {
    if (place) {
      setFormData({
        id: place.id,
        name: place.name,
        address: place.address || '', // null 방지
        mainCategory: place.mainCategory,
        subCategory: place.subCategory,
      });
    }
  }, [place]);

  // 폼 입력 변경 핸들러
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  // 저장 버튼 클릭 핸들러
  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData); // 부모 컴포넌트(PlaceListPage)의 onSave 함수 호출
  };

  if (!place) return null;

  return (
    // 모달 배경 (어둡게)
    <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
      {/* 모달 본체 */}
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg p-6 m-4 overflow-y-auto max-h-[90vh]">
        <h2 className="text-2xl font-bold text-gray-800 mb-4">장소 정보 수정</h2>
        
        <form onSubmit={handleSubmit}>
          {/* ID (읽기 전용) */}
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">Place ID</label>
            <input
              type="text"
              value={formData.id}
              readOnly
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 bg-gray-100"
            />
          </div>

          {/* Name (수정 가능) */}
          <div className="mb-4">
            <label htmlFor="name" className="block text-gray-700 text-sm font-bold mb-2">사업장명 (Name)</label>
            <input
              type="text"
              id="name"
              name="name"
              value={formData.name}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>

          {/* Address (수정 가능) */}
          <div className="mb-4">
            <label htmlFor="address" className="block text-gray-700 text-sm font-bold mb-2">주소 (Address)</label>
            <input
              type="text"
              id="address"
              name="address"
              value={formData.address}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>
          
          {/* MainCategory (수정 가능) */}
          <div className="mb-4">
            <label htmlFor="mainCategory" className="block text-gray-700 text-sm font-bold mb-2">Main Category</label>
            <input
              type="text"
              id="mainCategory"
              name="mainCategory"
              value={formData.mainCategory}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
          </div>

          {/* SubCategory (수정 가능) */}
          <div className="mb-6">
            <label htmlFor="subCategory" className="block text-gray-700 text-sm font-bold mb-2">Sub Category</label>
            <input
              type="text"
              id="subCategory"
              name="subCategory"
              value={formData.subCategory}
              onChange={handleChange}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
            />
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