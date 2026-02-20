# hiddenlonerbackend

Spring Boot backend for hidden loner project.

## Requirements

- Java 21+

## Environment

Set environment values before running.

Required values:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JPA_DDL_AUTO` (`validate` recommended in production)
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY` (required for private storage upload in production)
- `SUPABASE_STORAGE_BUCKET` (e.g. `mission-images`)
- `SUPABASE_USERNAME_EMAIL_DOMAIN` (optional)
- `APP_ADMIN_USERNAMES` (e.g. `master`)
- `APP_DEV_BOOTSTRAP_ENABLED` (`false` in production)
- `APP_SECURITY_PRODUCTION_MODE` (`true` in production, enables startup validation)
- `APP_SECURITY_EXPOSE_DETAILED_ERRORS` (`false` in production)
- `APP_CORS_ALLOWED_ORIGINS` (comma-separated frontend origins, required in production mode)
- `PORT` (optional, default 8080)

## Production security baseline

- Set `APP_SECURITY_PRODUCTION_MODE=true`
- Set `APP_DEV_BOOTSTRAP_ENABLED=false`
- Set `APP_SECURITY_EXPOSE_DETAILED_ERRORS=false`
- Configure `APP_CORS_ALLOWED_ORIGINS` to exact frontend domain(s) only
- Keep `SUPABASE_STORAGE_BUCKET` private and use `SUPABASE_SERVICE_ROLE_KEY` only on backend
- Rotate all keys/passwords before deployment if they were exposed in local chat/history

## Run

```bash
cd /Users/gimminhyeong/Downloads/hiddenlonerbackend
./gradlew bootRun
```

## APIs

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me` (Bearer access token)

### Mission / Post

- `GET /api/missions`
- `GET /api/missions/{missionId}`
- `GET /api/missions/{missionId}/posts`
- `POST /api/missions/{missionId}/posts` (Bearer access token)
- `GET /api/posts/{postId}`
- `POST /api/posts/{postId}/likes` (Bearer access token)
- `POST /api/posts/{postId}/comments` (Bearer access token)
- `DELETE /api/posts/{postId}/comments/{commentId}` (Bearer access token, own comment only)

### Admin

- `GET /api/admin/posts` (Bearer access token, admin only)
- `POST /api/admin/posts/{postId}/approve` (Bearer access token, admin only)

### Dev

- `POST /api/dev/test-accounts/bootstrap` (enabled only when `APP_DEV_BOOTSTRAP_ENABLED=true`)

Bootstraps test users:

- `user1` / `1234`
- `user2` / `1234`
- `master` / `1234`

## Health check

```bash
curl http://localhost:8080/api/health
```
