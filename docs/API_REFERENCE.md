# HireBean API Reference

This document describes the 48 application endpoints implemented by the backend controllers. Runtime documentation is
also available through Swagger UI, but this file records authorization and workflow details that are not expressed by
the generated OpenAPI document.

## Base URL and formats

Local base URL:

```text
http://localhost:8080
```

JSON is used unless an endpoint explicitly accepts `multipart/form-data`. Date-time values use ISO-8601 local date-time
syntax, for example `2026-09-15T10:30:00`. IDs are positive integers.

## Authentication and authorization

Register or log in to receive an `AuthResponse.token`, then send it on protected requests:

```http
Authorization: Bearer <token>
```

The backend is stateless. Tokens expire after one hour by default. Logging out revokes the submitted token, so it can no
longer authenticate requests.

| Access label | Meaning |
|---|---|
| Public | No token is required. Supplying a valid token may affect job visibility. |
| Authenticated | Any authenticated `CANDIDATE`, `EMPLOYER`, or `ADMIN`. |
| Self or admin | The path user ID must identify the current user, unless the caller is an admin. |
| Candidate self or admin | The current candidate's ID must match the path ID, unless the caller is an admin. |
| Company owner or admin | The caller must be an employer attached to the relevant company, unless the caller is an admin. |
| Admin | The caller must have the `ADMIN` role. |

Roles:

- `CANDIDATE` manages their profile, applications, bookmarks, and notifications.
- `EMPLOYER` manages their company, its jobs and posts, and applications submitted to those jobs.
- `ADMIN` can perform administrative operations and bypasses ownership checks implemented by the backend.

## Pagination and sorting

Endpoints returning `Page<T>` accept Spring-style query parameters:

| Parameter | Example | Meaning |
|---|---|---|
| `page` | `0` | Zero-based page index. |
| `size` | `20` | Requested page size. |
| `sort` | `createdAt,desc` | Property and direction. Repeat the parameter for multiple sort fields. |

A page response contains the result array in `content` plus page metadata such as `number`, `size`, `totalElements`,
`totalPages`, `first`, and `last`.

## Endpoints

### Authentication — 3 endpoints

| Method and path | Access | Input | Success |
|---|---|---|---|
| `POST /api/auth/register` | Public | JSON `RegisterRequest` | `201 Created` — `AuthResponse` |
| `POST /api/auth/login` | Public | JSON `LoginRequest` | `200 OK` — `AuthResponse` |
| `POST /api/auth/logout` | Authenticated | `Authorization` header | `204 No Content` |

### Users and profiles — 10 endpoints

| Method and path | Access | Input | Success |
|---|---|---|---|
| `GET /api/users` | Admin | Optional `search`, `role`; pagination and sorting | `200 OK` — `Page<UserResponse>` |
| `GET /api/users/{id}` | Self or admin | Path `id` | `200 OK` — `UserResponse` |
| `GET /api/users/{id}/profile` | Self or admin | Path `id` | `200 OK` — `UserProfileResponse` |
| `PATCH /api/users/{id}/profile` | Self or admin | Path `id`; JSON `UpdateProfileRequest` | `200 OK` — `UserProfileResponse` |
| `PATCH /api/users/{id}/profile-picture` | Self or admin | Path `id`; multipart part `file` | `200 OK` — `UserProfileResponse` |
| `PATCH /api/users/{id}/resume` | Self or admin | Path `id`; multipart part `file` | `200 OK` — `UserProfileResponse` |
| `POST /api/users/password/reset-request` | Public | Required query `email` | `200 OK` — empty body |
| `GET /api/users/password/reset-confirm` | Public | Required query `token` | `302 Found` — redirects to `{APP_FRONTEND_URL}/reset-password?token=...` |
| `POST /api/users/password/reset` | Public | Required query `token`; JSON `ChangePasswordRequest` | `200 OK` — empty body |
| `DELETE /api/users/{id}` | Admin | Path `id` | `204 No Content` |

The reset-request endpoint sends an email containing a backend confirmation URL. The confirmation endpoint does not
change the password; it redirects the browser to the frontend form. The final reset endpoint validates the token and
changes the password.

### Companies — 5 endpoints

