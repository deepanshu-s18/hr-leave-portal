-- V1__init_schema.sql — HR Leave Portal initial schema

CREATE TABLE employees (
    id                   BIGSERIAL     PRIMARY KEY,
    username             VARCHAR(50)   NOT NULL UNIQUE,
    email                VARCHAR(100)  NOT NULL UNIQUE,
    password             VARCHAR(255)  NOT NULL,
    full_name            VARCHAR(100)  NOT NULL,
    department           VARCHAR(100),
    employee_id          VARCHAR(20)   UNIQUE,
    role                 VARCHAR(20)   NOT NULL DEFAULT 'EMPLOYEE',
    manager_id           BIGINT        REFERENCES employees(id) ON DELETE SET NULL,
    annual_leave_balance INT           NOT NULL DEFAULT 21,
    sick_leave_balance   INT           NOT NULL DEFAULT 10,
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_employees_username   ON employees(username);
CREATE INDEX idx_employees_email      ON employees(email);
CREATE INDEX idx_employees_manager    ON employees(manager_id);
CREATE INDEX idx_employees_department ON employees(department);

CREATE TABLE leave_requests (
    id               BIGSERIAL    PRIMARY KEY,
    employee_id      BIGINT       NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    approved_by      BIGINT       REFERENCES employees(id) ON DELETE SET NULL,
    leave_type       VARCHAR(30)  NOT NULL,
    start_date       DATE         NOT NULL,
    end_date         DATE         NOT NULL,
    reason           VARCHAR(500),
    manager_comment  VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    approved_at      TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_leave_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_status   ON leave_requests(status);
CREATE INDEX idx_leave_dates    ON leave_requests(start_date, end_date);
CREATE INDEX idx_leave_approver ON leave_requests(approved_by);
