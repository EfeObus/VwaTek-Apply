-- V1__baseline.sql
-- Baseline migration: Creates all existing tables
-- This migration represents the initial schema state.
-- For existing databases, run: flyway baseline
-- For new databases, this creates everything from scratch.

-- ============================================
-- Core Tables
-- ============================================

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    profile_picture TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resumes (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    industry VARCHAR(100),
    source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_resumes_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS resume_versions (
    id VARCHAR(36) PRIMARY KEY,
    resume_id VARCHAR(36) NOT NULL,
    version_number INT NOT NULL,
    content TEXT NOT NULL,
    change_description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resume_versions_resume FOREIGN KEY (resume_id) REFERENCES resumes(id),
    INDEX idx_resume_versions_resume_id (resume_id)
);

CREATE TABLE IF NOT EXISTS resume_analyses (
    id VARCHAR(36) PRIMARY KEY,
    resume_id VARCHAR(36) NOT NULL,
    job_description TEXT NOT NULL,
    match_score INT NOT NULL,
    missing_keywords TEXT NOT NULL,
    recommendations TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resume_analyses_resume FOREIGN KEY (resume_id) REFERENCES resumes(id),
    INDEX idx_resume_analyses_resume_id (resume_id)
);

CREATE TABLE IF NOT EXISTS cover_letters (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    resume_id VARCHAR(36),
    job_title VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    tone VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cover_letters_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_cover_letters_resume FOREIGN KEY (resume_id) REFERENCES resumes(id),
    INDEX idx_cover_letters_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS interview_sessions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    resume_id VARCHAR(36),
    job_title VARCHAR(255) NOT NULL,
    job_description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_interview_sessions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_interview_sessions_resume FOREIGN KEY (resume_id) REFERENCES resumes(id),
    INDEX idx_interview_sessions_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS interview_questions (
    id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    question TEXT NOT NULL,
    expected_answer TEXT,
    user_answer TEXT,
    feedback TEXT,
    score INT,
    question_type VARCHAR(50) NOT NULL DEFAULT 'BEHAVIORAL',
    order_number INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_interview_questions_session FOREIGN KEY (session_id) REFERENCES interview_sessions(id)
);

CREATE TABLE IF NOT EXISTS settings (
    `key` VARCHAR(255) NOT NULL,
    `value` TEXT NOT NULL,
    user_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`key`, user_id)
);

-- ============================================
-- Job Tracker Tables
-- ============================================

CREATE TABLE IF NOT EXISTS job_applications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    resume_id VARCHAR(36),
    cover_letter_id VARCHAR(36),
    job_title VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    company_logo TEXT,
    job_url TEXT,
    job_description TEXT,
    job_board_source VARCHAR(50),
    external_job_id VARCHAR(100),
    city VARCHAR(100),
    province VARCHAR(50),
    country VARCHAR(50) NOT NULL DEFAULT 'Canada',
    is_remote BOOLEAN NOT NULL DEFAULT FALSE,
    is_hybrid BOOLEAN NOT NULL DEFAULT FALSE,
    salary_min INT,
    salary_max INT,
    salary_currency VARCHAR(3) NOT NULL DEFAULT 'CAD',
    salary_period VARCHAR(20),
    status VARCHAR(30) NOT NULL DEFAULT 'SAVED',
    applied_at TIMESTAMP,
    response_received_at TIMESTAMP,
    noc_code VARCHAR(10),
    requires_work_permit BOOLEAN,
    is_lmia_required BOOLEAN,
    contact_name VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    sync_status VARCHAR(20) NOT NULL DEFAULT 'SYNCED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_applications_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_job_applications_resume FOREIGN KEY (resume_id) REFERENCES resumes(id),
    CONSTRAINT fk_job_applications_cover_letter FOREIGN KEY (cover_letter_id) REFERENCES cover_letters(id),
    INDEX idx_job_applications_user_id (user_id),
    INDEX idx_job_applications_status (status)
);

-- Baseline migration complete. Future changes go in V2, V3, etc.
