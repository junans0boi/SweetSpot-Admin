import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios'; // 로그인 페이지는 apiClient 대신 axios를 직접 사용
import { jwtDecode } from 'jwt-decode';
import Swal from 'sweetalert2';

// ❗️ 메인 백엔드(로그인용) URL
const MAIN_API_URL = 'https://sweetspot.kro.kr/api/auth';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      // 1. 🚀 메인 백엔드로 로그인 요청
      const response = await axios.post(`${MAIN_API_URL}/signin`, {
        email: email,
        password: password,
      });

      const { accessToken } = response.data;

      // 2. 🧐 토큰 디코딩
      const decodedToken = jwtDecode(accessToken);
      
      // 3. 👮‍♂️ 관리자(ROLE_ADMIN) 권한 확인
      if (!decodedToken.roles || !decodedToken.roles.includes('ROLE_ADMIN')) {
        Swal.fire('접근 거부', '관리자 계정이 아닙니다.', 'error');
        setLoading(false);
        return; // 함수 종료
      }

      // 4. ✅ 관리자 확인! 토큰을 로컬 스토리지에 저장
      localStorage.setItem('accessToken', accessToken);

      // 5. 🏠 관리자 대시보드로 이동
      Swal.fire({
        icon: 'success',
        title: '로그인 성공!',
        text: '관리자 페이지로 이동합니다.',
        timer: 1500,
        showConfirmButton: false,
      }).then(() => {
        navigate('/admin/users');
      });

    } catch (err) {
      console.error('로그인 실패:', err);
      Swal.fire({
        icon: 'error',
        title: '로그인 실패',
        text: err.response?.data?.message || '이메일 또는 비밀번호를 확인해주세요.',
      });
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-900 text-white">
      <div className="bg-gray-800 p-10 rounded-lg shadow-xl w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold">SweetSpot</h1>
          <p className="text-gray-400 mt-2">Admin Panel</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label htmlFor="email" className="block text-gray-300 text-sm font-bold mb-2">
              관리자 이메일
            </label>
            <input
              type="email"
              id="email"
              className="shadow appearance-none border border-gray-700 rounded w-full py-3 px-4 bg-gray-700 text-white leading-tight focus:outline-none focus:shadow-outline"
              placeholder="admin@sweetspot.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="mb-6">
            <label htmlFor="password" className="block text-gray-300 text-sm font-bold mb-2">
              비밀번호
            </label>
            <input
              type="password"
              id="password"
              className="shadow appearance-none border border-gray-700 rounded w-full py-3 px-4 bg-gray-700 text-white mb-3 leading-tight focus:outline-none focus:shadow-outline"
              placeholder="••••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <div className="flex items-center justify-between">
            <button
              type="submit"
              disabled={loading}
              className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-3 px-4 rounded focus:outline-none focus:shadow-outline w-full transition duration-200 disabled:bg-indigo-400"
            >
              {loading ? '로그인 중...' : '로그인'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}