# MariaDB 스키마 (sql/)

백엔드의 JPA 엔티티와 맞춘 MariaDB 테이블 정의입니다. 로컬과 운영 실행은 MariaDB만 사용하며,
`mariadb`/`prod` 프로필은 `ddl-auto=validate`라서 이 SQL과 엔티티가 다르면 서버가 시작되지 않습니다.
H2는 실행 환경에서 사용하지 않고 자동화 테스트에서만 사용합니다.

| 파일 | 용도 |
|---|---|
| `coyote_backend_mj_schema.sql` | **새 DB**를 만들 때 실행하는 전체 스크립트(데이터베이스 `coyote_db_backend_mj` 생성 포함) |
| `migrations/001_place_details.sql` | 이미 만든 DB에 장소 구조화 컬럼을 더하는 스크립트(여러 번 실행해도 안전) |
| `migrations/002_place_decision.sql` | 이미 만든 DB에 마감 후 장소 결정(호스트 선택·자동 선정) 컬럼과 후보 테이블을 더하는 스크립트(여러 번 실행해도 안전) |

## 적용 방법
새 DB:

```bash
mariadb -u root -p < sql/coyote_backend_mj_schema.sql
```

이후 `backend/.env.example`을 `backend/.env`로 복사해 `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`을 설정하면
별도 프로필 지정 없이도 기본 `mariadb` 프로필로 실행됩니다.

이미 만든 DB(예전 스키마):

```bash
mariadb -u root -p < sql/migrations/001_place_details.sql
mariadb -u root -p < sql/migrations/002_place_decision.sql
```
(001을 이미 실행했다면 002만 실행하면 됩니다. 번호 순서대로 실행하세요.)

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

## 002 변경 내용
| 테이블 | 변경 |
|---|---|
| `final_result` | `place_status`, `host_decision_deadline` 추가 (`place_status`가 NULL인 기존 결과는 확정으로 취급) |
| `final_result_candidate` | 새 테이블: 호스트가 고를 후보(이름·장소 ID·주소·업종·좌표·득표 수·AI 이유, `sort_order`로 순서 유지) |

`place_status` 값: `PENDING`(후보 분석 중) / `AWAITING_HOST`(호스트 선택 대기) / `DECIDED`(확정) / `NONE`(추후 협의).
