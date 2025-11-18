import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useOutletContext } from 'react-router-dom';
import apiClient from '../apiClient';
import Swal from 'sweetalert2';

export default function ReviewListPage() {
    const { placeId } = useParams(); // ✅ URL에서 placeId 파라미터 가져오기
    const navigate = useNavigate();
    const { handleLogout } = useOutletContext(); // ✅ 부모(AdminLayout)로부터 로그아웃 함수 받기
    const [reviews, setReviews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // 리뷰 목록 불러오기
    const fetchReviews = async () => {
        try {
            setLoading(true);
            const response = await apiClient.get(`/places/${placeId}/reviews`);
            setReviews(response.data);
            setError(null);
        } catch (err) {
            console.error("리뷰 목록 로딩 실패:", err);
            setError("데이터를 불러오는 데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchReviews();
    }, [placeId]); // placeId가 바뀔 때마다 다시 불러오기

    // 이 장소의 모든 리뷰 캐시 삭제
    const handleDeleteAllReviews = () => {
        Swal.fire({
            title: '이 장소의 리뷰를 삭제하시겠습니까?',
            text: "DB에서 리뷰 캐시가 삭제되며, 다음에 사용자가 조회할 때 Google API에서 새로 가져옵니다.",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d33',
            cancelButtonText: '취소',
            confirmButtonText: '캐시 삭제'
        }).then((result) => {
            if (result.isConfirmed) {
                apiClient.delete(`/places/${placeId}/reviews`)
                    .then(() => {
                        Swal.fire('삭제 완료!', '리뷰 캐시가 삭제되었습니다.', 'success');
                        fetchReviews(); // 목록 새로고침
                    })
                    .catch((err) => {
                        Swal.fire('삭제 실패', err.response?.data || '오류가 발생했습니다.', 'error');
                    });
            }
        });
    };
    return (
        <div className="flex-1 flex flex-col">
            {/* 1. 상단 헤더 */}
            <header className="bg-white shadow p-4 flex justify-between items-center">
                <h1 className="text-2xl font-bold text-gray-800">리뷰 관리 (장소 ID: {placeId})</h1>
                <div>
                    <button
                        onClick={() => navigate('/admin/places')} // 👈 목록으로 돌아가기
                        className="bg-gray-500 hover:bg-gray-600 text-white font-bold py-2 px-4 rounded transition duration-200 mr-4"
                    >
                        뒤로가기
                    </button>
                    <button
                        onClick={handleDeleteAllReviews}
                        className="bg-yellow-500 hover:bg-yellow-600 text-white font-bold py-2 px-4 rounded transition duration-200 mr-4"
                    >
                        리뷰 캐시 삭제
                    </button>
                    <button
                        onClick={handleLogout} // ✅ 부모에서 받은 함수 사용
                        className="bg-red-500 hover:bg-red-600 text-white font-bold py-2 px-4 rounded transition duration-200"
                    >
                        로그아웃
                    </button>
                </div>
            </header>

            {/* 2. 실제 콘텐츠 (리뷰 테이블) */}
            <main className="flex-1 p-6 overflow-y-auto">
                {loading && <p>로딩 중...</p>}
                {error && <p className="text-red-500">{error}</p>}

                <div className="bg-white shadow-md rounded-lg overflow-hidden">
                    <table className="min-w-full divide-y divide-gray-200">
                        <thead className="bg-gray-50">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">작성자 (author_name)</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">평점 (rating)</th>
                                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">리뷰 내용 (text)</th>
                            </tr>
                        </thead>
                        <tbody className="bg-white divide-y divide-gray-200">
                            {reviews.map((review, index) => (
                                <tr key={index}>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{review.author_name}</td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{review.rating} / 5.0</td>
                                    <td className="px-6 py-4 text-sm text-gray-900">{review.text}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                    {reviews.length === 0 && !loading && (
                        <p className="p-4 text-center text-gray-500">이 장소에는 캐시된 리뷰가 없습니다.</p>
                    )}
                </div>
            </main>
        </div>
    );
}