# 🏢 HR Leave Management Portal

> Full-stack internal HR tool — Spring Boot 3 REST backend + React 18/TypeScript frontend. Automates employee leave workflows with a complete state machine, role-based access, and real-time balance tracking.

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)](https://docs.docker.com/compose/)

---

## 📸 Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                   Frontend (React 18 + TS)               │
│   Dashboard │ Apply Leave │ My Leaves │ Approvals │ Team │
│        Axios + JWT Interceptors (auto token refresh)      │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP/REST (CORS: port 5173→8082)
┌────────────────────────▼────────────────────────────────┐
│              Backend (Spring Boot 3)                     │
│  AuthController │ LeaveController │ EmployeeController   │
│  ────────────────────────────────────────────────────── │
│  LeaveService (State Machine + Guard Clauses)            │
│  JWT Filter → @PreAuthorize RBAC                         │
│  ────────────────────────────────────────────────────── │
│  Spring Data JPA + Flyway → PostgreSQL 16                │
└─────────────────────────────────────────────────────────┘
```

---

## ✨ Features

### Leave State Machine
```
PENDING ──[Manager Approve]──► APPROVED
        ──[Manager Reject]───► REJECTED
        ──[Employee Cancel]──► CANCELLED

APPROVED ──[Cancel before start]──► CANCELLED (balance restored)
```

### Guard Clauses on `apply()`
| Guard | Rule |
|---|---|
| Past date | Start date cannot be in the past |
| Date order | End date must be ≥ start date |
| Overlap check | No existing PENDING/APPROVED leave on same dates |
| Balance check | Insufficient annual/sick leave balance → rejected |

### Role-Based Access Control
| Role | Capabilities |
|---|---|
| `EMPLOYEE` | Apply, view own leaves, cancel own leaves |
| `MANAGER` | All employee rights + approve/reject direct reports' leaves |
| `HR` | View all leaves, approve/reject any leave |
| `ADMIN` | Full access including user management |

### Leave Balance Tracking
- Annual leave deducted **on approval**, restored **on cancellation**
- Sick, Casual, Maternity/Paternity — separate balance pools
- Working-days calculation (Mon–Fri, weekends excluded)

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.2.5, Spring Security |
| Auth | JWT (JJWT 0.12.5) — access (24h) + refresh (7d) tokens |
| Database | PostgreSQL 16, Spring Data JPA, Flyway migrations |
| Frontend | React 18, TypeScript 5, Vite 5 |
| HTTP Client | Axios with JWT interceptors (auto token refresh) |
| API Docs | SpringDoc OpenAPI 3.0 (Swagger UI at `/swagger-ui.html`) |
| Testing | JUnit 5, Mockito — 20 unit tests across 4 `@Nested` groups |
| Infrastructure | Docker, docker-compose |

---

## 📁 Project Structure

```
hr-leave-portal/
├── backend/                          # Spring Boot 3 REST API
│   ├── src/main/java/com/deepanshu/hrportal/
│   │   ├── controller/               # AuthController, LeaveController, EmployeeController
│   │   ├── service/                  # LeaveService (full business logic)
│   │   ├── repository/               # EmployeeRepository, LeaveRepository
│   │   ├── model/                    # Employee, LeaveRequest (JPA entities)
│   │   ├── dto/                      # Request/Response DTOs
│   │   ├── security/                 # JwtTokenProvider, JwtAuthFilter
│   │   ├── config/                   # SecurityConfig (CORS, RBAC)
│   │   └── exception/                # GlobalExceptionHandler + custom exceptions
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/
│   │       ├── V1__init_schema.sql   # employees + leave_requests tables
│   │       └── V2__seed_data.sql     # Admin, HR, Manager, Employee seeds
│   └── src/test/                     # LeaveServiceTest (20 unit tests)
│
├── frontend/                         # React 18 + TypeScript
│   └── src/
│       ├── pages/                    # Dashboard, LeaveRequest, MyLeaves, Approval, Employees
│       ├── components/               # Layout, ErrorBoundary
│       ├── hooks/                    # useAuth (JWT context)
│       └── services/                 # api.ts (Axios client + interceptors)
│
└── docker-compose.yml                # PostgreSQL + Backend + Frontend
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+, Maven 3.9+
- Node 18+, npm
- Docker + Docker Compose

### Option A — Docker (Recommended)

```bash
git clone https://github.com/deepanshu-s18/hr-leave-portal.git
cd hr-leave-portal

# Set environment (or use defaults)
export JWT_SECRET=your-256-bit-secret

docker-compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8082/api/v1 |
| Swagger UI | http://localhost:8082/swagger-ui.html |

### Option B — Local Development

```bash
# Backend
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Frontend (new terminal)
cd frontend
npm install
npm run dev
```

---

## 🔌 API Endpoints

### Auth
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Login (username/email + password) |
| `POST` | `/api/v1/auth/register` | Self-registration (EMPLOYEE role) |
| `POST` | `/api/v1/auth/refresh` | Refresh access token |

### Leave Management
| Method | Endpoint | Role | Description |
|---|---|---|---|
| `POST` | `/api/v1/leaves` | EMPLOYEE | Apply for leave |
| `GET` | `/api/v1/leaves/my` | EMPLOYEE | My leave history (paginated) |
| `PATCH` | `/api/v1/leaves/{id}/cancel` | EMPLOYEE | Cancel own leave |
| `GET` | `/api/v1/leaves/pending` | MANAGER+ | Pending leaves for my team |
| `PATCH` | `/api/v1/leaves/{id}/approve` | MANAGER+ | Approve a leave request |
| `PATCH` | `/api/v1/leaves/{id}/reject` | MANAGER+ | Reject a leave request |
| `GET` | `/api/v1/leaves` | ADMIN/HR | All leave requests (paginated) |

### Employees
| Method | Endpoint | Role | Description |
|---|---|---|---|
| `GET` | `/api/v1/employees/me` | ANY | Current employee profile |
| `GET` | `/api/v1/employees` | MANAGER+ | List active employees |
| `GET` | `/api/v1/employees/{id}` | MANAGER+ | Get employee by ID |
| `PATCH` | `/api/v1/employees/{id}/deactivate` | ADMIN | Deactivate account |

---

## 🧪 Testing

```bash
cd backend
mvn test
```

**20 unit tests** via JUnit 5 + Mockito:
- `apply()` — 7 tests (valid, past date, overlap, balance, sick/casual types)
- `approve()` — 6 tests (manager auth, admin override, PENDING guard, balance deduction)
- `reject()` — 4 tests (comment required, PENDING guard, auth)
- `cancel()` — 6 tests (ownership, APPROVED future, balance restoration, status guards)

---

## 🔑 Default Credentials (Seed Data)

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | ADMIN |
| `hrmanager` | `Hr@12345` | HR |
| `engmanager` | `Admin@123` | MANAGER |
| `deepanshu` | `Hr@12345` | EMPLOYEE |

---

## 📄 License

MIT — built for Google Application Engineering Intern application.
