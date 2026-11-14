-- COYOTE backend-mj MariaDB schema
-- Generated from backend-mj JPA entity mappings.
-- Creates a separate database to preserve the earlier coyote_db tables.
-- Backend uses MariaDB only when configured with the mariadb/prod profile and DB_* env vars.
-- Kept in the repository (sql/). Update it together with every JPA entity change; see sql/README.md.
-- Includes sql/migrations/001_place_details.sql (structured place columns).

CREATE DATABASE IF NOT EXISTS coyote_db_backend_mj
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE coyote_db_backend_mj;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt hash, never a plain password',
  name VARCHAR(50) NOT NULL,
  address VARCHAR(255) NULL,
  terms_agreed_at TIMESTAMP(6) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS email_verification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  code VARCHAR(6) NOT NULL,
  verified TINYINT(1) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  failed_attempts INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_email_verification_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS meeting (
  id BIGINT NOT NULL AUTO_INCREMENT,
  invite_token VARCHAR(64) NOT NULL,
  host_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  description VARCHAR(1000) NULL,
  response_due_date TIMESTAMP(6) NOT NULL,
  link_expiration_date TIMESTAMP(6) NULL,
  candidate_start_date DATE NOT NULL,
  candidate_end_date DATE NOT NULL,
  place_policy_mode VARCHAR(20) NOT NULL,
  policy_region VARCHAR(100) NULL,
  policy_radius_km INT NULL,
  policy_keyword VARCHAR(100) NULL,
  fixed_place VARCHAR(100) NULL,
  status VARCHAR(20) NOT NULL,
  manually_closed TINYINT(1) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_meeting_invite_token (invite_token),
  KEY ix_meeting_host_id (host_id),
  KEY ix_meeting_status_due_date (status, response_due_date),
  CONSTRAINT fk_meeting_host FOREIGN KEY (host_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS meeting_place_option (
  meeting_id BIGINT NOT NULL,
  option_name VARCHAR(100) NOT NULL,
  distance VARCHAR(20) NULL,
  place_id VARCHAR(64) NULL COMMENT 'place provider id (store number); NULL for legacy name-only options',
  address VARCHAR(255) NULL,
  latitude DOUBLE NULL,
  longitude DOUBLE NULL,
  category VARCHAR(100) NULL,
  sort_order INT NOT NULL,
  PRIMARY KEY (meeting_id, sort_order),
  CONSTRAINT fk_meeting_place_option_meeting FOREIGN KEY (meeting_id) REFERENCES meeting (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS participant (
  id BIGINT NOT NULL AUTO_INCREMENT,
  meeting_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  nickname VARCHAR(20) NOT NULL,
  role VARCHAR(10) NOT NULL,
  guest_id VARCHAR(64) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_participant_meeting_guest (meeting_id, guest_id),
  KEY ix_participant_user_id (user_id),
  CONSTRAINT fk_participant_meeting FOREIGN KEY (meeting_id) REFERENCES meeting (id),
  CONSTRAINT fk_participant_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS meeting_response (
  id BIGINT NOT NULL AUTO_INCREMENT,
  meeting_id BIGINT NOT NULL,
  participant_id BIGINT NOT NULL,
  place_mode VARCHAR(20) NOT NULL,
  region VARCHAR(100) NULL,
  place_preference VARCHAR(100) NULL,
  recommendation_radius INT NULL,
  recommendation_keyword VARCHAR(100) NULL,
  latitude DOUBLE NULL,
  longitude DOUBLE NULL,
  place_id VARCHAR(64) NULL COMMENT 'chosen or proposed place id; latitude/longitude above hold its coordinates',
  place_address VARCHAR(255) NULL,
  place_category VARCHAR(100) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  updated_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_meeting_response_participant (participant_id),
  KEY ix_meeting_response_meeting_id (meeting_id),
  CONSTRAINT fk_meeting_response_meeting FOREIGN KEY (meeting_id) REFERENCES meeting (id),
  CONSTRAINT fk_meeting_response_participant FOREIGN KEY (participant_id) REFERENCES participant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS response_available_date (
  response_id BIGINT NOT NULL,
  available_date DATE NOT NULL,
  PRIMARY KEY (response_id, available_date),
  CONSTRAINT fk_response_available_date_response FOREIGN KEY (response_id) REFERENCES meeting_response (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS final_result (
  id BIGINT NOT NULL AUTO_INCREMENT,
  meeting_id BIGINT NOT NULL,
  selected_date DATE NOT NULL,
  date_vote_count INT NOT NULL,
  place_vote_count INT NOT NULL,
  total_responses INT NOT NULL,
  decision_type VARCHAR(20) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  place_status VARCHAR(20) NULL,
  host_decision_deadline TIMESTAMP(6) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_final_result_meeting (meeting_id),
  CONSTRAINT fk_final_result_meeting FOREIGN KEY (meeting_id) REFERENCES meeting (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS final_result_place (
  final_result_id BIGINT NOT NULL,
  place_name VARCHAR(100) NOT NULL,
  place_id VARCHAR(64) NULL,
  address VARCHAR(255) NULL,
  latitude DOUBLE NULL,
  longitude DOUBLE NULL,
  sort_order INT NOT NULL,
  PRIMARY KEY (final_result_id, sort_order),
  CONSTRAINT fk_final_result_place_result FOREIGN KEY (final_result_id) REFERENCES final_result (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

SHOW TABLES;