| Method and path | Access | Input | Success |
|---|---|---|---|
| `GET /api/companies` | Public | Optional `search`; pagination and sorting | `200 OK` — `Page<CompanyResponse>` |
| `GET /api/companies/{id}` | Public | Path `id` | `200 OK` — `CompanyResponse` |
| `POST /api/companies` | Employer without a company, or admin | JSON `CompanyRequest` | `201 Created` — `CompanyResponse` |
| `PUT /api/companies/{id}` | Company owner or admin | Path `id`; JSON `CompanyRequest` | `200 OK` — `CompanyResponse` |
| `DELETE /api/companies/{id}` | Company owner or admin | Path `id` | `204 No Content` |

An employer can create a company only when they are not already attached to one. On creation, the service associates
the new company with the eligible employer.

### Job offers — 5 endpoints

| Method and path | Access | Input | Success |
|---|---|---|---|
| `GET /api/jobs` | Public | Optional filters `search`, `location`, `minSalary`, `maxSalary`, `companyId`, `tags`, `jobStatus`; pagination and sorting | `200 OK` — `Page<JobOfferResponse>` |
| `GET /api/jobs/{id}` | Public | Path `id` | `200 OK` — `JobOfferResponse` |
| `POST /api/jobs` | Company owner or admin | JSON `JobOfferRequest` | `201 Created` — `JobOfferResponse` |
| `PUT /api/jobs/{id}` | Company owner or admin | Path `id`; JSON `JobOfferRequest` | `200 OK` — `JobOfferResponse` |
| `DELETE /api/jobs/{id}` | Company owner or admin | Path `id` | `204 No Content` |

Anonymous users and candidates see public active jobs. Employers can also see non-public jobs for their managed company;
admins have full status visibility. When updating a job, a non-admin employer must own both the existing job and the
company specified by the request.

Multiple tags may be passed using repeated query values, for example `?tags=Java&tags=Spring`.

### Job applications — 8 endpoints

| Method and path | Access | Input | Success |
|---|---|---|---|
| `POST /api/applications/apply/{candidateId}` | Candidate self or admin | Path `candidateId`; multipart JSON part `data` (`JobApplicationRequest`) and required file part `cv` | `200 OK` — `JobApplicationResponse` |
| `GET /api/applications/candidate/{candidateId}` | Self or admin | Path `candidateId` | `200 OK` — `List<JobApplicationResponse>` |
| `GET /api/applications/candidate/{candidateId}/job/{jobOfferId}/exists` | Candidate self or admin | Path `candidateId`, `jobOfferId` | `200 OK` — boolean |
| `GET /api/applications/job/{jobOfferId}` | Company owner or admin | Path `jobOfferId` | `200 OK` — `List<JobApplicationResponse>` |
| `PATCH /api/applications/{applicationId}/status` | Company owner or admin | Path `applicationId`; required query `status` | `200 OK` — `JobApplicationResponse` |
| `PATCH /api/applications/{applicationId}/review` | Company owner or admin | Path `applicationId`; JSON `ReviewApplicationRequest` | `200 OK` — `JobApplicationResponse` |
| `PUT /api/applications/{applicationId}/interview` | Company owner or admin | Path `applicationId`; JSON `InterviewInvitationRequest` | `200 OK` — `JobApplicationResponse` |
| `DELETE /api/applications/{applicationId}/interview` | Company owner or admin | Path `applicationId` | `200 OK` — `JobApplicationResponse` |

The multipart CV file is authoritative when applying. Although `JobApplicationRequest` currently exposes an optional
`cvUrl` property, the application service stores the uploaded `cv` file instead.

### Company posts — 5 endpoints

| Method and path | Access | Input | Success |
|---|---|---|---|
| `GET /api/posts` | Public | Optional `companyId`; pagination and sorting | `200 OK` — `Page<PostResponse>` |
| `GET /api/posts/{id}` | Public | Path `id` | `200 OK` — `PostResponse` |
| `POST /api/posts` | Company owner or admin | JSON `PostRequest` | `201 Created` — `PostResponse` |
| `PUT /api/posts/{id}` | Company owner or admin | Path `id`; JSON `PostRequest` | `200 OK` — `PostResponse` |
| `DELETE /api/posts/{id}` | Company owner or admin | Path `id` | `204 No Content` |

For a non-admin employer, `companyId` must identify their company. If `authorId` is supplied, it must identify that
employer. An admin may manage any post.

### Bookmarks — 3 endpoints

| Method and path | Access | Input | Success |
|---|---|---|---|
| `GET /api/bookmarks/user/{userId}` | Self or admin | Path `userId` | `200 OK` — `List<JobOfferResponse>` |
| `POST /api/bookmarks/user/{userId}/job/{jobOfferId}` | Self or admin | Path `userId`, `jobOfferId` | `200 OK` — empty body |
| `DELETE /api/bookmarks/user/{userId}/job/{jobOfferId}` | Self or admin | Path `userId`, `jobOfferId` | `204 No Content` |

