# LMS Microservices Platform

A Learning Management System built as a production-grade Spring Boot microservices platform with JWT authentication, API Gateway routing, Docker, Kubernetes, CI/CD, and full observability (metrics + logs).

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Services](#services)
- [API Reference](#api-reference)
- [Error Handling](#error-handling)
- [Running Locally (Maven)](#running-locally-maven)
- [Docker Compose](#docker-compose)
- [Kubernetes](#kubernetes)
- [CI/CD Pipeline](#cicd-pipeline)
- [Observability](#observability)
  - [Metrics — Prometheus + Grafana](#metrics--prometheus--grafana)
  - [Logs — Loki + Promtail](#logs--loki--promtail)
- [Project Structure](#project-structure)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Security | Spring Security + JJWT 0.12.5 |
| Gateway | Spring Cloud Gateway Server MVC |
| Database | MongoDB 7 |
| Build | Maven 3.9 (wrapper included) |
| Containerisation | Docker + Docker Compose |
| Orchestration | Kubernetes |
| CI/CD | GitHub Actions |
| Metrics | Micrometer + Prometheus + Grafana |
| Logging | ECS JSON (structured) + Loki + Promtail |
| Test Coverage | JaCoCo (70% line minimum) |

---

## Architecture

```
                        Client
                          │
                          ▼
               ┌─────────────────────┐
               │     API Gateway      │  :8085
               │  JWT validation      │
               │  Identity injection  │
               └──────────┬──────────┘
                    │              │
                    ▼              ▼
          ┌──────────────┐  ┌──────────────┐
          │ auth-service │  │course-service│
          │    :8081     │  │    :8082     │
          └──────┬───────┘  └──────┬───────┘
                 │                  │
                 ▼                  ▼
            MongoDB             MongoDB
           (authdb)            (coursedb)


   Observability (all environments)
   ┌──────────────────────────────────────────┐
   │  Prometheus :9090  ←  scrapes /actuator/ │
   │  Loki :3100        ←  Promtail ships logs│
   │  Grafana :3000     →  dashboards         │
   └──────────────────────────────────────────┘
```

### Identity Propagation Contract

The gateway validates the JWT on protected routes and injects these headers before forwarding:

| Header | Value |
|---|---|
| `X-User-Id` | Subject from JWT |
| `X-User-Email` | Email claim |
| `X-User-Roles` | Roles claim (CSV) |
| `X-Gateway-Auth` | `true` |

`course-service` rejects any request missing `X-Gateway-Auth`, preventing direct calls that bypass the gateway.

---

## Services

### `auth-service` — port `8081`
- User registration and login
- Password hashing (BCrypt)
- JWT generation and signing

### `course-service` — port `8082`
- Course lifecycle (create / update / publish / delete)
- Enrollment workflows with duplicate prevention
- Instructor and student scoped queries
- Pagination on all list endpoints

### `api-gateway` — port `8085`
- Routes `/auth/**` → auth-service
- Routes `/courses/**`, `/instructor/**`, `/student/**` → course-service
- JWT validation and identity header injection for protected routes

---

## API Reference

All requests go through the gateway at `http://localhost:8085`.

### Auth

#### `POST /auth/register`
```json
// Request
{ "email": "user@example.com", "password": "password123", "role": "STUDENT" }

// 201 Created
{ "accessToken": "<jwt>", "tokenType": "Bearer" }
```
`role` is optional — defaults to `STUDENT`. Allowed values: `STUDENT`, `INSTRUCTOR`, `ADMIN`.

#### `POST /auth/login`
```json
// Request
{ "email": "user@example.com", "password": "password123" }

// 200 OK
{ "accessToken": "<jwt>", "tokenType": "Bearer" }
```

#### `GET /auth/me`
Requires `Authorization: Bearer <token>`. Returns the authenticated principal name.

---

### Courses

All course endpoints require `Authorization: Bearer <token>`.

| Method | Path | Role | Description |
|---|---|---|---|
| `GET` | `/courses` | any | List all published courses (paginated) |
| `GET` | `/courses/{id}` | any | Get course by ID |
| `POST` | `/courses` | INSTRUCTOR | Create a course |
| `PUT` | `/courses/{id}` | INSTRUCTOR | Update own course |
| `PUT` | `/courses/{id}/publish` | INSTRUCTOR | Publish own course |
| `DELETE` | `/courses/{id}` | INSTRUCTOR | Delete own course |
| `POST` | `/courses/{id}/enroll` | STUDENT | Enroll in a published course |
| `GET` | `/instructor/courses` | INSTRUCTOR | List own courses (paginated) |
| `GET` | `/student/enrollments` | STUDENT | List own enrollments (paginated) |
| `GET` | `/courses/{id}/students` | INSTRUCTOR | List enrolled students |

**Pagination parameters** (list endpoints): `?page=0&size=10`

**Create / Update request body:**
```json
{ "title": "Intro to Java", "description": "Core Java fundamentals" }
```

---

## Error Handling

All services use a `GlobalExceptionHandler` that maps typed exceptions to consistent JSON responses:

| Exception | HTTP Status |
|---|---|
| `CourseNotFoundException` | `404 Not Found` |
| `UnauthorizedException` | `403 Forbidden` |
| `ValidationException` | `400 Bad Request` |
| `MethodArgumentNotValidException` | `400 Bad Request` (with per-field errors) |
| Unexpected | `500 Internal Server Error` |

Error response shape:
```json
{ "message": "Course not found: abc123" }

// Validation errors include a field breakdown
{ "message": "Validation failed", "errors": ["title: must not be blank"] }
```

---

## Running Locally (Maven)

Prerequisites: Java 21, MongoDB running on `localhost:27017`.

```bash
# Terminal 1
cd auth-service && ./mvnw spring-boot:run

# Terminal 2
cd course-service && ./mvnw spring-boot:run

# Terminal 3
cd api-gateway && ./mvnw spring-boot:run
```

### Run tests with coverage report

```bash
# Run for a single service
cd auth-service && ./mvnw verify

# HTML coverage report
open auth-service/target/site/jacoco/index.html
```

JaCoCo enforces **70% line coverage** — `mvn verify` fails if the threshold is not met.

---

## Docker Compose

### Start the full stack

```bash
# First time or after code changes — rebuild images
docker compose up --build

# Subsequent starts (no code changes)
docker compose up
```

### Stop and clean up

```bash
# Stop containers (keep volumes)
docker compose down

# Stop and delete all data volumes
docker compose down -v
```

### Override the JWT secret

```bash
JWT_SECRET=my-secret docker compose up
```

### Service URLs

| Service | URL |
|---|---|
| API Gateway | http://localhost:8085 |
| auth-service | http://localhost:8081 |
| course-service | http://localhost:8082 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| Loki | http://localhost:3100 |

---

## Kubernetes

### Prerequisites

- `kubectl` configured against your cluster
- Images pushed to a registry (or built locally for Docker Desktop)

### Build and tag images

```bash
docker compose build
# Images are tagged as lms/auth-service:latest etc.
# For a registry, retag and push:
docker tag lms/auth-service:latest <registry>/auth-service:latest
docker push <registry>/auth-service:latest
```

### Deploy (first time)

```bash
# 1. Namespace
kubectl apply -f k8s/namespace.yaml

# 2. Secrets (edit k8s/secret.yaml with real values first, or use the command below)
kubectl create secret generic lms-secrets \
  --from-literal=JWT_SECRET="<your-secret>" \
  --from-literal=AUTH_MONGODB_URI="mongodb://mongodb:27017/authdb" \
  --from-literal=COURSE_MONGODB_URI="mongodb://mongodb:27017/coursedb" \
  --namespace=lms --dry-run=client -o yaml | kubectl apply -f -

# 3. MongoDB
kubectl apply -f k8s/mongodb/

# 4. Application services
kubectl apply -f k8s/auth-service/
kubectl apply -f k8s/course-service/
kubectl apply -f k8s/api-gateway/

# 5. Monitoring stack
kubectl apply -f k8s/monitoring/
```

### Apply everything at once (re-deploy)

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/mongodb/
kubectl apply -f k8s/auth-service/
kubectl apply -f k8s/course-service/
kubectl apply -f k8s/api-gateway/
kubectl apply -f k8s/monitoring/
```

### Useful kubectl commands

```bash
# Watch pods come up
kubectl get pods -n lms -w

# Check logs for a service
kubectl logs -n lms deployment/auth-service -f

# Describe a pod (useful for debugging startup failures)
kubectl describe pod -n lms -l app=auth-service

# Check rollout status
kubectl rollout status deployment/auth-service -n lms

# Port-forward a service for local testing
kubectl port-forward -n lms svc/api-gateway 8085:8085
```

### Kubernetes resource layout

```
k8s/
├── namespace.yaml
├── secret.yaml
├── mongodb/
│   ├── statefulset.yaml      # MongoDB with 1Gi PVC
│   └── service.yaml
├── auth-service/
│   ├── deployment.yaml
│   └── service.yaml
├── course-service/
│   ├── deployment.yaml
│   └── service.yaml
├── api-gateway/
│   ├── deployment.yaml
│   └── service.yaml          # type: LoadBalancer
└── monitoring/
    ├── prometheus-config.yaml
    ├── prometheus-deployment.yaml
    ├── grafana-deployment.yaml
    ├── loki-deployment.yaml
    └── promtail-daemonset.yaml
```

---

## CI/CD Pipeline

GitHub Actions workflow at `.github/workflows/ci-cd.yml`.

### Pipeline stages

```
Push to main / dev
       │
       ▼
  test (matrix: 3 services in parallel)
  ├── Java 21 setup + Maven cache
  ├── MongoDB service container
  ├── ./mvnw verify  (tests + JaCoCo 70% gate)
  └── Upload JaCoCo HTML report as artifact
       │
       ▼  (push events only — not PRs)
  build-push (matrix: 3 services in parallel)
  ├── Login to ghcr.io (GITHUB_TOKEN)
  ├── docker build
  └── Push ghcr.io/<owner>/<service>:sha-<commit> + :latest
       │
       ▼  (main branch only)
  deploy
  ├── Decode KUBE_CONFIG secret → ~/.kube/config
  ├── Sync K8s Secret from GitHub secrets
  ├── kubectl apply (namespace, mongodb, services, deployments)
  ├── kubectl set image (SHA-pinned tag per deployment)
  └── kubectl rollout status (waits for healthy rollout)
```

### Required GitHub Secrets

Go to **Settings → Secrets and variables → Actions** and add:

| Secret | How to get it |
|---|---|
| `KUBE_CONFIG` | `cat ~/.kube/config \| base64` |
| `JWT_SECRET` | Your JWT signing key |

`GITHUB_TOKEN` is provided automatically by GitHub — no action needed.

### Trigger behaviour

| Event | test | build-push | deploy |
|---|---|---|---|
| Push to `main` | ✅ | ✅ | ✅ |
| Push to `dev` | ✅ | ✅ | ❌ |
| PR to `main` | ✅ | ❌ | ❌ |

---

## Observability

### Metrics — Prometheus + Grafana

All services expose metrics at `/actuator/prometheus`. Prometheus scrapes every 15 seconds.

**Grafana** is pre-configured with Prometheus as a data source (via provisioning).

#### Access

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin / admin)

#### Recommended Grafana dashboards (import by ID)

| ID | Dashboard |
|---|---|
| `4701` | JVM (Micrometer) — memory, CPU, GC, threads |
| `11378` | Spring Boot Statistics — HTTP request rates per endpoint |
| `6756` | MongoDB overview |

To import: **Dashboards → Import → enter ID → select Prometheus datasource → Import**

#### Example PromQL queries

```promql
# HTTP request rate per service
rate(http_server_requests_seconds_count{application="auth-service"}[1m])

# 95th percentile response time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM heap used
jvm_memory_used_bytes{area="heap"}

# Active MongoDB connections
mongodb_driver_pool_size{application="course-service"}
```

---

### Logs — Loki + Promtail

All services log in **ECS JSON format** to stdout. Promtail ships those logs to Loki with labels extracted from the structured JSON.

#### Access

- Loki API: http://localhost:3100
- View logs in Grafana: **Explore → select Loki**

#### LogQL query examples

```logql
# All logs from auth-service
{service="auth-service"}

# ERROR logs across all services
{log_level="ERROR"}

# Errors from a specific service
{service="course-service", log_level="ERROR"}

# Full-text search within logs
{service="auth-service"} |= "Invalid credentials"

# Error rate over time (use in a Grafana panel)
sum by (service) (rate({log_level="ERROR"}[1m]))
```

#### How the labels are extracted

Promtail parses the ECS JSON each service produces and automatically promotes these fields as Loki labels:

| Loki label | Source in ECS JSON |
|---|---|
| `service` | `com.docker.compose.service` container label |
| `log_level` | `log.level` JSON field |
| `service_name` | `service.name` JSON field |

---

## Project Structure

```
lms-platform/
├── api-gateway/                  Spring Cloud Gateway (JWT validation, routing)
│   ├── src/
│   └── Dockerfile
├── auth-service/                 Authentication service (register, login, JWT)
│   ├── src/
│   └── Dockerfile
├── course-service/               Course & enrollment management
│   ├── src/
│   └── Dockerfile
├── monitoring/
│   ├── prometheus.yml            Prometheus scrape config (Docker Compose)
│   ├── loki-config.yml           Loki server config
│   ├── promtail-config.yml       Promtail Docker socket scrape config
│   └── grafana/provisioning/
│       └── datasources/
│           ├── prometheus.yml    Auto-wires Prometheus into Grafana
│           └── loki.yml          Auto-wires Loki into Grafana
├── k8s/
│   ├── namespace.yaml
│   ├── secret.yaml
│   ├── mongodb/
│   ├── auth-service/
│   ├── course-service/
│   ├── api-gateway/
│   └── monitoring/               Prometheus, Grafana, Loki, Promtail DaemonSet
├── .github/
│   └── workflows/
│       └── ci-cd.yml             GitHub Actions CI/CD pipeline
└── docker-compose.yml            Full local stack (all services + monitoring)
```
