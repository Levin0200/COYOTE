CREATE DATABASE IF NOT EXISTS coyote_db;
USE coyote_db;


-- =========================
-- 유저 가입 정보 테이블
-- =========================
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    home_address VARCHAR(255),

    latitude DECIMAL(10,6),
    -- 위도 (실시간 위치 기반 장소 추천 및 거리 계산용)

    longitude DECIMAL(10,6),
    -- 경도 (실시간 위치 기반 장소 추천 및 거리 계산용)

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =========================
-- 회원가입 시 사용자의 기본 선호도를 저장하는 테이블
-- =========================
CREATE TABLE user_preferences (
    preference_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,

    preference_type ENUM(
        'TIME',
        'PLACE',
        'CATEGORY',
        'BUDGET',
        'DISTANCE',
        'TRANSPORT'
    ) NOT NULL,

    preference_value VARCHAR(100) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_preferences_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_user_preference_type
        UNIQUE (user_id, preference_type)
);


-- =========================
-- 생성된 모임의 제목, 카테고리, 상태 등 모임 기본 정보 저장 테이블
-- =========================
CREATE TABLE meetings (
    meeting_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    host_id BIGINT NOT NULL,

    title VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    description TEXT,

    status ENUM('OPEN','CONFIRMED','CLOSED') DEFAULT 'OPEN',
    -- OPEN = 조율중
    -- CONFIRMED = 모임 확정
    -- CLOSED = 모임 종료

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_meetings_host
        FOREIGN KEY (host_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);


-- =========================
-- 모임 참여자 및 초대 인원 정보를 관리하는 테이블
-- =========================
CREATE TABLE meeting_members (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    meeting_id BIGINT NOT NULL,
    user_id BIGINT,

    guest_name VARCHAR(50),
    guest_email VARCHAR(100),

    join_status ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
    -- PENDING = 응답 대기
    -- ACCEPTED = 참여 수락
    -- REJECTED = 참여 거절

    start_location VARCHAR(255),

    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_meeting_members_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_meeting_members_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE SET NULL
);


-- =========================
-- 사용자의 개인 일정 및 가능 시간 정보를 저장하는 테이블
-- =========================
CREATE TABLE personal_schedules (
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT NOT NULL,
    meeting_id BIGINT,

    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,

    description VARCHAR(100),

    is_available BOOLEAN,
    -- true = 가능
    -- false = 불가능

    CONSTRAINT fk_personal_schedules_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_personal_schedules_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE
);


-- =========================
-- 시스템(AI)이 추천한 모임 시간 후보를 저장하는 테이블
-- =========================
CREATE TABLE meeting_time_options (
    time_option_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    meeting_id BIGINT NOT NULL,

    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,

    recommendation_score FLOAT,
    -- AI 추천 점수

    ai_reason TEXT,
    -- 추천 이유

    is_selected BOOLEAN DEFAULT FALSE,
    -- 최종 선택 여부

    CONSTRAINT fk_time_options_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE
);


-- =========================
-- 참여자들이 시간 후보에 대해 응답한 결과를 저장하는 테이블
-- =========================
CREATE TABLE votes (
    vote_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    meeting_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    time_option_id BIGINT NOT NULL,

    response ENUM('YES', 'NO','MAYBE') DEFAULT 'NO',
    -- YES = 참여 가능
    -- NO = 참여 불가능
    -- MAYBE = 애매함 / 조율 가능

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_votes_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_votes_member
        FOREIGN KEY (member_id)
        REFERENCES meeting_members(member_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_votes_time_option
        FOREIGN KEY (time_option_id)
        REFERENCES meeting_time_options(time_option_id)
        ON DELETE CASCADE,

    CONSTRAINT uq_member_time_vote
        UNIQUE (member_id, time_option_id)
);


-- =========================
-- 추천된 모임 장소 후보와 관련 정보를 저장하는 테이블
-- =========================
CREATE TABLE places (
    place_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    meeting_id BIGINT NOT NULL,

    place_name VARCHAR(100) NOT NULL,

    address VARCHAR(255),
    category VARCHAR(50),

    rating FLOAT,
    -- 장소 평점

    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),

    recommendation_score FLOAT,
    -- AI 추천 점수

    ai_reason TEXT,
    -- 추천 이유

    is_selected BOOLEAN DEFAULT FALSE,
    -- 최종 선택 여부

    CONSTRAINT fk_places_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE
);


-- =========================
-- 최종 확정된 모임 시간과 장소를 저장하는 테이블
-- =========================
CREATE TABLE meeting_results (
    result_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    meeting_id BIGINT NOT NULL UNIQUE,

    selected_time_option_id BIGINT,
    place_id BIGINT,

    finalized_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_results_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_results_time_option
        FOREIGN KEY (selected_time_option_id)
        REFERENCES meeting_time_options(time_option_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_results_place
        FOREIGN KEY (place_id)
        REFERENCES places(place_id)
        ON DELETE SET NULL
);


-- =========================
-- 초대, 확정, 리마인드 등 알림 정보를 저장하는 테이블
-- =========================
CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    user_id BIGINT,
    meeting_id BIGINT NOT NULL,

    content TEXT,

    type ENUM('INVITE', 'CONFIRMED', 'REMINDER'),
    -- INVITE = 초대 알림
    -- CONFIRMED = 확정 알림
    -- REMINDER = 리마인드 알림

    is_read BOOLEAN DEFAULT FALSE,
    -- true = 읽음
    -- false = 읽지 않음

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notifications_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE
);


-- =========================
-- AI 추천 요청 내역 및 처리 상태를 관리하는 테이블
-- =========================
CREATE TABLE ai_requests (
    ai_request_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    meeting_id BIGINT NOT NULL,
    user_id BIGINT,

    request_type ENUM(
        'TIME',
        'PLACE',
        'AUTO_DECISION',
        'PREFERENCE_BASED'
    ),
    -- TIME = 시간 추천 요청
    -- PLACE = 장소 추천 요청
    -- AUTO_DECISION = 자동 의사결정 요청
    -- PREFERENCE_BASED = 선호도 기반 추천 요청

    model_name VARCHAR(100),
    -- 사용된 AI 모델명

    status ENUM('REQUESTED', 'DONE', 'FAIL') DEFAULT 'REQUESTED',
    -- REQUESTED = 요청됨
    -- DONE = 처리 완료
    -- FAIL = 처리 실패

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,

    CONSTRAINT fk_ai_requests_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_requests_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE SET NULL
);


-- =========================
-- AI가 생성한 시간·장소 추천 결과를 저장하는 테이블
-- =========================
CREATE TABLE ai_recommendations (
    recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    ai_request_id BIGINT NOT NULL,
    meeting_id BIGINT NOT NULL,

    recommendation_type ENUM(
        'TIME',
        'PLACE',
        'SUMMARY',
        'PREFERENCE'
    ),
    -- TIME = 시간 추천 결과
    -- PLACE = 장소 추천 결과
    -- SUMMARY = 통합 요약 결과
    -- PREFERENCE = 선호도 기반 추천 결과

    result_json JSON,
    -- AI 원본 응답 데이터(JSON)

    score FLOAT,
    -- 추천 점수

    reason TEXT,
    -- 추천 이유

    is_selected BOOLEAN DEFAULT FALSE,
    -- 최종 선택 여부

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_recommendations_request
        FOREIGN KEY (ai_request_id)
        REFERENCES ai_requests(ai_request_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_recommendations_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE
);