### Notifications — 4 endpoints

| Method and path | Access | Input | Success |
|---|---|---|---|
| `GET /api/notifications/user/{userId}` | Self or admin | Path `userId` | `200 OK` — `List<NotificationResponse>` |
| `GET /api/notifications/user/{userId}/unread-count` | Self or admin | Path `userId` | `200 OK` — integer count |
| `PATCH /api/notifications/user/{userId}/mark-all-read` | Self or admin | Path `userId` | `204 No Content` |
| `PATCH /api/notifications/{notificationId}/mark-read` | Recipient or admin | Path `notificationId` | `200 OK` — `NotificationResponse` |

### Files — 1 endpoint

| Method and path | Access | Input | Success |
|---|---|---|---|
| `POST /api/files/upload` | Authenticated | Multipart parts `file` and `folder` | `200 OK` — object containing `key` and, for public folders, `publicUrl` |

Supported folder conventions:

| Folder | Bucket | Returned access |
|---|---|---|
| `profile-pictures` | Public | Permanent public URL |
| `company-logos` | Public | Permanent public URL |
| `post-images` | Public | Permanent public URL |
| `resumes` | Private | Stored key; profile responses produce a short-lived signed URL |
| `cvs` | Private | Stored key; application responses produce a short-lived signed URL |

Unknown folder names are currently routed to the private bucket. Folder names containing `/` or `..` are rejected.
Uploads are limited to 10 MB.

### Career assistant — 1 endpoint

| Method and path | Access | Input | Success |
|---|---|---|---|
| `POST /api/ai/prompt` | Authenticated | JSON `AiPromptRequest` | `200 OK` — `AiPromptResponse` |

Despite the route name, the current service does not call an external AI provider. It generates a deterministic local
template and returns `provider: "LOCAL_TEMPLATE"`.

### Admin audit logs — 3 endpoints

| Method and path | Access | Input | Success |
|---|---|---|---|
| `GET /api/admin/logs` | Admin | Optional `actorId`, `action`, `entity`, `severity`, ISO date-time `from`, `to`; pagination and sorting | `200 OK` — `Page<LogResponse>` |
| `GET /api/admin/logs/{id}` | Admin | Path `id` | `200 OK` — `LogResponse` |
| `DELETE /api/admin/logs/{id}` | Admin | Path `id` | `204 No Content` |

## Request models

Fields marked **required** are validated by the controller.

| Model | Fields |
|---|---|
| `RegisterRequest` | **`email`** (valid email), **`password`** (minimum 8 characters), **`firstName`**, **`lastName`**, optional `role` (defaults to `CANDIDATE`), optional `companyId` |
| `LoginRequest` | **`email`** (valid email), **`password`** |
| `ChangePasswordRequest` | **`newPassword`** (minimum 8 characters), **`confirmPassword`** |
| `UpdateProfileRequest` | Optional `firstName`, `lastName`, `bio`, `linkedInUrl`, `githubUrl`, `jobTitle`, `profilePicture` |
| `CompanyRequest` | **`name`**, optional `description`, `websiteUrl`, `logoUrl`, `location` |
| `JobOfferRequest` | **`title`**, **`description`**, **`jobType`**, **`companyId`**; optional `location`, `minSalary`, `maxSalary`, `status`, `tags` |
| `JobApplicationRequest` | **`jobOfferId`**, optional `coverLetter`, optional legacy `cvUrl`; sent as the multipart `data` part |
| `ReviewApplicationRequest` | **`status`**, optional `feedbackMessage` (maximum 4000 characters) |
| `InterviewInvitationRequest` | **`interviewAt`** (future date-time), optional `message` (maximum 2000 characters) |
| `PostRequest` | **`title`**, **`content`**, **`companyId`**; optional `authorId`, `imageUrl` |
| `AiPromptRequest` | **`prompt`** (maximum 4000 characters), optional `purpose` |

`JobOfferFilterRequest` is populated from the job-list query string rather than a JSON body. Its fields are `search`,
`location`, `minSalary`, `maxSalary`, `companyId`, `tags`, and `jobStatus`.

## Response models

