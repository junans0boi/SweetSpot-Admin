import React, { useState, useEffect } from 'react';
import apiClient from '../apiClient'; 
import Swal from 'sweetalert2';
import { useOutletContext } from 'react-router-dom'; // ✅ useNavigate 대신 useOutletContext 임포트
import UserEditModal from '../components/UserEditModal';
import UserCreateModal from '../components/UserCreateModal';

export default function UserListPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editingUser, setEditingUser] = useState(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
const { handleLogout } = useOutletContext(); 
  // 사용자 목록을 불러오는 함수
  const fetchUsers = async () => {
    try {
      setLoading(true);
      const response = await apiClient.get('/users');
      setUsers(response.data);
      setError(null);
    } catch (err) {
      console.error("사용자 목록 로딩 실패:", err);
      setError("데이터를 불러오는 데 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);


  // 사용자 수정 모달 열기
  const handleEdit = (user) => {
    setEditingUser({ ...user });
    setIsEditModalOpen(true);
  };

  // 사용자 수정 제출
  const handleUpdateUser = async (updatedUserData) => {
    const payload = {
      roles: updatedUserData.roles.split(',').map(role => role.trim())
    };
    try {
      setLoading(true);
      await apiClient.put(`/users/${updatedUserData.id}/roles`, payload);
      Swal.fire('수정 완료', '사용자 정보가 성공적으로 업데이트되었습니다.', 'success');
      setIsEditModalOpen(false);
      setEditingUser(null);
      fetchUsers();
    } catch (err) {
      console.error("사용자 수정 실패:", err);
      Swal.fire('수정 실패', err.response?.data?.message || '사용자 정보 업데이트에 실패했습니다.', 'error');
    } finally {
      setLoading(false);
    }
  };

  // 사용자 삭제
  const handleDelete = (userId, userName) => {
    Swal.fire({
      title: `${userName} 님을 삭제하시겠습니까?`,
      text: "삭제된 사용자는 복구할 수 없습니다!",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: '삭제',
      cancelButtonText: '취소'
    }).then(async (result) => {
      if (result.isConfirmed) {
        try {
          setLoading(true);
          await apiClient.delete(`/users/${userId}`);
          Swal.fire('삭제 완료!', `${userName} 님의 계정이 삭제되었습니다.`, 'success');
          fetchUsers();
        } catch (err) {
          console.error("사용자 삭제 실패:", err);
          Swal.fire('삭제 실패', err.response?.data?.message || '사용자 삭제에 실패했습니다.', 'error');
        } finally {
          setLoading(false);
        }
      }
    });
  };

  // 사용자 생성 제출
  const handleCreateUser = async (newUserData) => {
    try {
      setLoading(true);
      await apiClient.post('/users', newUserData);
      Swal.fire('생성 완료', '신규 사용자가 성공적으로 등록되었습니다.', 'success');
      setIsCreateModalOpen(false);
      fetchUsers();
    } catch (err) {
      console.error("사용자 생성 실패:", err);
      Swal.fire('생성 실패', err.response?.data?.message || '사용자 등록에 실패했습니다.', 'error');
    } finally {
      setLoading(false);
    }
  };

  // ✅ [수정] return문에서 최상위 div와 Sidebar 제거
  return (
    <div className="flex-1 flex flex-col">
      {/* 1. 상단 헤더 */}
      <header className="bg-white shadow p-4 flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800">회원 관리</h1>
        <div>
          <button
            onClick={() => setIsCreateModalOpen(true)}
            className="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded transition duration-200 mr-4"
          >
            신규 회원 등록
          </button>
          <button
            onClick={handleLogout} // ✅ 부모에서 받은 함수 사용
            className="bg-red-500 hover:bg-red-600 text-white font-bold py-2 px-4 rounded transition duration-200"
          >
            로그아웃
          </button>
        </div>
      </header>

      {/* 2. 실제 콘텐츠 (테이블) */}
      <main className="flex-1 p-6 overflow-y-auto">
        {loading && <p>로딩 중...</p>}
        {error && <p className="text-red-500">{error}</p>}

        <div className="bg-white shadow-md rounded-lg overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            {/* ... (thead) ... */}
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Roles</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {users.map((user) => (
                <tr key={user.id}>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{user.id}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{user.email}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{user.name}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {user.roles.map(role => (
                      <span key={role} className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${role === 'ROLE_ADMIN' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                        }`}>
                        {role.replace('ROLE_', '')}
                      </span>
                    ))}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <button
                      onClick={() => handleEdit(user)}
                      className="text-indigo-600 hover:text-indigo-900 mr-4"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => handleDelete(user.id, user.name)}
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
      </main>

      {/* 3. 수정 모달 */}
      {isEditModalOpen && (
        <UserEditModal
          user={editingUser}
          onClose={() => setIsEditModalOpen(false)}
          onSave={handleUpdateUser}
        />
      )}

      {/* 4. 생성 모달 */}
      {isCreateModalOpen && (
        <UserCreateModal
          onClose={() => setIsCreateModalOpen(false)}
          onSave={handleCreateUser}
        />
      )}
    </div>
  );
}