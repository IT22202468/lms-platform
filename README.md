# LMS Microservices Platform

Learning Management System built as Spring Boot microservices with JWT authentication, API Gateway routing, and trusted identity propagation.

## Tech Stack

- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Cloud Gateway Server MVC
- MongoDB
- Maven

## Services

### `auth-service` (port `8081`)
- User registration and login
- Password hashing
- JWT generation
- Current principal probe endpoint

### `course-service` (port `8082`)
- Course lifecycle management
- Enrollment workflows
- Instructor/student scoped APIs
- Gateway-trusted identity extraction

### `api-gateway` (port `8085`)
- Route requests to internal services
- Validate JWT for protected routes
- Inject identity headers for downstream services

## Architecture

```text
Client
  |
  v
API Gateway (JWT validation + identity header injection)
  |                                |
  v                                v
Auth Service                    Course Service
  |                                |
  v                                v
MongoDB                          MongoDB
```

## Identity Propagation Contract

Headers injected by gateway on protected routes:
- `X-User-Id`
- `X-User-Email`
- `X-User-Roles`
- `X-Gateway-Auth: true`

`course-service` enforces gateway-only access using `IdentityExtractor`.

## API Endpoints

### Auth APIs

#### `POST /auth/register`
Registers a user and returns JWT.

Request:
```json
{
  "email": "user@example.com",
  "password": "password123",
  "role": "STUDENT"
}
```

Success: `201 Created`
```json
{
  "token": "<jwt>"
}
```

Errors:
- `400 Bad Request` validation error
- `409 Conflict` duplicate email

#### `POST /auth/login`
Authenticates user and returns JWT.

Request:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Success: `200 OK`
```json
{
  "token": "<jwt>"
}
```

Errors:
- `401 Unauthorized` invalid credentials

#### `GET /auth/me`
Returns current authenticated principal.

Success: `200 OK`
```json
{
  "name": "user-id-or-email"
}
```

Errors:
- `401 Unauthorized`

### Course APIs (protected via gateway)

#### `GET /courses`
List all published courses.

Success: `200 OK` (`CourseResponse[]`)

#### `GET /courses/{courseId}`
Get course by id.

Visibility rules:
- Published course: visible to any authenticated user
- Unpublished course: visible only to owning instructor

Success: `200 OK` (`CourseResponse`)

Errors:
- `400 Bad Request` course not found
- `403 Forbidden` unpublished course not owned by requester

#### `POST /courses`
Create a course (instructor only).

Request:
```json
{
  "title": "Intro to Java",
  "description": "Core Java fundamentals"
}
```

Success: `201 Created` (`CourseResponse`)

Errors:
- `400 Bad Request` validation failure
- `403 Forbidden` requester is not instructor

#### `PUT /courses/{courseId}`
Update own course title/description (instructor + owner only).

Request:
```json
{
  "title": "Intro to Java (Updated)",
  "description": "Updated description"
}
```

Success: `200 OK` (`CourseResponse`)

Errors:
- `400 Bad Request` course not found / validation failure
- `403 Forbidden` not owner or not instructor

#### `PUT /courses/{courseId}/publish`
Publish own course (instructor + owner only).

Success: `200 OK` (`CourseResponse`)

Errors:
- `400 Bad Request` course not found
- `403 Forbidden` not owner or not instructor

#### `DELETE /courses/{courseId}`
Delete own course (instructor + owner only).

Success: `200 OK`
```json
{
  "message": "Course deleted"
}
```

Errors:
- `400 Bad Request` course not found
- `403 Forbidden` not owner or not instructor

#### `POST /courses/{courseId}/enroll`
Enroll into published course (student only).

Rules:
- Must have `STUDENT` role
- Course must be published
- Cannot enroll twice

Success: `201 Created`
```json
{
  "message": "Enrolled"
}
```

Errors:
- `400 Bad Request` course not found / not published / already enrolled
- `403 Forbidden` requester is not student

#### `GET /instructor/courses`
List current instructor-owned courses.

Success: `200 OK` (`CourseResponse[]`)

Errors:
- `403 Forbidden` requester is not instructor

#### `GET /student/enrollments`
List current student enrollments.

Success: `200 OK` (`Enrollment[]`)

Errors:
- `403 Forbidden` requester is not student

#### `GET /courses/{courseId}/students`
List students enrolled in a course (instructor + owner only).

Success: `200 OK`
```json
[
  {
    "studentId": "abc123",
    "enrolledAt": "2026-04-06T10:00:00Z"
  }
]
```

Errors:
- `400 Bad Request` course not found
- `403 Forbidden` not owner or not instructor

## Current Implementation Status

Implemented:
- Auth service: register, login, me
- Course service: list/create/get/update/publish/delete/enroll, instructor courses, student enrollments, course students list, enrollment unique compound index, pagination on list endpoints
- Gateway: protected route JWT validation and identity header injection

Remaining roadmap:
- Dockerfiles and Docker Compose
- Production architecture concerns (service discovery, load balancing, Kubernetes)

## Local Run

From project root, run each service in separate terminals:
- `cd auth-service && ./mvnw spring-boot:run`
- `cd course-service && ./mvnw spring-boot:run`
- `cd api-gateway && ./mvnw spring-boot:run`

## Recommended Next Order

1. Add Dockerfiles + Docker Compose
2. Add production orchestration capabilities (Service Discovery, Load Balancing)
3. Implement Kubernetes manifests
