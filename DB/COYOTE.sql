CREATE DATABASE IF NOT EXISTS coyote_db;
USE coyote_db;

CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    home_address VARCHAR(255),
    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE meetings (
    meeting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    host_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    description TEXT,
    status ENUM('OPEN','CONFIRMED','CLOSED') DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_meetings_host
        FOREIGN KEY (host_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);

CREATE TABLE meeting_members (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    user_id BIGINT,
    guest_name VARCHAR(50),
    guest_email VARCHAR(100),
    join_status ENUM('PENDING', 'ACCEPTED', 'REJECTED') DEFAULT 'PENDING',
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

CREATE TABLE personal_schedules (
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    meeting_id BIGINT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    description VARCHAR(100),
    is_available BOOLEAN,

    CONSTRAINT fk_personal_schedules_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_personal_schedules_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE
);

CREATE TABLE meeting_time_options (
    time_option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    recommendation_score FLOAT,
    ai_reason TEXT,
    is_selected BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_time_options_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE
);

CREATE TABLE votes (
    vote_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    time_option_id BIGINT NOT NULL,
    response ENUM('YES', 'NO','MAYBE') DEFAULT 'NO',
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

CREATE TABLE places (
    place_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    place_name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    category VARCHAR(50),
    rating FLOAT,
    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),
    recommendation_score FLOAT,
    ai_reason TEXT,
    is_selected BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_places_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(meeting_id)
        ON DELETE CASCADE
);

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

CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    meeting_id BIGINT NOT NULL,
    content TEXT,
    type ENUM('INVITE', 'CONFIRMED', 'REMINDER'),
    is_read BOOLEAN DEFAULT FALSE,
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

CREATE TABLE ai_requests (
    ai_request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    user_id BIGINT,
    request_type ENUM('TIME', 'PLACE', 'AUTO_DECISION'),
    model_name VARCHAR(100),
    status ENUM('REQUESTED', 'DONE', 'FAIL') DEFAULT 'REQUESTED',
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

CREATE TABLE ai_recommendations (
    recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ai_request_id BIGINT NOT NULL,
    meeting_id BIGINT NOT NULL,
    recommendation_type ENUM('TIME', 'PLACE', 'SUMMARY'),
    result_json JSON,
    score FLOAT,
    reason TEXT,
    is_selected BOOLEAN DEFAULT FALSE,
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