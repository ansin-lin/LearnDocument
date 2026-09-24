-- MySQL 8.4 LTS
CREATE DATABASE IF NOT EXISTS paid_leave_training
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE paid_leave_training;

CREATE TABLE departments (
  code VARCHAR(32) PRIMARY KEY,
  display_name VARCHAR(50) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL,
  UNIQUE KEY uq_departments_sort_order (sort_order)
) ENGINE = InnoDB;

CREATE TABLE users (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  employee_number VARCHAR(20) NULL,
  account VARCHAR(20) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(40) NOT NULL,
  department_code VARCHAR(32) NOT NULL,
  paid_leave_days DECIMAL(4,1) NOT NULL DEFAULT 12.0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uq_users_employee_number (employee_number),
  UNIQUE KEY uq_users_account (account),
  CONSTRAINT fk_users_department
    FOREIGN KEY (department_code) REFERENCES departments (code),
  CONSTRAINT ck_users_paid_leave_days
    CHECK (paid_leave_days >= 0)
) ENGINE = InnoDB;

CREATE TABLE leave_receipt_counters (
  receipt_date DATE PRIMARY KEY,
  last_sequence INT UNSIGNED NOT NULL DEFAULT 0
) ENGINE = InnoDB;

CREATE TABLE user_sessions (
  session_id VARCHAR(128) PRIMARY KEY,
  expires_at BIGINT UNSIGNED NOT NULL,
  session_data TEXT NOT NULL,
  KEY ix_user_sessions_expires_at (expires_at)
) ENGINE = InnoDB;

CREATE TABLE leave_applications (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  receipt_number VARCHAR(30) NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  leave_type VARCHAR(20) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  leave_days DECIMAL(5,1) NOT NULL,
  reason VARCHAR(200) NOT NULL,
  handover_status VARCHAR(20) NOT NULL,
  note VARCHAR(300) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  submitted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  cancelled_at DATETIME(3) NULL,
  UNIQUE KEY uq_leave_applications_receipt (receipt_number),
  KEY ix_leave_applications_user_status (user_id, status),
  KEY ix_leave_applications_user_period (user_id, start_date, end_date),
  CONSTRAINT fk_leave_applications_user
    FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT ck_leave_applications_type
    CHECK (leave_type IN ('paid', 'half-am', 'half-pm', 'special')),
  CONSTRAINT ck_leave_applications_handover
    CHECK (handover_status IN ('done', 'not-required')),
  CONSTRAINT ck_leave_applications_status
    CHECK (status IN ('pending', 'approved', 'returned', 'cancelled')),
  CONSTRAINT ck_leave_applications_dates
    CHECK (end_date >= start_date),
  CONSTRAINT ck_leave_applications_days
    CHECK (leave_days > 0)
) ENGINE = InnoDB;
