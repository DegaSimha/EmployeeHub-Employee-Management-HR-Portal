CREATE DATABASE IF NOT EXISTS employeehub
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE employeehub;

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    manager_name VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_departments_name (name)
);

CREATE TABLE IF NOT EXISTS employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_code VARCHAR(20) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(160) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(32),
    department_id BIGINT NOT NULL,
    designation VARCHAR(120) NOT NULL,
    salary DECIMAL(12,2) NOT NULL,
    joining_date DATE NOT NULL,
    employment_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    address VARCHAR(240),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_employees_employee_code (employee_code),
    UNIQUE KEY uk_employees_email (email),
    KEY idx_employees_department (department_id),
    KEY idx_employees_status (status),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments (id)
);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(160) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    employee_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_employee (employee_id),
    CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    check_in TIME(6),
    check_out TIME(6),
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attendance_employee_date (employee_id, attendance_date),
    KEY idx_attendance_date (attendance_date),
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

CREATE TABLE IF NOT EXISTS leaves (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    leave_type VARCHAR(24) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days INT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    admin_comment VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_leaves_employee (employee_id),
    KEY idx_leaves_status (status),
    CONSTRAINT fk_leaves_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

CREATE TABLE IF NOT EXISTS company_holidays (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    holiday_date DATE NOT NULL,
    description VARCHAR(500),
    paid BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_company_holidays_name (name),
    KEY idx_company_holidays_date (holiday_date)
);
