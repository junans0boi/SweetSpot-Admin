import React, { useState, useEffect } from 'react';
import apiClient from '../apiClient';
import { useNavigate, useOutletContext } from 'react-router-dom'; // ✅ useOutletContext 추가
import Swal from 'sweetalert2';
import PlaceEditModal from '../components/PlaceEditModal'; // ✅ [추가]

export default function PlaceListPage() {
  const [places, setPlaces] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  // ✅ 부모(AdminLayout)로부터 로그아웃 함수 받기
  const { handleLogout } = useOutletContext();

  // 페이지네이션 상태
  const [pageInfo, setPageInfo] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);

  // 필터 상태
  const [categoryList, setCategoryList] = useState([]);
  const [filterCategory, setFilterCategory] = useState('');
  const [filterKeyword, setFilterKeyword] = useState('');

  // 실제 API 요청에 사용될 고정된 필터값
  const [activeFilters, setActiveFilters] = useState({
    category: '',
    keyword: ''
  });
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingPlace, setEditingPlace] = useState(null);
  const PAGE_SIZE = 20;

  // 장소 목록 (필터 + 검색어 + 페이지)
  const fetchPlaces = async (page, filters) => {
    try {
      setLoading(true);
      const response = await apiClient.get('/places', {
        params: {
          page: page,
          size: PAGE_SIZE,
          mainCategory: filters.category,
          keyword: filters.keyword,
          sort: 'id,asc'
        }
      });

      setPlaces(response.data.content);
      setPageInfo(response.data);
      setError(null);
    } catch (err) {
      console.error("장소 목록 로딩 실패:", err);
      setError("데이터를 불러오는 데 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 카테고리 목록 불러오기
  const fetchCategories = async () => {
    try {
      const response = await apiClient.get('/places/categories');
      setCategoryList(response.data);
    } catch (err) {
      console.error("카테고리 로딩 실패:", err);
    }
  };

  // 컴포넌트 마운트 시 API 2개 호출
  useEffect(() => {
    fetchCategories();
    fetchPlaces(currentPage, activeFilters);
  }, [currentPage, activeFilters]);


  // ❌ handleLogout 함수 전체 삭제 (부모가 제공)

  // 장소 전체 삭제
  const handleDeleteAll = () => {
    Swal.fire({
      title: '정말로 모든 장소 데이터를 삭제하시겠습니까?',
      text: "DB에서 모든 장소 데이터가 영구적으로 삭제되며, 복구할 수 없습니다!",
      icon: 'error',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: '전체 삭제',
      cancelButtonText: '취소'
    }).then((result) => {
      if (result.isConfirmed) {
        setLoading(true);
        apiClient.delete('/places/all')
          .then((response) => {
            Swal.fire('삭제 완료!', response.data, 'success');
            clearSearch(); // ✅ [수정] 초기화하면서 1페이지로 이동 및 새로고침
          })
          .catch((err) => {
            Swal.fire('삭제 실패', err.response?.data || '오류가 발생했습니다.', 'error');
          })
          .finally(() => {
            setLoading(false);
          });
      }
    });
  };

  // 서버에서 CSV 일괄 등록 실행
  const handleLoadFromDisk = async () => {
    Swal.fire({
      title: '서버 CSV 일괄 등록',
      text: "서버의 CSV 파일을 DB로 적재합니다. 몇 분 이상 소요될 수 있습니다. 실행하시겠습니까?",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: '실행',
      cancelButtonText: '취소'
    }).then(async (result) => {
      if (result.isConfirmed) {
        setLoading(true);
        try {
          const response = await apiClient.post('/places/import-json');
          const jobId = response.data;

          Swal.fire({
            title: '데이터 적재 중...',
            html: '서버에서 작업을 시작합니다...',
            allowOutsideClick: false,
            didOpen: () => {
              Swal.showLoading();
            }
          });

          // 2초마다 상태를 체크하는 Polling 시작
          const intervalId = setInterval(async () => {
            try {
              const statusResponse = await apiClient.get(`/places/status/${jobId}`);
              const statusData = statusResponse.data;
              const status = statusData.status;

              const swalContent = Swal.getHtmlContainer();
              if (swalContent) {
                if (status.startsWith("파일 처리 중")) {
                  const parts = status.split('(');
                  const progressText = parts[parts.length - 1].replace(')', '');
                  // 파일 이름만 깔끔하게 추출
                  const filePart = parts.slice(0, -1).join('(').split(': ')[1] || '';
                  swalContent.innerHTML = `현재 작업중인 파일 : ${filePart} <br/> <strong>${progressText}</strong>`;
                } else {
                  swalContent.textContent = status;
                }
              }

              if (status.startsWith("COMPLETED") || status.startsWith("FAILED")) {
                clearInterval(intervalId);
                setLoading(false);
                clearSearch(); // ✅ [수정] 초기화하면서 1페이지로 이동 및 새로고침

                if (status.startsWith("COMPLETED")) {
                  Swal.fire('성공!', status.replace("COMPLETED: ", ""), 'success');
                } else {
                  Swal.fire('실패', status.replace("FAILED: ", ""), 'error');
                }
              }
            } catch (pollErr) {
              console.error("상태 조회 실패:", pollErr);
              clearInterval(intervalId);
              setLoading(false);
              Swal.fire('오류', '작업 상태를 조회하는 데 실패했습니다.', 'error');
            }
          }, 2000);

        } catch (startErr) {
          console.error("CSV 일괄 적재 시작 실패:", startErr);
          Swal.fire('실패', startErr.response?.data || '작업 시작에 실패했습니다.', 'error');
          setLoading(false);
        }
      }
    });
  };

  // 검색 버튼 클릭
  const handleSearch = () => {
    setCurrentPage(0);
    setActiveFilters({
      category: filterCategory,
      keyword: filterKeyword
    });
  };

  // 검색어 초기화
  const clearSearch = () => {
    setCurrentPage(0);
    setFilterCategory('');
    setFilterKeyword('');
    setActiveFilters({ category: '', keyword: '' });
  };

  // 페이지 이동
  const goToPage = (page) => {
    if (pageInfo && page >= 0 && page < pageInfo.totalPages) {
      setCurrentPage(page);
    }
  };
  // ✅ [신규] 수정 모달 열기
  const handleEdit = (place) => {
    setEditingPlace(place);
    setIsEditModalOpen(true);
  };

  // ✅ [신규] 수정 API 호출
  const handleUpdatePlace = async (updatedPlaceData) => {
    try {
      setLoading(true);
      await apiClient.put(`/places/${updatedPlaceData.id}`, updatedPlaceData);
      Swal.fire('수정 완료', '장소 정보가 성공적으로 업데이트되었습니다.', 'success');
      setIsEditModalOpen(false);
      setEditingPlace(null);
      // 현재 페이지와 필터를 유지하며 목록 새로고침
      fetchPlaces(currentPage, activeFilters);
    } catch (err) {
      console.error("장소 수정 실패:", err);
      Swal.fire('수정 실패', err.response?.data?.message || '장소 정보 업데이트에 실패했습니다.', 'error');
    } finally {
      setLoading(false);
    }
  };

  // ✅ [신규] 개별 장소 삭제 API 호출
  const handleDeletePlace = (placeId, placeName) => {
    Swal.fire({
      title: `[${placeName}] 장소를 삭제하시겠습니까?`,
      text: "삭제된 데이터는 복구할 수 없습니다!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonText: '취소',
      confirmButtonText: '삭제'
    }).then((result) => {
      if (result.isConfirmed) {
        setLoading(true);
        apiClient.delete(`/places/${placeId}`)
          .then(() => {
            Swal.fire('삭제 완료!', `[${placeName}] 장소가 삭제되었습니다.`, 'success');
            // 현재 페이지와 필터를 유지하며 목록 새로고침
            fetchPlaces(currentPage, activeFilters);
          })
          .catch((err) => {
            Swal.fire('삭제 실패', err.response?.data || '오류가 발생했습니다.', 'error');
          })
          .finally(() => {
            setLoading(false);
          });
      }
    });
  };
  // ✅ [수정] return문에서 최상위 div와 Sidebar 제거
  return (
    <div className="flex-1 flex flex-col">
      {/* 1. 상단 헤더 */}
      <header className="bg-white shadow p-4 flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800">장소 관리</h1>
        <div>
          <button
            onClick={handleLoadFromDisk}
            disabled={loading}
            className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded transition duration-200 mr-4 disabled:bg-blue-300"
          >
            {loading ? '처리 중...' : '서버 CSV 일괄 등록'}
          </button>

          <button
            onClick={handleDeleteAll}
            disabled={loading}
            className="bg-yellow-500 hover:bg-yellow-600 text-white font-bold py-2 px-4 rounded transition duration-200 mr-4 disabled:bg-yellow-300"
          >
            {loading ? '처리 중...' : '전체 삭제 (Test)'}
          </button>

          <button
            onClick={handleLogout} // ✅ 부모에서 받은 함수 사용
            className="bg-red-500 hover:bg-red-600 text-white font-bold py-2 px-4 rounded transition duration-200"
          >
            로그아웃
          </button>
        </div>
      </header>

      {/* 2. 실제 콘텐츠 */}
      <main className="flex-1 p-6 overflow-y-auto">

        <div className="bg-white shadow-md rounded-lg p-4 mb-6 grid grid-cols-1 md:grid-cols-4 gap-4">

          <div className="md:col-span-1">
            <label htmlFor="category" className="block text-sm font-medium text-gray-700">카테고리</label>
            <select
              id="category"
              value={filterCategory}
              onChange={(e) => setFilterCategory(e.target.value)}
              className="mt-1 block w-full py-2 px-3 border border-gray-300 bg-white rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            >
              <option value="">-- 전체 카테고리 --</option>
              {categoryList.map(cat => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </div>

          <div className="md:col-span-2">
            <label htmlFor="keyword" className="block text-sm font-medium text-gray-700">매장명 또는 주소</label>
            <input
              type="text"
              id="keyword"
              placeholder="매장명 또는 주소 일부..."
              value={filterKeyword}
              onChange={(e) => setFilterKeyword(e.target.value)}
              className="mt-1 block w-full py-2 px-3 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
            />
          </div>

          <div className="md:col-span-1 flex items-end space-x-2">
            <button
              onClick={handleSearch}
              className="bg-gray-700 hover:bg-gray-800 text-white font-bold py-2 px-4 rounded transition duration-200 w-full"
            >
              검색
            </button>
            <button
              onClick={clearSearch}
              className="bg-gray-300 hover:bg-gray-400 text-black font-bold py-2 px-4 rounded transition duration-200 w-full"
            >
              초기화
            </button>
          </div>
        </div>

        {loading && <p>로딩 중...</p>}
        {error && <p className="text-red-500">{error}</p>}

        <div className="bg-white shadow-md rounded-lg overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Address</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Category</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {places.map((place) => (
                <tr key={place.id}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{place.id}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{place.name}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{place.address}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{place.mainCategory} &gt; {place.subCategory}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button
                      onClick={() => navigate(`/admin/places/${place.id}/reviews`)}
                      className="text-blue-600 hover:text-blue-900 mr-4"
                    >
                      리뷰 보기
                    </button>
                    <button
                      onClick={() => handleEdit(place)}
                      className="text-indigo-600 hover:text-indigo-900 mr-4"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDeletePlace(place.id, place.name)}
                      className="text-red-600 hover:text-red-900"
                    >
                      삭제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {pageInfo && (
          <div className="flex justify-between items-center mt-4">
            <span className="text-sm text-gray-700">
              총 {pageInfo.totalElements}개 항목 중 {pageInfo.numberOfElements}개 표시 (페이지 {pageInfo.number + 1} / {pageInfo.totalPages})
            </span>
            <div className="space-x-2">
              <button
                onClick={() => goToPage(pageInfo.number - 1)}
                disabled={pageInfo.first}
                className="bg-gray-300 hover:bg-gray-400 text-gray-800 font-bold py-2 px-4 rounded disabled:opacity-50"
              >
                이전
              </button>
              <button
                onClick={() => goToPage(pageInfo.number + 1)}
                disabled={pageInfo.last}
                className="bg-gray-300 hover:bg-gray-400 text-gray-800 font-bold py-2 px-4 rounded disabled:opacity-50"
              >
                다음
              </button>
            </div>
          </div>
        )}

      </main>
      {/* ✅ [신규] 장소 수정 모달 렌더링 */}
      {isEditModalOpen && (
        <PlaceEditModal
          place={editingPlace}
          onClose={() => setIsEditModalOpen(false)}
          onSave={handleUpdatePlace}
        />
      )}
    </div>
  );
}