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
