# LMS Microservices Project — Comprehensive Technical Report

# 1. Project Overview

## Objective

Build a **Learning Management System (LMS)** using:

* Spring Boot Microservices
* MongoDB
* JWT Authentication
* API Gateway
* Service‑to‑Service Identity Propagation

---

# 2. Current Architecture

## Services Implemented

### 1. Auth Service

Handles:

* User registration
* User login
* JWT generation
* Password hashing

### 2. Course Service

Handles:

* Course creation
* Course publishing
* Course enrollment
* Instructor course listing
* Student enrollment listing

### 3. API Gateway

Handles:

* Routing requests
* JWT validation
* Identity propagation
* Service boundary enforcement

---

# 3. System Architecture

```
Client
  |
  v
API Gateway (JWT Validation + Routing)
  |                     |
  v                     v
Auth Service        Course Service
  |                     |
  v                     v
MongoDB              MongoDB
```

---

# 4. Auth Service Implementation

## Package Structure

```
auth-service
 ├── config
 ├── controller
 ├── dto
 ├── model
 ├── repo
 ├── security
 └── service
```

---

## Implemented Components

### application.properties

Configured:

* Service name
* Server port (8081)
* MongoDB connection
* JWT secret
* Actuator endpoints

---

### User Model

Fields:

* id
* email
* passwordHash
* roles
* createdAt

---

### UserRepository

Functions:

* findByEmail()
* existsByEmail()
* save()

---

### DTOs

#### RegisterRequest

```json
{
  "email": "user@example.com",
  "password": "password123",
  "role": "STUDENT"
}
```

#### LoginRequest

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### AuthResponse

```json
{
  "token": "jwt-token"
}
```

---

### JwtService

Responsibilities:

* Generate JWT
* Parse JWT
* Validate JWT

Token contains:

* subject → userId
* email
* roles

---

### AuthService

#### Register Flow

1. Normalize email
2. Check duplicate email
3. Hash password
4. Save user
5. Generate JWT

#### Login Flow

1. Find user by email
2. Validate password
3. Generate JWT

---

### AuthController

Endpoints:

```
POST /auth/register
POST /auth/login
```

---

### SecurityConfig

Configured:

* Stateless sessions
* Password encoder
* CSRF disabled

---

### GlobalExceptionHandler

Handles:

* Duplicate email
* Invalid login
* Validation errors

---

# 5. Course Service Implementation

## Package Structure

```
course-service
 ├── controller
 ├── dto
 ├── model
 ├── repo
 ├── security
 └── service
```

---

## Models

### Course

Fields:

* id
* title
* description
* instructorId
* published
* createdAt

---

### Enrollment

Fields:

* id
* courseId
* studentId
* enrolledAt

---

## Repositories

### CourseRepository

Functions:

* findPublished
* findByInstructorId

### EnrollmentRepository

Functions:

* existsByStudentAndCourse
* findByStudentId

---

# 6. Identity System

Headers used:

```
X-User-Id
X-User-Roles
X-Gateway-Auth
```

---

## RequestIdentity

Represents:

* userId
* roles

---

## IdentityExtractor

Responsibilities:

* Extract headers
* Validate gateway access
* Build RequestIdentity

---

# 7. Course Service Features

## Create Course

```
POST /courses
```

Instructor only

---

## Publish Course

```
PUT /courses/{id}/publish
```

---

## List Published Courses

```
GET /courses
```

---

## Enroll Student

```
POST /courses/{id}/enroll
```

Rules:

* Must be student
* Must be published
* Cannot enroll twice

---

## Instructor Courses

```
GET /instructor/courses
```

---

## Student Enrollments

```
GET /student/enrollments
```

---

# 8. API Gateway

Gateway Port:

```
8085
```

---

## Responsibilities

* Route requests
* Validate JWT
* Inject identity headers
* Protect services

---

# 9. Gateway Flow

```
Client
  ↓
Gateway
  ↓
JWT Validation
  ↓
Header Injection
  ↓
Course Service
  ↓
Business Logic
```

---

# 10. Completed Features

## Auth Service

* Register
* Login
* JWT generation

---

## Course Service

* Create course
* Publish course
* List courses
* Enroll student
* Instructor course list
* Student enrollment list

---

## API Gateway

* Routing
* JWT validation
* Header injection

---

# 11. Known Limitations

Missing features:

* Update course
* Delete course
* Get course by ID
* Pagination
* Dockerization
* Load balancing

---

# 12. Development Roadmap

## Phase 1 — Core Features

### Update Course

```
PUT /courses/{id}
```

---

### Get Course By ID

```
GET /courses/{id}
```

---

### Delete Course

```
DELETE /courses/{id}
```

---

# Phase 2 — Enrollment Improvements

Add unique index:

```
courseId + studentId
```

---

# Phase 3 — Instructor Tools

```
GET /courses/{id}/students
```

---

# Phase 4 — Pagination

Add pagination to:

* courses
* enrollments
* instructor courses

---

# Phase 5 — Dockerization

Create:

* Dockerfile auth-service
* Dockerfile course-service
* Dockerfile gateway

---

# Phase 6 — Docker Compose

Run:

* auth-service
* course-service
* gateway
* mongodb

---

# Phase 7 — Production Architecture

Add:

* Load balancing
* Kubernetes
* Service discovery

---

# 13. Current Project Status

Maturity Level:

Intermediate Microservices Architecture

---

# 14. Recommended Next Order

1. Update course
2. Get course by id
3. Delete course
4. Instructor students list
5. Pagination
6. Dockerization
7. Load balancing

---

# 15. AI Agent Instructions

AI agent should:

1. Continue development from course-service
2. Add update course endpoint
3. Maintain architecture pattern
4. Use identity extraction system
5. Add validation rules

