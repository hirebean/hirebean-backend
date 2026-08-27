# HireBean Backend

HireBean Backend is the Spring Boot API for a recruitment platform. It provides JWT authentication, candidate and
employer profiles, companies, job offers, applications, bookmarks, notifications, company posts, audit logs, file
storage, password-reset email delivery, and a local template-based career assistant.

The separate frontend repository is available at
[HireBean Frontend](https://github.com/darimachine/HireBean-Frontend).

## Technology overview

- Java 21 and Spring Boot
- PostgreSQL with Spring Data JPA
- Stateless bearer-token authentication with JWT
- Supabase Storage for public images and private CVs/resumes
- Gmail SMTP for password-reset messages
- Gradle Wrapper, JUnit, H2, Spotless, Docker, and Spring Boot Actuator
- Runtime OpenAPI documentation through Springdoc and Swagger UI

## Prerequisites

Install the following before starting the backend:

- JDK 21
- Docker Desktop, or Docker Engine with Docker Compose v2
- Git
- A Supabase project for file storage
- A Gmail SMTP account or app password if password-reset email delivery will be used

Gradle does not need to be installed globally; use the included Gradle Wrapper. Node.js and npm are not required to
run this Java backend.

## Local setup

### 1. Clone the repository

```bash
git clone <repository-url>
cd hirebean-backend
```

Run all following commands from the repository root. The application loads `.env` from the current working directory.

### 2. Create the local environment file

Windows PowerShell:

```powershell
Copy-Item env_example .env
```

Linux or macOS:

```bash
cp env_example .env
```

Edit `.env` and replace every `replace-with-...` or `your-...` value. The file is ignored by Git; never commit it.
See [Environment variables](#environment-variables) for each setting.

Generate a local JWT secret with one of these commands:

Windows PowerShell:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Linux or macOS:

```bash
openssl rand -base64 32
```

Copy the generated value into `JWT_SECRET`.

### 3. Configure Supabase Storage

In the Supabase dashboard:

1. Create a bucket named `hirebean-public` and mark it **Public**. It stores profile pictures, company logos, and post
   images.
2. Create a bucket named `hirebean-private` and leave it **Private**. It stores resumes and application CVs.
3. Copy the project URL into `SUPABASE_URL`.
4. Copy the legacy server-only `service_role` JWT into `SUPABASE_SECRET_KEY`.

The current Java REST client sends the storage credential as a bearer JWT, so it currently requires the legacy
`service_role` key. Supabase's newer `sb_secret_...` keys use different authorization semantics and require a backend
code change before they can replace it. Keep this key on the server only: it has elevated access and bypasses Storage
RLS. See the [Supabase API-key documentation](https://supabase.com/docs/guides/getting-started/api-keys) and the
[storage migration guide](docs/supabase-storage-migration.md).

### 4. Start PostgreSQL

The Compose file defines both PostgreSQL and the backend. This step starts PostgreSQL alone, which is what the Gradle
workflow in step 5 needs. It exposes the database on local port `5433`, matching the URL in `env_example`.

```bash
docker compose up -d postgres
docker compose ps
```

If port `5433` is already in use, update both the port mapping in `docker-compose.yml` and
`SPRING_DATASOURCE_URL` in `.env`.

### 5. Run the backend

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

Linux or macOS:

```bash
./gradlew bootRun
```

If the wrapper is not executable on Linux or macOS, run `chmod +x gradlew` once.

On first startup, the application creates the `CANDIDATE`, `EMPLOYER`, and `ADMIN` roles if they do not exist.

### Alternative: run the backend in Docker

Instead of steps 4 and 5, start PostgreSQL and the backend together:

```bash
docker compose up -d --build
```

```bash
docker compose up -d --build backend  # rebuild after changing Java code
```

### 6. Verify the installation

- Health: <http://localhost:8080/actuator/health>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Windows PowerShell health check:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Linux or macOS health check:

```bash
curl http://localhost:8080/actuator/health
```

## Environment variables

The values below are read from `.env` during local startup or from normal environment variables in deployed
environments.

| Variable | Required | Default/example | Purpose |
|---|---|---|---|
| `HIREBEAN_DB_USERNAME` | Yes | `hirebean` | PostgreSQL username. It must match the Compose database user. |
| `HIREBEAN_DB_PASS` | Yes | `change-this-database-password` | PostgreSQL password. Use a secret value outside local development. |
| `SPRING_DATASOURCE_URL` | Yes | `jdbc:postgresql://localhost:5433/hirebean_db` | JDBC connection URL. Inside the Compose/Kubernetes network the host and port differ. |
| `MAIL_USERNAME` | For password reset | `your-account@gmail.com` | Gmail SMTP username. |
| `MAIL_PASSWORD` | For password reset | `replace-with-an-app-password` | Gmail SMTP password or app-specific credential. |
| `APP_BACKEND_URL` | No | `http://localhost:8080` | Public backend base URL used in password-reset email links. |
| `APP_FRONTEND_URL` | No | `http://localhost:3000` | Frontend base URL used after reset-token confirmation. |
| `PORT` | No | `8080` | HTTP server port. |
| `JWT_SECRET` | Yes | No secure default | HMAC signing secret. Use a random value of at least 32 characters. |
| `APP_SEED_DEMO_DATA` | No | `false` | Enables idempotent local demo data. Never enable it in shared or production environments. |
| `SUPABASE_URL` | For file features | `https://your-project-ref.supabase.co` | Supabase project URL. |
| `SUPABASE_SECRET_KEY` | For file features | No default | Current backend's server-only legacy `service_role` JWT. Never expose it to clients. |
| `SUPABASE_SERVICE_ROLE_KEY` | Alternative | No default | Supported fallback name when `SUPABASE_SECRET_KEY` is absent. Do not set both. |
| `SUPABASE_PUBLIC_BUCKET` | No | `hirebean-public` | Public bucket for profile pictures, company logos, and post images. |
| `SUPABASE_PRIVATE_BUCKET` | No | `hirebean-private` | Private bucket for resumes and application CVs. |
| `SUPABASE_SIGNED_URL_SECONDS` | No | `600` | Lifetime of generated private-file URLs, in seconds. |

The backend does not use a Supabase publishable key or JWKS URL. Do not add frontend-only Supabase settings to this
backend's `.env`.

## Local demo data

Demo data is disabled by default. Set `APP_SEED_DEMO_DATA=true` only for a private local database, then restart the
application. The initializer is idempotent and preserves existing records.

| Role | Email | Password |
|---|---|---|
| Admin | `admin@hirebean.dev` | `Admin123!` |
| Employer | `employer@hirebean.dev` | `Employer123!` |
| Candidate | `candidate@hirebean.dev` | `Candidate123!` |

The seed also creates BluePeak Technologies, sample jobs and posts, an application, bookmarks, and notifications.
Disable it again after use.

## Useful commands

| Task | Windows | Linux/macOS |
|---|---|---|
| Run the backend | `.\gradlew.bat bootRun` | `./gradlew bootRun` |
| Run tests | `.\gradlew.bat test` | `./gradlew test` |
| Check formatting | `.\gradlew.bat spotlessCheck` | `./gradlew spotlessCheck` |
| Apply formatting | `.\gradlew.bat spotlessApply` | `./gradlew spotlessApply` |
| Build the application | `.\gradlew.bat build` | `./gradlew build` |
| Start PostgreSQL | `docker compose up -d postgres` | `docker compose up -d postgres` |
| View PostgreSQL logs | `docker compose logs -f postgres` | `docker compose logs -f postgres` |
| Start PostgreSQL and the backend | `docker compose up -d --build` | `docker compose up -d --build` |
| View backend logs | `docker compose logs -f backend` | `docker compose logs -f backend` |
| Rebuild the backend image | `docker compose build backend` | `docker compose build backend` |
| Stop the containers | `docker compose down` | `docker compose down` |

The built executable JAR is written under `build/libs/`.

## Authentication quick start

Register or log in through `/api/auth`, then send the returned token on protected requests:

```http
Authorization: Bearer <token>
```

Tokens expire after one hour by default. Logout revokes the current token. Detailed authorization rules and all
48 application endpoints are documented in the [API reference](docs/API_REFERENCE.md).

## Documentation

- [API reference](docs/API_REFERENCE.md)
- [Architecture](docs/ARCHITECTURE.md)
- [DevOps and deployment status](devops-documentation/README.md)
- [Supabase Storage migration](docs/supabase-storage-migration.md)

## Production caveats

The repository is ready for local development, but the checked-in deployment assets require review before production:

- Kubernetes and CD files still contain obsolete AWS-era settings and incomplete secret wiring. See the
  [deployment status](devops-documentation/README.md).
- Hibernate currently uses `ddl-auto: update`; introduce versioned database migrations before controlled production
  releases.
- SQL logging and detailed health output are enabled, and CORS currently accepts any origin pattern. Harden these
  settings for the target environment.
- Keep demo data disabled, rotate all example credentials, and use an external secret manager.
- The Supabase credential flow still depends on a legacy `service_role` JWT and should be migrated before that key
  format is retired.
- The Compose file is a local PostgreSQL dependency only; it is not a production stack.
