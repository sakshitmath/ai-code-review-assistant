# AI Code Review Assistant

An AI-powered full-stack application that automates Java code review. Users upload source files, ZIP archives, or paste snippets — the system analyses the code using three static analysis tools and a Large Language Model, then presents bugs, code smells, security issues, and refactoring suggestions in an interactive dashboard.

Built with Spring Boot, React, PostgreSQL, and Groq.

---

## Objective

Manual code review is slow and inconsistent. This project automates the first pass: rule-based static analysis catches standards violations and known bug patterns, while an LLM provides semantic review that pattern-matching tools cannot — explaining *why* code is problematic and suggesting concrete fixes.

The result is a tool that mirrors what modern engineering teams use internally, built end to end: authentication, file processing, third-party analysis engines, AI integration, relational persistence, and production deployment.

---

## Live Demo

| Service | URL |
|---|---|
| Frontend | https://ai-code-review-assistant-wheat.vercel.app |
| Backend API | https://ai-code-review-backend-ofy1.onrender.com |
| Health Check | https://ai-code-review-backend-ofy1.onrender.com/api/health |

> The backend runs on a free tier and spins down when idle. The first request may take up to 50 seconds to wake the service.

---

## Screenshots

![Dashboard](docs/dashboard.png)
<img width="1917" height="845" alt="Dashboard ai-review" src="https://github.com/user-attachments/assets/8a6be915-81cb-4b77-bb87-2e96fd4c4a32" />

*Analysis results with quality score, severity breakdown, and detailed findings*
<img width="1900" height="857" alt="Static results" src="https://github.com/user-attachments/assets/c7fb2775-6934-4409-9fe5-6a8dff738074" />

![Login](docs/login.png)
*JWT-authenticated login*
<img width="1917" height="881" alt="login ai-review" src="https://github.com/user-attachments/assets/809201fd-a39c-4a47-8dd2-291bc523807a" />

----

## Features

**Authentication** — Register, login, logout, update profile, reset password. Stateless JWT with BCrypt password hashing.

**Code Submission** — Upload `.java` files, upload `.zip` archives with automatic extraction and junk-folder filtering, or paste snippets directly.

**Static Analysis** — Three tools run programmatically inside the backend:

| Tool | Analyses | Detects |
|---|---|---|
| Checkstyle 10.21.4 | Source AST | Coding standards, naming, magic numbers |
| PMD 7.10.0 | Source AST | Code smells, performance, security rules |
| SpotBugs 4.8.6 | Bytecode | Null dereferences, resource leaks, contract violations |

SpotBugs compiles source to bytecode at runtime, allowing it to catch dataflow bugs invisible to source-level tools.

**AI Review** — Sends code to an LLM with a schema-enforcing prompt. Returns structured findings covering bugs, security vulnerabilities, performance, naming, and refactoring, with a quality score out of 100.

**Complexity Analysis** — Classes, methods, lines of code, cyclomatic complexity, average method length, maintainability index.

**Documentation Generator** — Produces class and method documentation with parameters, return values, and exceptions. Exportable as styled HTML or PDF.

**Dashboard** — Project management, search, severity filtering, review history, Chart.js visualisation, dark theme.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite, Tailwind CSS |
| Backend | Spring Boot 3.5, Java 21, Maven |
| Security | Spring Security, JWT (jjwt) |
| Persistence | Spring Data JPA / Hibernate |
| Database | H2 (dev) · PostgreSQL/Neon (prod) |
| Static Analysis | Checkstyle, PMD, SpotBugs |
| AI | Groq — Llama 3.3 70B |
| Visualisation | Chart.js |
| Deployment | Render (Docker) · Vercel |

---

## Architecture

```
┌─────────────────┐        ┌──────────────────────────┐        ┌──────────────┐
│  React Frontend │ ──JWT─▶│   Spring Boot Backend    │ ──────▶│  PostgreSQL  │
│    (Vercel)     │        │     (Render / Docker)    │        │    (Neon)    │
└─────────────────┘        └──────────────────────────┘        └──────────────┘
                                       │
                           ┌───────────┼───────────┬─────────────┐
                           ▼           ▼           ▼             ▼
                     Checkstyle       PMD      SpotBugs      Groq LLM
```

**Request flow**

1. User authenticates; backend issues a signed JWT.
2. Axios attaches the token to every request; a custom `JwtAuthenticationFilter` validates it before any controller executes.
3. Uploaded files are stored in a per-user UUID directory. ZIP archives are extracted with build and dependency folders filtered out.
4. Each analysis tool runs in isolation — one tool failing cannot break the others. Findings are normalised into a common entity.
5. Code is bundled and sent to the LLM with a JSON-schema prompt; the response is cleaned and parsed.
6. Quality score computed as `100 − (HIGH×5 + MEDIUM×2 + LOW×1)`.
7. Reviews and findings persist with cascade delete.

---

## Database Schema

```
users                    projects                  reviews                review_findings
─────                    ────────                  ───────                ───────────────
id           PK          id            PK          id           PK        id          PK
name                     user_id       FK          project_id   FK        review_id   FK
email        UNIQUE      project_name             review_type            tool
password     (BCrypt)    upload_type              review_score           severity
created_at               storage_path             summary                issue
                         file_count               created_at             explanation
                         created_at                                      suggestion
                                                                         file_name
                                                                         line_number
```