| Model | Fields |
|---|---|
| `AuthResponse` | `userId`, `token`, `email`, `firstName`, `lastName`, `role`, `companyId` |
| `UserResponse` | `id`, `email`, `firstName`, `lastName`, `role`, `companyId` |
| `UserProfileResponse` | `id`, `email`, `firstName`, `lastName`, `bio`, `linkedinUrl`, `githubUrl`, `jobTitle`, `resumeUrl`, `profilePictureUrl` |
| `CompanyResponse` | `id`, `name`, `description`, `websiteUrl`, `logoUrl`, `location`, `createdAt` |
| `JobOfferResponse` | `id`, `title`, `description`, `location`, `jobType`, `minSalary`, `maxSalary`, `status`, `createdAt`, `companyId`, `companyName`, `companyLogoUrl`, `tags` |
| `JobApplicationResponse` | `id`, `candidateId`, `candidateEmail`, `jobOfferId`, `jobTitle`, `coverLetter`, `cvUrl`, `status`, `feedbackMessage`, `interviewAt`, `interviewMessage`, `createdAt` |
| `PostResponse` | `id`, `title`, `content`, `imageUrl`, `companyId`, `companyName`, `authorId`, `authorEmail`, `createdAt`, `updatedAt` |
| `NotificationResponse` | `id`, `recipientId`, `message`, `read`, `type`, `createdAt` |
| `AiPromptResponse` | `prompt`, `response`, `provider`, `createdAt` |
| `LogResponse` | `id`, `action`, `entity`, `entityId`, `actorId`, `actorEmail`, `details`, `severity`, `timestamp` |

Private `resumeUrl` and `cvUrl` values are signed Supabase URLs and normally expire after 600 seconds. Clients must not
store them as permanent file identifiers.

## Enums

| Enum | Accepted values |
|---|---|
| `RoleType` | `CANDIDATE`, `EMPLOYER`, `ADMIN` |
| `JobType` | `FULL_TIME`, `PART_TIME`, `CONTRACT`, `INTERN`, `FREELANCE` |
| `JobStatus` | `ACTIVE`, `ARCHIVED`, `DRAFT` |
| `ApplicationStatus` | `PENDING`, `REVIEWED`, `ACCEPTED`, `REJECTED` |
| `LogSeverity` | `INFO`, `WARN`, `ERROR` |

Enum values are case-sensitive.

## Error responses

Errors use this JSON envelope:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters.",
  "path": "/api/example",
  "timestamp": "2026-08-27 15:30:00"
}
```

Common statuses:

| Status | Meaning |
|---|---|
| `400 Bad Request` | Invalid parameters, malformed JSON, validation failure, invalid file/folder, or invalid operation. |
| `401 Unauthorized` | Authentication is missing, invalid, expired, or revoked. |
| `403 Forbidden` | The caller is authenticated but lacks the required role or ownership. |
| `404 Not Found` | The requested entity does not exist. |
| `409 Conflict` | A unique or workflow conflict exists, such as a duplicate application or company. |
| `502 Bad Gateway` | A Supabase upload or signed-link request failed. |
| `500 Internal Server Error` | An unexpected backend error occurred. |

## Multipart examples

Apply to a job:

```bash
curl -X POST "http://localhost:8080/api/applications/apply/3" \
  -H "Authorization: Bearer <token>" \
  -F 'data={"jobOfferId":1,"coverLetter":"I am interested in this role."};type=application/json' \
  -F 'cv=@resume.pdf;type=application/pdf'
```

Upload a profile picture:

```bash
curl -X PATCH "http://localhost:8080/api/users/3/profile-picture" \
  -H "Authorization: Bearer <token>" \
  -F 'file=@avatar.png;type=image/png'
```

Upload a generic public asset:

```bash
curl -X POST "http://localhost:8080/api/files/upload" \
  -H "Authorization: Bearer <token>" \
  -F 'file=@company-logo.png;type=image/png' \
  -F 'folder=company-logos'
```

## Operational endpoints

These endpoints are provided by Spring Boot infrastructure and are not included in the 48 application endpoints:

| Path | Access | Purpose |
|---|---|---|
| `/actuator/health` | Public | Application and dependency health. |
| `/actuator/info` | Public | Application information. |
| `/actuator/metrics` | Authenticated | Metrics root; it is exposed but not publicly permitted. |
| `/swagger-ui/index.html` | Public | Interactive generated API documentation. |
| `/v3/api-docs` | Public | Generated OpenAPI JSON. |

Swagger is generated from the running application. The project currently has no committed OpenAPI document or bearer
security scheme customization, so use this reference for authorization details.

## Related documentation

- [Project setup](../README.md)
- [Architecture](ARCHITECTURE.md)
- [Supabase Storage migration](supabase-storage-migration.md)
