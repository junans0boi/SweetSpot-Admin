import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.config.dev/
export default defineConfig({
  plugins: [react()],
  
  // ✅ [추가] 이 부분을 추가하세요
  server: {
    allowedHosts: [
      'admin.sweetspot.kro.kr'
    ]
  }
})