- `users` 1 → N `projects`
- `projects` 1 → N `reviews`
- `reviews` 1 → N `review_findings` *(cascade delete, orphan removal)*

---

## API Reference

All endpoints are prefixed with `/api`. Protected routes require `Authorization: Bearer <token>`.

### Authentication
| Method | Endpoint | Auth | Description |
|---|---|:---:|---|
| POST | `/auth/register` | — | Create account, returns JWT |
| POST | `/auth/login` | — | Authenticate, returns JWT |
| POST | `/auth/logout` | ✓ | Clear security context |

### User
| Method | Endpoint | Auth | Description |
|---|---|:---:|---|
| GET | `/user/me` | ✓ | Current user details |
| PUT | `/user/profile` | ✓ | Update display name |
| PUT | `/user/reset-password` | ✓ | Change password |

### Projects
| Method | Endpoint | Auth | Description |
|---|---|:---:|---|
| POST | `/projects/upload/files` | ✓ | Upload `.java` files |
| POST | `/projects/upload/zip` | ✓ | Upload and extract `.zip` |
| POST | `/projects/snippet` | ✓ | Save a pasted snippet |
| GET | `/projects` | ✓ | List all projects |
| GET | `/projects/{id}` | ✓ | Get one project |
| GET | `/projects/search?keyword=` | ✓ | Search by name |
| GET | `/projects/{id}/file?fileName=` | ✓ | Read file contents |
| DELETE | `/projects/{id}` | ✓ | Delete project |

### Reviews & Analysis
| Method | Endpoint | Auth | Description |
|---|---|:---:|---|
| POST | `/reviews/static/{projectId}` | ✓ | Run Checkstyle, PMD, SpotBugs |
| POST | `/reviews/ai/{projectId}` | ✓ | Run LLM review |
| GET | `/reviews/project/{projectId}` | ✓ | Review history |
| GET | `/reviews/{reviewId}` | ✓ | Get one review |
| DELETE | `/reviews/{reviewId}` | ✓ | Delete review |
| GET | `/complexity/{projectId}` | ✓ | Complexity metrics |
| POST | `/documentation/{projectId}` | ✓ | Generate documentation |
| GET | `/health` | — | Service health check |

**Example**

```bash
curl -X POST https://ai-code-review-backend-ofy1.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane","email":"jane@example.com","password":"password123"}'
```

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "userId": 1,
  "name": "Jane",
  "email": "jane@example.com"
}
```

---

## Project Structure

```
ai-code-review-assistant/
│
├── backend/
│   ├── src/main/java/com/aicodereview/backend/
│   │   ├── controller/       REST endpoints
│   │   ├── service/          Business logic and analysis engines
│   │   ├── repository/       Spring Data JPA interfaces
│   │   ├── entity/           JPA entities
│   │   ├── dto/              Request and response objects
│   │   ├── security/         JWT service, filter, user details
│   │   ├── util/             File storage helpers
│   │   ├── config/           Security and CORS configuration
│   │   └── exception/        Global exception handling
│   │
│   ├── src/main/resources/
│   │   ├── application-dev.properties      H2 profile
│   │   ├── application-prod.properties     PostgreSQL profile
│   │   ├── checkstyle.xml
│   │   └── pmd-ruleset.xml
│   │
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/
│   └── src/
│       ├── pages/            Login, Register, Dashboard, Profile
│       ├── components/       UploadPanel, ProjectList, AnalysisPanel
│       └── services/         Axios client with JWT interceptor
│
└── README.md
```

---

## Local Setup

**Prerequisites** — Java 21, Node.js 18+, and a free [Groq API key](https://console.groq.com).

### Backend

```bash
cd backend
```

Set the environment variable `GROQ_API_KEY`, then run:

```bash
./mvnw spring-boot:run
```

Runs on `http://localhost:8081` with an H2 in-memory database.
H2 console: `http://localhost:8081/h2-console` · JDBC URL `jdbc:h2:mem:codereviewdb` · user `sa`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`. To target a deployed backend, create `frontend/.env`:

```
VITE_API_URL=https://your-backend-url/api
```

---

## Configuration

All credentials are supplied as environment variables and are never committed to the repository.

**Backend (production)**

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Set to `prod` |
| `DATABASE_URL` | JDBC PostgreSQL connection string |
| `DB_USERNAME` · `DB_PASSWORD` | Database credentials |
| `GROQ_API_KEY` | LLM API key |
| `GROQ_API_URL` · `GROQ_API_MODEL` | LLM endpoint and model |

**Frontend**

| Variable | Purpose |
|---|---|
| `VITE_API_URL` | Backend API base URL |

---

## Deployment

**Backend** — Deployed to Render as a Docker web service using a multi-stage build. The runtime image uses a JDK rather than a JRE, since SpotBugs invokes the Java compiler at runtime.

**Frontend** — Deployed to Vercel from the `frontend` directory using the Vite preset.

**Database** — Neon serverless PostgreSQL, with schema managed by Hibernate.

Both services auto-deploy on push to `main`.

---

## Documentation

- [Sample Test Cases](TEST_CASES.md)
