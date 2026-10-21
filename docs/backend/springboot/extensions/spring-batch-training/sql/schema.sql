-- MySQL 8: execute only after selecting the dedicated attendance_batch_training database.
CREATE TABLE employees (
    id BIGINT NOT NULL PRIMARY KEY,
    display_name VARCHAR(50) NOT NULL
);

CREATE TABLE attendance_records (
    id BIGINT NOT NULL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    business_date DATE NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    break_minutes INT NOT NULL,
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees(id),
    CONSTRAINT uq_attendance_employee_day UNIQUE (employee_id, business_date),
    INDEX idx_attendance_day_id (business_date, id)
);

CREATE TABLE attendance_daily_summary (
    employee_id BIGINT NOT NULL,
    business_date DATE NOT NULL,
    worked_minutes BIGINT NOT NULL,
    overtime_minutes BIGINT NOT NULL,
    PRIMARY KEY (employee_id, business_date),
    CONSTRAINT fk_summary_employee FOREIGN KEY (employee_id) REFERENCES employees(id)
);
