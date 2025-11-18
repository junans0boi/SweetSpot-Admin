# 1. 기본 이미지로 Node.js 20-alpine 버전을 사용
FROM node:20-alpine

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. package.json과 package-lock.json (또는 yarn.lock)을 먼저 복사
COPY package*.json ./

# 4. 의존성 설치
RUN NODE_ENV=development npm install

# 5. 나머지 모든 소스 코드를 복사
COPY . .

# 6. 프론트엔드 포트 5008번을 노출
EXPOSE 5008

# 7. 컨테이너가 시작될 때 "npm run dev" 스크립트 실행
# (package.json의 "dev" 스크립트가 "vite --port 5008 --host"로 설정되어 있어야 함)
CMD ["npm", "run", "dev"]