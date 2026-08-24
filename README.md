# HireBean-Backend, Spring Backend & React & Typescript app + Devops

# HireBean-Frontend is here -> https://github.com/darimachine/HireBean-Frontend
## Форматиране
| Цел           | Команда                   |
| ------------- | ------------------------- |
| Форматиране   | `./gradlew spotlessApply` |
| Проверка (CI) | `./gradlew spotlessCheck` |

## Local backend checks

Проектът е настроен за Java 21 toolchain. Ако default Java на машината е по-нова, пусни Gradle с JDK 21:

```powershell
$env:JAVA_HOME='C:\Users\Az\.jdks\temurin-21.0.9'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat test
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## Local demo data

> Local/demo only. Never enable this seed in a shared, staging, or production environment.

Demo data is disabled by default. Enable it for a local run with:

```powershell
$env:APP_SEED_DEMO_DATA='true'
.\gradlew.bat bootRun
```

The seed is idempotent and preserves existing database records. It adds the `BluePeak Technologies` company, sample
jobs, posts, an application, bookmarks, notifications, and these demo accounts:

| Role      | Email                    | Password       |
|-----------|--------------------------|----------------|
| Admin     | `admin@hirebean.dev`     | `Admin123!`    |
| Employer  | `employer@hirebean.dev`  | `Employer123!` |
| Candidate | `candidate@hirebean.dev` | `Candidate123!` |

Disable the seed again with:

```powershell
Remove-Item Env:APP_SEED_DEMO_DATA
```

## Supabase Storage setup

The backend uses Supabase Storage instead of AWS S3. It uses the Supabase Storage REST API, so the backend keeps
object keys in PostgreSQL while images are public and CV files remain private behind short-lived signed URLs.

1. Create a free Supabase project at `https://supabase.com`.
2. Open Storage and create two file buckets:
   - `hirebean-public` and mark it **Public** for profile pictures, company logos, and post images.
   - `hirebean-private` and leave it **Private** for resumes and application CVs.
3. Open Project Settings -> API and copy the project URL and the server-only `service_role` key.
4. Put these values in the local, gitignored `.env` file:

```dotenv
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_SECRET_KEY=<server-only-secret-key>
SUPABASE_PUBLIC_BUCKET=hirebean-public
SUPABASE_PRIVATE_BUCKET=hirebean-private
SUPABASE_SIGNED_URL_SECONDS=600
```

Never expose `SUPABASE_SECRET_KEY` in the frontend or commit it. The backend maps folders as follows:
`profile-pictures`, `company-logos`, and `post-images` go to the public bucket; `resumes` and `cvs` go to the private
bucket. Existing database values remain object keys, so no PostgreSQL migration is required for this storage switch.

## API groups

- Auth: `/api/auth/register`, `/api/auth/login`, `/api/auth/logout`
- Users/profile: `/api/users`, `/api/users/{id}/profile`, `/api/users/{id}/profile-picture`, `/api/users/{id}/resume`
- Jobs: `/api/jobs`, `/api/jobs/{id}` with filters `search`, `location`, `minSalary`, `maxSalary`, `companyId`, `tags`
- Applications: `/api/applications/apply/{candidateId}`, `/api/applications/candidate/{candidateId}`, `/api/applications/job/{jobOfferId}`, `/api/applications/{applicationId}/status`
- Companies/posts: `/api/companies`, `/api/posts`
- Bookmarks: `/api/bookmarks/user/{userId}`, `/api/bookmarks/user/{userId}/job/{jobOfferId}`
- Notifications: `/api/notifications/user/{userId}`, `/api/notifications/user/{userId}/unread-count`, `/api/notifications/user/{userId}/mark-all-read`
- AI assistant: `/api/ai/prompt`
- Admin logs: `/api/admin/logs`
