# HireBean Backend Architecture

## Overview

HireBean is a single Spring Boot application backed by PostgreSQL. HTTP controllers expose the API, services implement
business workflows, repositories provide persistence, and Spring Security applies stateless JWT authentication plus
method-level role and ownership checks.

External integrations are deliberately narrow:

- Supabase Storage holds uploaded objects; PostgreSQL stores their object keys.
- Gmail SMTP delivers password-reset and application-workflow email messages.
- The career-assistant endpoint currently uses local templates and has no external AI provider.

The separate frontend consumes this backend through JSON and multipart HTTP requests.

## Package structure

All application code is under `src/main/java/bg/uni/sofia/fmi/spring/hirebean`.

| Package | Responsibility |
|---|---|
| `config` | Security beans, startup role initialization, and optional demo-data initialization. |
| `controller` | HTTP routing, transport validation, status codes, and response types. |
| `dto.request` | Validated API input models. |
| `dto.response` | Public response and error models. |
| `model.entity` | JPA entities and relationships. |
| `model.enums` | Stable domain values used by persistence and API requests. |
| `repository` | Spring Data JPA persistence interfaces. |
| `service` | Service contracts and the shared storage implementation. |
| `service.impl` | Transactional business workflows and response mapping. |
| `security` | JWT parsing, user loading, ownership authorization, and job-visibility rules. |
| `exception` | Domain exceptions and centralized HTTP error mapping. |

## Request flow

For a protected request, the normal flow is:

1. `JwtAuthenticationFilter` reads `Authorization: Bearer <token>`.
2. The filter rejects revoked tokens, verifies the signature and expiry, then loads the current user and roles.
3. `SecurityConfig` applies route-level public/authenticated rules.
4. Controller `@PreAuthorize` expressions apply role, identity, company ownership, or resource ownership rules.
5. The controller validates transport input and calls a service.
6. The service enforces business rules, coordinates repositories and integrations, records relevant audit events, and
   maps entities to response DTOs.
7. `GlobalExceptionHandler` converts known failures into the common `ErrorResponse` envelope.

Controllers should remain transport-focused. Business and persistence decisions belong in services and repositories,
respectively.

## Security model

### Authentication

Registration and login are public. Successful authentication returns a signed JWT whose subject is the user's email.
The configured lifetime is one hour. The application does not create HTTP sessions.

Logout stores the current token in the `RevokedToken` table. The JWT filter checks that table on subsequent requests.
This makes logout effective before the token's natural expiry, at the cost of a database lookup for bearer-token
requests.

### Authorization

The three roles are `CANDIDATE`, `EMPLOYER`, and `ADMIN`. Route rules are augmented by
`OwnershipAuthorizationService`, which resolves the current database user and checks:

- user identity for profiles, bookmarks, notifications, and candidate application lists;
- employer-to-company ownership for companies, jobs, posts, and job applications;
- application, post, job, and notification ownership through their persisted relationships;
- admin override for administrative and support workflows.

Public job reads have visibility rules rather than simple unrestricted database access. Anonymous users and candidates
receive public active jobs, employers also receive non-public jobs for their own company, and admins receive all
statuses.

### Error boundary

Authentication failures return `401`; authenticated authorization failures return `403`. Validation, domain, and
integration failures are mapped centrally so API clients receive a stable object containing `status`, `error`,
`message`, `path`, and `timestamp`.

## Persistence and domain model

PostgreSQL is the primary database. Spring Data JPA repositories persist the main entities:

- `User`, `Role`, and `CandidateProfile`
- `Company`, `JobOffer`, `JobApplication`, and `Post`
- `Bookmark` and `Notification`
- `PasswordResetToken` and `RevokedToken`
- `Log` for audit events

Services define transaction boundaries. Response DTOs prevent JPA entities and internal relationships from becoming
the public API contract.

The current local configuration uses Hibernate `ddl-auto: update` and has no Flyway or Liquibase migrations. This is
convenient for development but unsuitable as the only schema-change mechanism for controlled deployments. Production
work should introduce versioned migrations, backups, and an explicit rollback process before disabling automatic
schema updates.

## File storage

`StorageService` calls the Supabase Storage REST API using Java's HTTP client. Database columns keep stable object keys
such as `resumes/<uuid>.pdf`; they do not store time-limited download URLs.

