# MariaDB 스키마 (backend/db)

백엔드의 JPA 엔티티와 맞춘 MariaDB 테이블 정의입니다. `mariadb`/`prod` 프로필은 `ddl-auto=validate`라서 이 SQL과
엔티티가 다르면 서버가 시작되지 않습니다. (개발용 H2는 `ddl-auto=update`로 자동 생성하므로 이 파일을 쓰지 않습니다.)

| 파일 | 용도 |
|---|---|
| `coyote_backend_mj_schema.sql` | **새 DB**를 만들 때 실행하는 전체 스크립트(데이터베이스 `coyote_db_backend_mj` 생성 포함) |
| `migrations/001_place_details.sql` | 이미 만든 DB에 장소 구조화 컬럼을 더하는 스크립트(여러 번 실행해도 안전) |

## 적용 방법
새 DB:

```bash
mariadb -u root -p < backend/db/coyote_backend_mj_schema.sql
```

이미 만든 DB(예전 스키마):

```bash
mariadb -u root -p < backend/db/migrations/001_place_details.sql
```

## 규칙
- 엔티티(`@Entity`, `@Embeddable`)의 컬럼을 바꾸면 **두 곳을 같이** 바꿉니다.
  1. `coyote_backend_mj_schema.sql`의 CREATE TABLE
  2. 기존 DB용 변경 스크립트를 `migrations/`에 번호를 붙여 추가(`002_...sql`)
- 시간 값(`TIMESTAMP(6)`)은 서버가 항상 UTC로 저장합니다(`application-mariadb.properties`의 시간대 설정).
- 장소 정보는 모두 NULL 허용입니다. 장소 ID 없이 이름만 받은 예전 방식의 데이터가 그대로 남아 있을 수 있습니다.

## 001 변경 내용
| 테이블 | 추가 컬럼 |
|---|---|
| `meeting_place_option` | `place_id`, `address`, `latitude`, `longitude`, `category` |
| `meeting_response` | `place_id`, `place_address`, `place_category` (`latitude`, `longitude`는 기존 컬럼 사용) |
| `final_result_place` | `place_id`, `address`, `latitude`, `longitude` |
