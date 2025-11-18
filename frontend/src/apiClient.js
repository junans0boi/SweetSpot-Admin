import axios from 'axios';

// 관리자 백엔드 전용 axios 인스턴스
const apiClient = axios.create({
  // Nginx가 /api/admin/ 요청을 5007 포트로 보내줍니다.
  baseURL: '/api/admin', 
});

// 1. 요청(Request) 인터셉터
apiClient.interceptors.request.use(
  (config) => {
    // 로컬 스토리지에서 토큰을 가져옵니다.
    const token = localStorage.getItem('accessToken');
    if (token) {
      // 토큰이 있으면 헤더에 추가
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 2. 응답(Response) 인터셉터
apiClient.interceptors.response.use(
  (response) => response, // 성공한 응답은 그대로 통과
  (error) => {
    // 401 (Unauthorized) 에러 발생 시
    if (error.response && error.response.status === 401) {
      // 토큰이 만료되었거나 유효하지 않음
      localStorage.removeItem('accessToken'); // 만료된 토큰 삭제
      alert('세션이 만료되었습니다. 다시 로그인해주세요.');
      window.location.href = '/'; // 로그인 페이지로 강제 이동
    }
    return Promise.reject(error);
  }
);

export default apiClient;