| Object prefix | Bucket type | Retrieval |
|---|---|---|
| `profile-pictures` | Public | Public object URL |
| `company-logos` | Public | Public object URL |
| `post-images` | Public | Public object URL |
| `resumes` | Private | Short-lived signed URL |
| `cvs` | Private | Short-lived signed URL |

Files are limited to 10 MB. Public and private bucket names and signed-URL lifetime are configurable through
environment variables.

The server credential has elevated access and must never reach the browser. The current implementation sends it as a
bearer JWT and therefore expects Supabase's legacy `service_role` key. Supabase is retiring legacy key formats in favor
of publishable and secret keys; migrating to an `sb_secret_...` key requires changing and testing the REST authorization
headers first. Track this as an integration migration rather than silently replacing the environment value.

See [Supabase Storage migration](supabase-storage-migration.md) for bucket and object migration details.

## Email and password reset

Email delivery uses Spring's mail abstraction with Gmail SMTP. Sending is asynchronous, so the initiating HTTP request
does not wait for SMTP delivery to finish.

Email messages are produced for:

- password-reset requests;
- application status changes and written feedback;
- interview invitations, updates, and cancellations.

The password-reset sequence is:

1. The user submits an email address.
2. The service creates a persisted reset token and emails a backend confirmation link.
3. The confirmation endpoint redirects the browser to the frontend reset page with the token.
4. The frontend submits the token and matching new-password fields to the final reset endpoint.

`APP_BACKEND_URL` and `APP_FRONTEND_URL` must represent externally reachable URLs in a deployed environment, or email
links and redirects will point to local addresses.

## Startup initialization

Two ordered startup initializers exist:

1. `DataInitializer` guarantees that all three roles exist. Registration depends on these records.
2. `DemoDataInitializer` runs only when `APP_SEED_DEMO_DATA=true`. It adds source-controlled local demo accounts and
   sample domain data idempotently.

Demo initialization must stay disabled in shared, staging, and production environments because it creates known
credentials.

The application also enables asynchronous method execution for email delivery.

## Observability and API discovery

Spring Boot Actuator exposes health, info, and metrics. Only health and info are public; metrics require authentication.
Detailed health output is currently enabled and should be reduced for production if it reveals dependency details.

Springdoc generates OpenAPI JSON at `/v3/api-docs` and Swagger UI at `/swagger-ui/index.html`. The generated document is
useful for runtime discovery, while [API_REFERENCE.md](API_REFERENCE.md) is the maintained source for ownership rules,
workflows, and examples.

Audit events are stored through `AuditLogService` and exposed to admins. Unexpected exceptions and server-side business
failures are logged by the global exception handler.

## Testing strategy

Tests run with JUnit and Spring Boot Test. The test configuration replaces PostgreSQL with an in-memory H2 database and
uses test values for mail, storage, URLs, and JWT signing.

The current suite includes application-context, controller, service, security, demo-initializer, and storage tests.
Controller tests exercise method authorization, while service tests cover ownership-sensitive business behavior.

Use both checks before merging:

```bash
./gradlew test
./gradlew spotlessCheck
```

Windows uses the equivalent `gradlew.bat` commands.

## Known limitations and evolution points

- There is no versioned database migration tool; Hibernate updates the schema automatically.
- The checked-in Kubernetes and CD assets still contain obsolete AWS-era variables and incomplete secret wiring. They
  must be repaired before use; see [DevOps and deployment status](../devops-documentation/README.md).
- CORS currently accepts any origin pattern while allowing credentials. Replace this with an environment-specific
  allowlist before public deployment.
- SQL logging and detailed health information are enabled globally.
- Storage depends on a legacy Supabase `service_role` JWT and needs a planned migration to the current secret-key model.
- Generic authenticated file upload accepts any valid single-segment folder name; unknown names are stored privately.
  Consider an explicit folder allowlist and endpoint-specific upload permissions.
- Swagger has no custom bearer security scheme or operation descriptions; it does not replace the maintained API
  reference.
- The AI assistant is a local deterministic template, not an LLM integration.

## Related documentation

- [Setup and configuration](../README.md)
- [API reference](API_REFERENCE.md)
- [DevOps and deployment status](../devops-documentation/README.md)
