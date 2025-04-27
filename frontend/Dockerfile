# Dockerfile (frontend 폴더 루트에 위치)
 
 # ========== 1단계: 빌드 환경 (Builder Stage) ==========
 # Node.js 환경을 준비한다. 'alpine' 버전은 가볍다. 이 단계를 'builder'라고 이름 붙인다.
 FROM node:18-alpine AS builder
 # 'node:18-alpine' 대신 현재 프로젝트와 호환되는 안정적인 Node LTS 버전을 사용하는 것이 좋다. 예: node:lts-alpine
 
 # 작업 디렉토리를 /app 으로 설정한다.
 WORKDIR /app
 
 # package.json과 package-lock.json(또는 yarn.lock) 파일을 먼저 복사한다.
 # 이렇게 하면 의존성 변경이 없을 때 npm install 단계를 캐시로 재사용할 수 있어 빌드 속도가 빨라진다.
 COPY package*.json ./
 
 # npm install 명령어로 의존성 패키지들을 설치한다.
 RUN npm install
 
 # 프로젝트의 나머지 모든 파일들을 /app 디렉토리로 복사한다.
 COPY . .
 
 # npm run build 명령어로 Vue 프로젝트를 빌드하여 정적 파일(HTML, CSS, JS)을 생성한다.
 # 빌드 결과물은 보통 /app/dist 폴더 안에 생긴다.
 RUN npm run build
 
 # ========== 2단계: 서비스 환경 (Final Stage) ==========
 # Nginx 웹 서버 이미지를 베이스로 사용한다. 'alpine' 버전이 가볍다.
 FROM nginx:stable-alpine
 
 # 1단계(builder)에서 생성된 빌드 결과물(/app/dist 폴더 내용)을
 # Nginx의 기본 HTML 서비스 폴더(/usr/share/nginx/html)로 복사한다.
 # Nginx는 기본적으로 이 폴더의 내용을 웹으로 서비스한다.
 COPY --from=builder /app/dist /usr/share/nginx/html
 
 # 컨테이너가 80 포트를 사용한다고 알려준다 (Nginx 기본 포트).
 EXPOSE 80
 
 # 컨테이너 시작 시 Nginx 웹 서버를 실행하는 명령어다.
 # nginx -g 'daemon off;'는 Nginx를 포그라운드에서 실행시켜서
 # 컨테이너가 바로 종료되지 않도록 하는 일반적인 방법이다.
 CMD ["nginx", "-g", "daemon off;"]