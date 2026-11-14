# EC2 운영 배포 설정 (2026-11-14 확인)

이 디렉터리는 운영 EC2의 `/home/ubuntu/moyeoon`에서 확인한 비밀값 없는 설정 파일을 보관한다. 서버의 환경파일, 데이터베이스 백업, 릴리스 압축파일은 포함하지 않는다. `compose.yaml`과 `Caddyfile`은 확인 당시 서버 파일과 동일하다.

## 실행 구조

- Ubuntu 24.04의 단일 EC2에서 Docker Compose 프로젝트 `moyeoon`을 실행한다.
- `mariadb:10.11`, Java 25 Spring Boot 이미지, `caddy:2.10-alpine` 컨테이너 세 개를 사용한다. Kubernetes와 파드는 사용하지 않는다.
- Caddy가 호스트의 80/443 포트를 받고 `X-Coyote-Origin`을 검사한 뒤 `backend:8080`으로 전달한다. 백엔드의 호스트 포트는 `127.0.0.1:8080`에만 바인딩하고, MariaDB 3306은 호스트에 게시하지 않는다.
- DB 데이터는 `mariadb_data` named volume을 컨테이너의 `/var/lib/mysql`에 연결한다. `caddy_data`, `caddy_config`도 named volume이다. 현재 세 볼륨은 EC2의 단일 루트 EBS에 저장된다.
- DB와 백엔드의 healthcheck 결과에 따라 다음 서비스를 시작하고, 세 서비스에 `restart: unless-stopped`를 적용한다.

## 파일 배치와 비밀값

서버의 `/home/ubuntu/moyeoon` 루트에 `compose.yaml`, `Caddyfile`, `prepare-release.sh`, `deploy-release.sh`를 두고, `app/`에 `backend/` 소스를 배치한다. `app/Dockerfile`은 이 저장소의 `backend/Dockerfile`과 같다. 이 디렉터리의 파일을 그대로 실행하려면 서버의 디렉터리 구조와 릴리스 압축파일 이름이 일치해야 한다.

실제 비밀값은 서버의 `.env`(DB), `app.env`(Spring), `caddy.env`(Origin 토큰)에 별도로 보관하고 권한을 `600`으로 제한한다. Cloudflare Worker의 `ORIGIN_TOKEN`도 별도로 설정한다. 세 환경파일과 실제 키는 Git에 올리지 않는다.

## 배포 절차와 주의점

현재 서버에 있는 두 스크립트는 `release-32e7166.tar.gz` 릴리스를 위해 작성된 **운영 당시의 스냅샷**이다. 새로운 커밋을 배포할 때는 릴리스 파일명과 이미지 태그, 스키마 적용 방식을 먼저 검토해야 한다. 특히 `prepare-release.sh`는 기존 `app/`를 `backups/`로 이동하고, `deploy-release.sh`는 DB 스키마 SQL을 실행하므로 검토 없이 재실행하지 않는다.

확인 당시의 흐름은 릴리스 업로드 → 설정·필수 환경변수 확인 → 기존 파일 백업 및 압축 해제 → MariaDB healthy 확인 → 스키마 적용 → 백엔드 이미지 빌드·기동 → `/api/health` 확인이다. Caddy는 Compose의 세 번째 서비스로 구동한다. GitHub Actions나 GHCR 자동 배포 설정은 없다.

정기 DB 백업과 자동 롤백은 이 구성에 포함되지 않았다. 데이터 볼륨이 컨테이너 재생성 후 유지된다는 것과 EBS 장애에 대비한 백업이 있다는 것은 다르다.
