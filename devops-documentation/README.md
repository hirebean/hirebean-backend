# HireBean DevOps and Deployment Status

This document describes the deployment assets that currently exist in the repository. It does not claim that the
checked-in Kubernetes and continuous-deployment configuration is production-ready.

> **Current status:** local development and CI are usable. The Kubernetes manifests and CD workflow still contain
> obsolete AWS-era settings and broken secret mappings. Repair and validate them before attempting a deployment.

## Local infrastructure

The root [`docker-compose.yml`](../docker-compose.yml) starts a PostgreSQL 17 container only. It is a local dependency,
not a complete application stack.

```bash
docker compose up -d postgres
docker compose ps
docker compose logs -f postgres
```

The database is exposed as `localhost:5433`; the container listens on `5432`. The matching local JDBC URL is:

```text
jdbc:postgresql://localhost:5433/hirebean_db
```

The backend runs separately through the Gradle Wrapper. See the root [setup guide](../README.md).

## Container image

The root [`Dockerfile`](../Dockerfile) is a multi-stage Java 21 build:

1. A Gradle/JDK 21 image builds the executable JAR with tests skipped.
2. A Java 21 JRE image copies the JAR and listens on port `8080`.

Build and inspect the image locally only after tests have passed:

```bash
./gradlew test spotlessCheck
docker build -t hirebean-backend:local .
```

The container still needs all required environment variables and reachable PostgreSQL, Supabase, and SMTP services.
The repository does not currently provide a Compose service that wires the backend container to PostgreSQL.

## Continuous integration

The [CI workflow](../.github/workflows/ci.yml) runs on pushes and pull requests to `main`:

1. Check out the repository.
2. Configure Temurin JDK 21.
3. Configure Gradle caching/tooling.
4. Run `spotlessCheck`.
5. Run the full test suite.
6. Run a Trivy filesystem scan.

The Trivy step currently uses `exit-code: 0`, so findings are reported but do not fail CI. Change that policy deliberately
when the team is ready to enforce a vulnerability threshold.

## Kubernetes assets: repair required

The `k8s/` directory contains backend and PostgreSQL Deployments, Services, a database PVC, and a health-check script.
The backend defines Actuator liveness and readiness probes at `/actuator/health`.

Do not apply the manifests as a production deployment in their current state. Known problems include:

- `k8s/apps/backend/deployment.yaml` still injects removed AWS S3 and CDN settings.
- The backend Deployment does not inject required `JWT_SECRET`, `SUPABASE_URL`, or a current Supabase storage key.
- It references separate `mail-secret` and `aws-secret` objects that the CD workflow does not create consistently.
- The image is hard-coded to `uchihadari/hirebean-backend:latest`, while the workflow builds from a configurable Docker
  Hub username.
- The database is a single Deployment with a small PVC; availability, backup, restore, and upgrade procedures are not
  defined.
- The backend Service is internal-only. No reviewed Ingress, TLS, DNS, or external load-balancer configuration is
  checked in.

## Continuous deployment workflow: repair required

The [CD workflow](../.github/workflows/cd.yml) is manual and creates an ephemeral Kind cluster on a GitHub-hosted runner.
It should be treated as an unfinished deployment experiment, not a persistent environment.

Current blocking defects include:

- `SPRING_DATASOURCE_URL` is populated from the `MAIL_USERNAME` secret.
- a `CDN_URL` value is populated from `MAIL_PASSWORD` and is also declared twice;
- obsolete AWS/CDN secrets are created while current Supabase and JWT secrets are absent;
- shell continuation and secret creation are inconsistent with the secret names consumed by the manifests;
- applying to the runner's temporary Kind cluster does not deploy to a durable external cluster.

## Required deployment configuration

Any repaired deployment must provide the current backend settings below. Store secret values in the target platform's
secret manager; do not commit them in manifests or workflow files.

| Setting | Sensitivity | Notes |
|---|---|---|
| `SPRING_DATASOURCE_URL` | Configuration | Use the in-cluster database hostname and port, or a managed PostgreSQL URL. |
| `HIREBEAN_DB_USERNAME` | Secret | PostgreSQL user. |
| `HIREBEAN_DB_PASS` | Secret | PostgreSQL password. |
| `JWT_SECRET` | Secret | Random signing value of at least 32 characters. |
| `SUPABASE_URL` | Configuration | Supabase project URL. |
| `SUPABASE_SECRET_KEY` | Secret | Current implementation's server-only legacy `service_role` JWT. |
| `MAIL_USERNAME` | Secret | SMTP account. |
| `MAIL_PASSWORD` | Secret | SMTP credential. |
| `APP_BACKEND_URL` | Configuration | Externally reachable backend URL used in email links. |
| `APP_FRONTEND_URL` | Configuration | Externally reachable frontend URL used for redirects. |
| `APP_SEED_DEMO_DATA` | Configuration | Must remain `false`. |

Bucket names, signed-URL lifetime, and `PORT` may use the defaults documented in [`env_example`](../env_example) or be
overridden explicitly.

## Deployment repair checklist

Before calling the Kubernetes/CD path deployable:

1. Remove every AWS/CDN variable and align all manifests with the current environment-variable list.
2. Define one consistent secret contract and create those secrets through the target platform's secret manager.
3. Replace the hard-coded image with an immutable registry tag or digest produced by the workflow.
4. Target a persistent cluster and separate environment-specific configuration from base manifests.
5. Add reviewed Ingress/TLS/DNS configuration where external access is required.
6. Replace Hibernate automatic schema updates with versioned database migrations and a rollback plan.
7. Define PostgreSQL backup, restore, upgrade, resource, and availability procedures.
8. Restrict CORS, reduce Actuator health detail, disable SQL logging, and keep Swagger exposure intentional.
9. Migrate the Supabase REST client from the legacy `service_role` JWT to the current secret-key authorization model.
10. Run tests, formatting, image scanning, manifest validation, rollout checks, and an authenticated API smoke test.

Suggested release gates:

```bash
./gradlew spotlessCheck test
docker build -t <registry>/hirebean-backend:<immutable-tag> .
```

Kubernetes commands should be added only after the manifests and target cluster have been repaired and reviewed.

## Health verification

The application exposes:

- `/actuator/health` for liveness/readiness;
- `/actuator/info` for application information;
- `/actuator/metrics` for authenticated metrics access.

A successful health response alone is not a complete deployment test. A smoke test should also register or log in,
exercise one protected endpoint, verify PostgreSQL persistence, and test one public and one private Supabase object flow.

## Related documentation

- [Project setup and environment variables](../README.md)
- [API reference](../docs/API_REFERENCE.md)
- [Architecture](../docs/ARCHITECTURE.md)
- [Supabase Storage migration](../docs/supabase-storage-migration.md)
