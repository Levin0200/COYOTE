# 모여ON (COYOTE)

초대 링크로 참여자의 가능한 날짜와 희망 장소를 받고, 득표·군집화·AI 후보 선정을 통해 모임 일정과 장소를 결정하는 서비스입니다.

## 프로젝트 구조

- `frontend/`: React 19 + Vite
- `backend/`: Spring Boot 4.1.1 + Java 25 + Gradle
- `deploy/`: Cloudflare·EC2 배포 설정 및 운영 당시 서버 설정 스냅샷
- `sql/`: MariaDB 전체 스키마와 기존 DB용 변경 스크립트

## 최초 실행

### 1. MariaDB 스키마 생성

```bash
mariadb -u root -p < sql/coyote_backend_mj_schema.sql
```

### 2. 백엔드 환경 설정과 실행

`backend/.env.example`을 `backend/.env`로 복사한 뒤 DB 계정, JWT 비밀키, 외부 API 키를 입력합니다. 실제 비밀값은 Git에 커밋하지 않습니다.

```bash
cd backend
./gradlew bootRun
```

Windows PowerShell에서는 `./gradlew.bat bootRun`을 사용합니다.

### 3. 프론트엔드 환경 설정과 실행

`frontend/.env.example`을 `frontend/.env.local`로 복사한 뒤 카카오 JavaScript 키 등을 입력합니다.

```bash
cd frontend
npm ci
npm run dev
```

기본 주소는 프론트엔드 `http://localhost:5173`, 백엔드 `http://localhost:8080`입니다.

## 운영 배포 구성

2026년 11월 14일 확인한 운영 구성은 Cloudflare Workers Static Assets의 프론트엔드와 단일 AWS EC2의 Docker Compose입니다. Worker가 같은 출처의 `/api/*` 요청을 EC2의 Caddy로 전달하고, Caddy는 Spring Boot 백엔드로 프록시합니다. MariaDB는 같은 EC2의 Compose 내부 네트워크에서 사용합니다.

- 프론트엔드 Worker 설정: [`frontend/wrangler.jsonc`](frontend/wrangler.jsonc), [`frontend/worker.js`](frontend/worker.js)
- 서버에서 가져온 비밀값 없는 배포 설정과 주의사항: [`deploy/ec2/README.md`](deploy/ec2/README.md)
- 새 DB 생성과 기존 DB 변경 스크립트: [`sql/README.md`](sql/README.md)
- 운영 환경파일과 API 키, DB 데이터, 릴리스 압축파일은 저장소에 포함하지 않습니다.

현재 `frontend/` 소스는 운영 Cloudflare 빌드와 일치합니다. `develop`의 `package-lock.json`은 기존 보안 의존성 업데이트를 유지하므로 운영 배포물과 바이트 단위로 동일한 번들을 만들지는 않습니다. 이 브랜치를 새로 배포할 때는 빌드 결과를 별도로 검증해야 합니다.

## 검증

```bash
cd backend
./gradlew test

cd ../frontend
npm run lint
npm run build
```
