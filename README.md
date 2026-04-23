# OtterSpaceLearn — LMS Microservices Platform

A Learning Management System built with Spring Boot microservices, deployed on an Oracle Cloud VM with Docker, Nginx reverse proxy, automated CI/CD, and SonarCloud code quality analysis.

**Live API:** `http://140.245.196.215`
**Frontend:** [otterspacelearn.vercel.app](https://otterspacelearn.vercel.app)

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Services](#services)
- [API Reference](#api-reference)
- [Error Handling](#error-handling)
- [Running Locally](#running-locally)
- [Docker Compose](#docker-compose)
- [Production Deployment](#production-deployment)
- [CI/CD Pipeline](#cicd-pipeline)
- [Nginx Reverse Proxy](#nginx-reverse-proxy)
- [Observability](#observability)
- [Kubernetes Production](#kubernetes-production)
- [Project Structure](#project-structure)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Security | Spring Security + JJWT 0.12.5 |
| Gateway | Spring Cloud Gateway Server MVC |
| Database | MongoDB 7 (Atlas in production) |
| Frontend | Next.js (separate repo, Vercel-hosted) |
| Build | Maven 3.9 (wrapper included) |
| Containerisation | Docker + Docker Compose |
| Reverse Proxy | Nginx |
| CI/CD | GitHub Actions |
| Code Quality | SonarCloud + JaCoCo (70% coverage gate) |
| Metrics | Micrometer + Prometheus + Grafana |
| Logging | ECS JSON (structured) + Loki + Promtail |
| VM | Oracle Cloud (1GB RAM) |

---

## Architecture

```
                     Browser / Client
                          │
                          ▼
                   ┌─────────────┐
                   │   Nginx     │  :80 (public)
                   │  reverse    │
                   │   proxy     │
                   └──────┬──────┘
                          │
                          ▼
               ┌─────────────────────┐
               │     API Gateway      │  :8085 (internal)
               │  JWT validation      │
               │  CORS handling       │
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
           MongoDB Atlas       MongoDB Atlas
            (authdb)            (coursedb)
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
- JWT generation and signing (60-min expiry)
- Roles: `STUDENT`, `INSTRUCTOR`, `ADMIN`

### `course-service` — port `8082`
- Course lifecycle (create / update / publish / delete)
- Enrollment workflows with duplicate prevention
- Instructor and student scoped queries
- Pagination on all list endpoints

### `api-gateway` — port `8085`
- Routes `/auth/**` → auth-service (no auth required)
- Routes `/courses/**`, `/instructor/**`, `/student/**` → course-service (JWT required)
- JWT validation and identity header injection
- CORS configuration (allows Vercel frontend + localhost dev servers)

---

## API Reference

All requests go through the gateway. In production: `http://140.245.196.215`. Locally: `http://localhost:8085`.

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

```json
{ "message": "Course not found: abc123" }

// Validation errors include a field breakdown
{ "message": "Validation failed", "errors": ["title: must not be blank"] }
```

---

## Running Locally

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
cd auth-service && ./mvnw verify
open auth-service/target/site/jacoco/index.html
```

JaCoCo enforces **70% line coverage** — `mvn verify` fails if the threshold is not met.

---

## Docker Compose

### Local development (full stack)

```bash
# Build and start all services + MongoDB + monitoring
docker compose up --build

# Stop (keep data)
docker compose down

# Stop and delete all data
docker compose down -v
```

### Production (on VM)

```bash
# Uses pre-built images from GHCR + MongoDB Atlas
docker compose -f docker-compose.prod.yml up -d

# View logs
docker compose -f docker-compose.prod.yml logs -f

# Restart after new images are pushed
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --remove-orphans
```

**Required `.env` file** on the VM (at `~/lms-platform/.env`):
```
JWT_SECRET="<your-jwt-signing-key>"
MONGODB_URI="mongodb+srv://<user>:<pass>@<cluster>.mongodb.net"
```

### Container memory limits (production)

| Service | Memory Limit | JVM Max Heap |
|---|---|---|
| auth-service | 256M | 160m |
| course-service | 256M | 160m |
| api-gateway | 192M | 128m |

---

## Production Deployment

The backend runs on a **Kubernetes cluster** with GHCR images and GitHub Actions deployment automation.

### Infrastructure overview

| Component | Details |
|---|---|
| Runtime | Kubernetes namespace `lms` |
| Workloads | Deployments + Services + HorizontalPodAutoscalers |
| Database | MongoDB Atlas (authdb + coursedb) |
| Ingress / Entry | `api-gateway` Service (public entrypoint depends on your cluster ingress/LB) |
| Frontend | Next.js on Vercel (otterspacelearn.vercel.app) |
| Code Quality | SonarCloud (chosen over self-hosted SonarQube due to memory) |
| CI/CD | GitHub Actions auto-deploys on push to main |

### Cluster setup steps

1. Provision a Kubernetes cluster and install `kubectl`
2. Install `metrics-server` (required for HPA CPU metrics)
3. Add GitHub repository secrets for cluster access and runtime app secrets
4. Push to `main` to trigger CI image build + Kubernetes deployment
5. Verify rollout and autoscaling with `kubectl get deploy,hpa -n lms`

---

## CI/CD Pipeline

GitHub Actions workflow at `.github/workflows/ci-cd.yml`.

```
Push to main / dev
       │
       ▼
  test (matrix: 3 services in parallel)
  ├── MongoDB service container
  ├── ./mvnw verify (tests + JaCoCo 70% gate)
  └── Upload JaCoCo report as artifact
       │
       ▼
  sonar (matrix: 3 services, each with own token)
  └── SonarCloud analysis per service
       │
       ▼  (push events only)
  build-push (matrix: 3 services)
  ├── Login to ghcr.io
  └── Push :sha-<commit> + :latest tags
       │
       ▼  (main branch only)
  deploy
  ├── Configure kubeconfig from secret
  ├── Apply namespace + services + deployments + HPAs
  ├── Set deployment images to :sha-<commit>
  └── Validate rollout + HPA status
```

### Trigger behaviour

| Event | test | sonar | build-push | deploy |
|---|---|---|---|---|
| Push to `main` | yes | yes | yes | yes |
| Push to `dev` | yes | yes | yes | no |
| PR to `main` | yes | yes | no | no |

### Required GitHub Secrets

| Secret | Purpose |
|---|---|
| `KUBECONFIG` | Cluster kubeconfig content used by deploy job |
| `JWT_SECRET` | JWT signing key (shared across services) |
| `AUTH_MONGODB_URI` | Full MongoDB Atlas URI for auth-service DB |
| `COURSE_MONGODB_URI` | Full MongoDB Atlas URI for course-service DB |
| `SONAR_TOKEN_AUTH` | SonarCloud token for auth-service |
| `SONAR_TOKEN_COURSE` | SonarCloud token for course-service |
| `SONAR_TOKEN_GATEWAY` | SonarCloud token for api-gateway |

`GITHUB_TOKEN` is provided automatically.

---

## Nginx Reverse Proxy

Config files are in the `nginx/` directory.

- `api-gateway.conf` — Nginx site config (port 80 → localhost:8085)
- `setup.sh` — Installation script for the VM

### What it does

- Proxies all traffic from port 80 to the api-gateway on 8085
- Forwards client headers (X-Real-IP, X-Forwarded-For, X-Forwarded-Proto)
- Blocks public access to `/actuator` endpoints
- Exposes `/health` endpoint for uptime monitoring
- Supports WebSocket upgrade headers
- Has a commented HTTPS block ready for Let's Encrypt SSL

### Setup on VM

```bash
# Copy files to VM
scp -i <key-file> -r nginx/ ubuntu@<vm-ip>:~/

# On the VM
cd ~/nginx
sudo bash setup.sh
```

### SSL setup (after pointing a domain to the VM)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.yourdomain.com
```

---

## Observability

Monitoring configs are in the `monitoring/` directory. These run in Docker Compose for local development. Not deployed on the production VM due to 1GB memory constraint.

### Metrics — Prometheus + Grafana

All services expose `/actuator/prometheus`. Prometheus scrapes every 15s.

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin / admin)

**Recommended Grafana dashboard IDs:** `4701` (JVM), `11378` (Spring Boot), `6756` (MongoDB)

```promql
# HTTP request rate
rate(http_server_requests_seconds_count{application="auth-service"}[1m])

# 95th percentile response time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM heap used
jvm_memory_used_bytes{area="heap"}
```

### Logs — Loki + Promtail

All services log in ECS JSON format. Promtail ships logs to Loki with labels extracted from structured JSON.

```logql
# All logs from auth-service
{service="auth-service"}

# ERROR logs across all services
{log_level="ERROR"}

# Full-text search
{service="auth-service"} |= "Invalid credentials"
```

---

## Kubernetes Production

K8s manifests in the `k8s/` directory are used by production CI/CD.

### Prerequisites

- Cluster has `metrics-server` installed (`kubectl top nodes` works).
- GHCR images are accessible by the cluster.
- Namespace `lms` exists (CI applies `k8s/namespace.yaml`).
- GitHub repository secrets are configured (`KUBECONFIG`, `JWT_SECRET`, `AUTH_MONGODB_URI`, `COURSE_MONGODB_URI`).
- For 1GB VM nodes, keep the low-memory profile in these manifests (requests/limits + HPA range 1-2 + namespace quota).

### Deploy manifests manually (optional)

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/resource-quota.yaml
kubectl apply -f k8s/auth-service/service.yaml
kubectl apply -f k8s/course-service/service.yaml
kubectl apply -f k8s/api-gateway/service.yaml
kubectl apply -f k8s/auth-service/deployment.yaml
kubectl apply -f k8s/course-service/deployment.yaml
kubectl apply -f k8s/api-gateway/deployment.yaml
kubectl apply -f k8s/auth-service/hpa.yaml
kubectl apply -f k8s/course-service/hpa.yaml
kubectl apply -f k8s/api-gateway/hpa.yaml
```

### Rollout verification

```bash
kubectl get deploy -n lms
kubectl rollout status deployment/auth-service -n lms
kubectl rollout status deployment/course-service -n lms
kubectl rollout status deployment/api-gateway -n lms
kubectl get hpa -n lms
kubectl top pods -n lms
```

### Rollback

```bash
kubectl rollout undo deployment/auth-service -n lms
kubectl rollout undo deployment/course-service -n lms
kubectl rollout undo deployment/api-gateway -n lms
```

```
k8s/
├── namespace.yaml
├── resource-quota.yaml       Namespace guardrail for 1GB VM capacity
├── secret.yaml               Placeholder secret manifest (CI creates runtime secret)
├── auth-service/             Deployment + ClusterIP + HPA
├── course-service/           Deployment + ClusterIP + HPA
├── api-gateway/              Deployment + Service + HPA
├── mongodb/                  Optional in-cluster StatefulSet + PVC (not used with Atlas)
└── monitoring/               Optional Prometheus, Grafana, Loki, Promtail
```

---

## Project Structure

```
lms-platform/
├── api-gateway/                  Spring Cloud Gateway (JWT, routing, CORS)
│   ├── src/
│   └── Dockerfile
├── auth-service/                 Authentication (register, login, JWT)
│   ├── src/
│   └── Dockerfile
├── course-service/               Course & enrollment management
│   ├── src/
│   └── Dockerfile
├── nginx/
│   ├── api-gateway.conf          Nginx reverse proxy config
│   └── setup.sh                  VM setup script
├── monitoring/
│   ├── prometheus.yml            Scrape config
│   ├── loki-config.yml           Loki server config
│   ├── promtail-config.yml       Log shipping config
│   └── grafana/provisioning/     Auto-wired datasources
├── k8s/                          Kubernetes manifests
├── .github/workflows/
│   └── ci-cd.yml                 CI/CD pipeline
├── docker-compose.yml            Local dev (all services + monitoring)
└── docker-compose.prod.yml       Production (GHCR images + Atlas)
```
