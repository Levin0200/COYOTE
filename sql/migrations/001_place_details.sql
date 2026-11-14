-- 001: 장소를 이름만이 아니라 ID, 주소, 좌표, 업종까지 구조화해서 저장하기 위한 컬럼 추가.
-- 이미 만들어 둔 coyote_db_backend_mj 에 적용한다. 새 DB는 coyote_backend_mj_schema.sql 만 실행하면 된다.
-- 모든 컬럼이 NULL 허용이라 기존 데이터는 그대로 두고, 여러 번 실행해도 안전하다(IF NOT EXISTS).

USE coyote_db_backend_mj;

-- 추천 후보: 검색에서 고른 장소의 ID와 공식 정보
ALTER TABLE meeting_place_option
  ADD COLUMN IF NOT EXISTS place_id VARCHAR(64) NULL COMMENT 'place provider id (store number); NULL for legacy name-only options' AFTER distance,
  ADD COLUMN IF NOT EXISTS address VARCHAR(255) NULL AFTER place_id,
  ADD COLUMN IF NOT EXISTS latitude DOUBLE NULL AFTER address,
  ADD COLUMN IF NOT EXISTS longitude DOUBLE NULL AFTER latitude,
  ADD COLUMN IF NOT EXISTS category VARCHAR(100) NULL AFTER longitude;

-- 게스트·호스트 응답: 선택하거나 제안한 장소(좌표는 기존 latitude/longitude 컬럼을 쓴다)
ALTER TABLE meeting_response
  ADD COLUMN IF NOT EXISTS place_id VARCHAR(64) NULL COMMENT 'chosen or proposed place id; latitude/longitude above hold its coordinates' AFTER longitude,
  ADD COLUMN IF NOT EXISTS place_address VARCHAR(255) NULL AFTER place_id,
  ADD COLUMN IF NOT EXISTS place_category VARCHAR(100) NULL AFTER place_address;

-- 최종 결과: 정해진 장소의 ID와 좌표
ALTER TABLE final_result_place
  ADD COLUMN IF NOT EXISTS place_id VARCHAR(64) NULL AFTER place_name,
  ADD COLUMN IF NOT EXISTS address VARCHAR(255) NULL AFTER place_id,
  ADD COLUMN IF NOT EXISTS latitude DOUBLE NULL AFTER address,
  ADD COLUMN IF NOT EXISTS longitude DOUBLE NULL AFTER latitude;

SHOW COLUMNS FROM meeting_place_option;
SHOW COLUMNS FROM meeting_response;
SHOW COLUMNS FROM final_result_place;
