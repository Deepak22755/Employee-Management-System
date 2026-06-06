# Construction HRMS — Worker Attendance & Overtime Engine

## Which HRMS was forked and why
This project is built fresh on Spring Boot + JPA + PostgreSQL (inspired by the amigoscode spring-boot-fullstack-professional structure). The clean layered architecture (Controller → Service → Repository) maps directly to the assignment's requirements and is easy to extend.

## AI Tools Used
- **Claude (Anthropic)** — used for scaffolding entity relationships, reviewing `@Transactional` proxy trap edge cases, and drafting the Redis TTL strategy. All business logic, transaction design, and ticket fixes were reasoned through manually with AI suggestions reviewed and corrected.
- All code was reviewed line-by-line and the LF-20x ticket fixes especially were traced through the codebase manually to confirm root cause before applying fixes.

---

## Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.8+
- Redis (local: `docker run -d -p 6379:6379 redis` or a free cloud instance)
- Supabase account (free tier) — [supabase.com](https://supabase.com)

### 1. Supabase Setup

1. Create a new project at supabase.com
2. Go to **Project Settings → Database**
3. **IMPORTANT**: Use the **connection pooler URL** (port **6543**, PgBouncer), NOT the direct connection (port 5432). Direct connections exhaust under load.
   - Pooler URL looks like: `postgresql://postgres.[project-ref]:[password]@aws-0-ap-south-1.pooler.supabase.com:6543/postgres`
4. Run the migration SQL:
   ```
   psql "your-supabase-pooler-url" -f src/main/resources/db/migration/V1__init_schema.sql
   ```

### 2. Environment Variables

Copy and set these before running:

```bash
export SUPABASE_DB_URL=jdbc:postgresql://db.YOURPROJECT.supabase.co:6543/postgres?pgbouncer=true
export SUPABASE_DB_USER=postgres
export SUPABASE_DB_PASSWORD=your-password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=          # leave empty if no auth
export CORS_ALLOWED_ORIGINS=http://localhost:3000
export API_USER=admin
export API_PASSWORD=admin
```

### 3. Run

```bash
mvn clean install -DskipTests
mvn spring-boot:run

# For staging profile (Supabase-specific HikariCP tuning):
mvn spring-boot:run -Dspring-boot.run.profiles=staging
```

### 4. Verify
```bash
curl -u admin:admin http://localhost:8080/actuator/health
```

---

## API Endpoints

All endpoints require Basic Auth: `admin:admin` (configurable via env vars).

### Attendance
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/attendance/clock-in` | Clock in a worker |
| POST | `/api/attendance/clock-out` | Clock out a worker |
| GET | `/api/attendance/active` | Active workers (from Redis) |
| GET | `/api/attendance/log?workerId=1&from=2026-05-01&to=2026-05-31` | Paginated attendance history |

### Overtime
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/overtime/summary/{workerId}?month=2026-04` | Monthly overtime summary |
| POST | `/api/overtime/settle/{workerId}?month=2026-04` | Settle overtime (past months only) |

### Workers / Sites
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/workers` | Create worker |
| GET | `/api/workers/{id}` | Get worker |
| PUT | `/api/workers/{id}` | Update worker (invalidates cache) |
| DELETE | `/api/workers/{id}` | Deactivate worker |
| POST | `/api/sites` | Create site |
| GET | `/api/sites/{id}` | Get site |
| DELETE | `/api/sites/{id}` | Deactivate site |

---

## Architecture & Design Decisions

### Schema
- `attendance_logs` has a **partial unique index** on `(worker_id) WHERE clock_out IS NULL` — this enforces no-double-clock-in at the DB level, not just in Java.
- `overtime_entries` has a `UNIQUE` constraint on `attendance_id` — one overtime entry per shift, no duplicates.
- All timestamps use `TIMESTAMPTZ` (timezone-aware).

### Redis Caching Strategy
- Active workers stored in a Redis **hash** (`active_workers` key), not individual keys. Hash allows O(1) membership checks and efficient full-list retrieval for the `/active` endpoint.
- Each worker also gets a TTL sentinel key (`active_worker_ttl:{id}` expiring in 16 hours). On `GET /active`, stale entries (where TTL key expired but hash entry wasn't cleaned) are detected and removed automatically.
- Cache degradation: if Redis is completely down, the app continues — clock-in/out falls back to DB, `/active` returns empty list. No 500s.

### Overtime Calculation
- Standard shift: 8 hours. Overtime = hours beyond 8.
- Tier 1 (≤ 2 overtime hours): **1.5x** hourly wage
- Tier 2 (> 2 overtime hours): first 2 at 1.5x, remainder at **2x**
- Monthly cap: 60 hours. Cap enforced at clock-out by summing existing entries for the month.

### Ticket Fixes Summary

| Ticket | Root Cause | Fix |
|--------|-----------|-----|
| LF-201 | CORS blocked by Spring Security before controller | `CorsConfigurationSource` bean registered in Security filter chain; origins externalized to yml |
| LF-202 | Redis hard failure crashed app startup and mid-request | Short connect timeout (3s) + custom `CacheErrorHandler` catches all cache errors |
| LF-203 | No pagination + N+1 on Worker/Site joins | `Pageable` throughout, `@EntityGraph` forces JOIN FETCH in one query |
| LF-204 | Settlement loop committed per-entry; SMS fired mid-loop | `@Transactional` wraps entire settlement; bulk update replaces loop; SMS via `@TransactionalEventListener(AFTER_COMMIT)` |
| LF-205 | External API call inside `@Transactional` held DB connection | External call moved before transaction; HikariCP tuned for Supabase; `application-staging.yml` profile |


### Hand-Drawn Diagram
   
   ![# alt text](image.png)


### Things I'd Do Differently With More Time
1. Add Flyway for proper migration management (currently uses `ddl-auto: validate` with manual SQL)
2. Add JWT auth instead of Basic Auth
3. Add integration tests with Testcontainers (PostgreSQL + Redis in Docker)
4. Add a retry queue for failed SMS notifications
5. Consider using a message broker (RabbitMQ/Kafka) instead of Spring events for SMS — more resilient across restarts


---

## Enabling Hibernate SQL Logging (Verify LF-203 fix)

In `application.yml`, set:
```yaml
spring:
  jpa:
    show-sql: true
```

Then call `GET /api/attendance/log?workerId=1&from=2026-05-01&to=2026-05-31` and verify only **ONE** SQL query runs (with JOIN), not N queries per record.

