-- 002: 마감 후 장소 결정(호스트 선택, 자동 선정)을 위한 컬럼과 후보 테이블.
-- 이미 만들어 둔 coyote_db_backend_mj 에 적용한다. 새 DB는 coyote_backend_mj_schema.sql 만 실행하면 된다.
-- 새 컬럼은 모두 NULL 허용이고, place_status 가 NULL 인 기존 결과는 "장소 확정(DECIDED)"으로 취급한다.
-- 여러 번 실행해도 안전하다(IF NOT EXISTS).

USE coyote_db_backend_mj;

-- 최종 결과: 장소가 어디까지 정해졌는지와 호스트 선택 시한
-- place_status: PENDING(후보 분석 중) / AWAITING_HOST(호스트 선택 대기) / DECIDED(확정) / NONE(추후 협의)
ALTER TABLE final_result
  ADD COLUMN IF NOT EXISTS place_status VARCHAR(20) NULL AFTER reason,
  ADD COLUMN IF NOT EXISTS host_decision_deadline TIMESTAMP(6) NULL COMMENT 'UTC; host must choose before this, otherwise the first candidate is picked' AFTER place_status;

-- 호스트가 고를 후보(공동 1위 장소 또는 합의 지역에서 고른 장소)
CREATE TABLE IF NOT EXISTS final_result_candidate (
  final_result_id BIGINT NOT NULL,
  candidate_name VARCHAR(100) NOT NULL,
  place_id VARCHAR(64) NULL,
  address VARCHAR(255) NULL,
  category VARCHAR(100) NULL,
  latitude DOUBLE NULL,
  longitude DOUBLE NULL,
  votes INT NOT NULL,
  reason VARCHAR(200) NULL,
  sort_order INT NOT NULL,
  PRIMARY KEY (final_result_id, sort_order),
  CONSTRAINT fk_final_result_candidate_result FOREIGN KEY (final_result_id) REFERENCES final_result (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SHOW COLUMNS FROM final_result;
SHOW COLUMNS FROM final_result_candidate;
