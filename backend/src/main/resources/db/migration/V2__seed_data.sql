-- V2__seed_data.sql — Admin, HR manager, and sample employees

-- Admin (password: Admin@123)
INSERT INTO employees (username, email, password, full_name, department, employee_id, role, annual_leave_balance, sick_leave_balance)
VALUES ('admin', 'admin@hrportal.dev', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/lewKyDALDR.oM.I4a',
        'System Admin', 'IT', 'EMP-0001', 'ADMIN', 21, 10);

-- HR Manager (password: Hr@12345)
INSERT INTO employees (username, email, password, full_name, department, employee_id, role)
VALUES ('hrmanager', 'hr@hrportal.dev', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh86',
        'HR Manager', 'Human Resources', 'EMP-0002', 'HR');

-- Engineering Manager (password: Manager@1)
INSERT INTO employees (username, email, password, full_name, department, employee_id, role)
VALUES ('engmanager', 'manager@hrportal.dev', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/lewKyDALDR.oM.I4a',
        'Engineering Manager', 'Engineering', 'EMP-0003', 'MANAGER');

-- Sample employee — Deepanshu (password: User@1234)
INSERT INTO employees (username, email, password, full_name, department, employee_id, role, manager_id)
VALUES ('deepanshu', 'deepanshuk2555@gmail.com', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh86',
        'Deepanshu Singh', 'Engineering', 'EMP-0004', 'EMPLOYEE',
        (SELECT id FROM employees WHERE username = 'engmanager'));
