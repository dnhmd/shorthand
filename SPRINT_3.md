# Sprint 3 — System Resilience & Live Proof

## Objective

Defend the architecture against traffic spikes, enrich analytics data, expose metrics via REST, and make the platform production-ready and interactive — all without degrading core redirect performance.

---

## Architecture

Sprint 3 hardened every layer of the system:

- **GeoIP enrichment** — country resolution added to the analytics pipeline
- **Partitioned tables** — time-series optimized storage for click events
- **Analytics API** — seven REST endpoints exposing click metrics
- **Rate limiting** — Token Bucket algorithm backed by Redis with atomic Lua execution
- **Circuit breakers** — Resilience4j protecting the redirect flow from Kafka failures
- **Full Dockerization** — one-command deployment of the entire platform

---

## What Was Built

### Task 1 — GeoIP Enrichment

**Library:** MaxMind GeoLite2 (`geoip2` Java client)

**`GeoIpAdapter`** in `consumer/infrastructure/adapter/outbound/geoip`:
- Initialized at startup with a `DatabaseReader` pointed at `GeoLite2-Country.mmdb`
- Exposes `resolveCountry(String ipAddress)` — returns country name or `"Unknown"` on failure
- Private IPs and localhost resolve to `"Unknown"` — expected behavior

**`ProcessClickEventService`** updated:
- Calls `geoIpAdapter.resolveCountry()` during enrichment
- OS values containing `"??"` from YAUAA are normalized to `"Unknown"`
- `GeoLite2-Country.mmdb` stored in `utils/bin/geo-ip/` — git-ignored, mounted via Docker volume

---

### Task 2 — Analytics API

Seven REST endpoints on the consumer service (port 8081):

```
GET /api/v1/analytics/{code}/summary          → all metrics in one response
GET /api/v1/analytics/{code}/clicks/total     → total click count
GET /api/v1/analytics/{code}/clicks/by-date   → clicks grouped by day
GET /api/v1/analytics/{code}/clicks/by-country → clicks grouped by country
GET /api/v1/analytics/{code}/clicks/by-browser → clicks grouped by browser
GET /api/v1/analytics/{code}/clicks/by-os     → clicks grouped by OS
GET /api/v1/analytics/{code}/clicks/by-device → clicks grouped by device
```

**Domain models:**
- `ClickMetric(String label, Long count)` — generic grouped result
- `AnalyticsSummary` — composite of all six breakdowns

**JPA queries** use JPQL constructor expressions to map directly to `ClickMetric`. Date grouping uses `DATE_TRUNC('day', clicked_at)` via `FUNCTION()`.

**`ClickAnalyticsService`** implements `ClickAnalyticsUseCase` — delegates all queries to `ClickAnalyticsRepository`, assembles the summary response.

---

### Task 3 — Redis-Backed Token Bucket Rate Limiter

**`RateLimitFilter`** in `backend/infrastructure/adapter/inbound/web/filter`:
- `@Order(1)` — first filter in the chain
- Only applies to `POST /api/v1/links` — redirects are never rate limited
- Extracts real client IP via `X-Forwarded-For` header with fallback to `getRemoteAddr()`
- Executes a Lua script atomically on Redis

**Lua script** (`rate_limiter.lua`):
1. Reads `{tokens, last_updated}` from Redis hash per IP
2. Calculates elapsed time since last request
3. Replenishes tokens continuously — no burst-at-boundary exploit
4. Caps tokens at bucket capacity
5. Consumes one token if available — returns `1` (allowed) or `0` (rejected)
6. Sets TTL for automatic cleanup of inactive IPs

**Why Lua?** Redis executes Lua scripts atomically — no race conditions between read and write. Essential for correctness under concurrent traffic.

**Config:**
```yaml
shorthand:
  rate-limiter:
    capacity: 10
    refill-rate: 0.1   # tokens per second ≈ 6 per minute
    key-prefix: rate-limit
```

Returns `429 Too Many Requests` with a structured `ErrorResponse` on rejection.

---

### Task 4 — PostgreSQL Table Partitioning

`analytics.click_events` converted from a regular table to a **range-partitioned table** on `clicked_at`.

**Migration V2:**
- Drops the existing table
- Recreates as `PARTITION BY RANGE (clicked_at)`
- Composite primary key `(id, clicked_at)` — required by PostgreSQL for partitioned tables
- `clicked_at` is `NOT NULL` — required for partition key columns
- Monthly child partitions: `click_events_2026_06`, `click_events_2026_07`

**Why partition?** Analytics queries are always time-scoped. PostgreSQL's partition pruning skips irrelevant months entirely — a query for last week never touches previous months' data.

---

### Task 5 — Resilience4j Circuit Breaker

**`@CircuitBreaker`** on `publishMessage` in `LinkClickEventPublisherAdapter`:
- Instance name: `kafka-publisher`
- Fallback: logs warning and returns gracefully — redirect always completes
- `@Async` and `@CircuitBreaker` combined — async fires first, circuit breaker monitors inside the virtual thread

**Config:**
```yaml
resilience4j:
  circuit-breaker:
    instances:
      kafka-publisher:
        failure-rate-threshold: 50
        minimum-number-of-calls: 5
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
        sliding-window-size: 10
        sliding-window-type: COUNT_BASED
```

**Tested:** Kafka stopped mid-traffic — redirects continued uninterrupted, fallback logged `Kafka Circuit Breaker | Kafka Unavailable`. Kafka restarted — circuit recovered automatically.

---

### Task 6 — Dockerization & Documentation

**Dockerfiles** for `backend` and `consumer`:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`docker-compose.yml`** updated — full stack in one command:
- PostgreSQL, Redis, Kafka, Zookeeper — infrastructure
- `backend` — built from `./backend/Dockerfile`, port 8080
- `consumer` — built from `./consumer/Dockerfile`, port 8081
- All services on `shorthand-network`
- Environment variables passed for DB, Redis, Kafka, GeoIP path
- GeoIP database mounted as bind volume into consumer container

**Springdoc OpenAPI:**
- Auto-generated Swagger UI at `/swagger-ui/index.html`
- Available on both services
- Controllers annotated with `@Tag` and `@Operation`
- Backend: `http://localhost:8080/swagger-ui/index.html`
- Consumer: `http://127.0.0.1:8081/swagger-ui/index.html`

---

## Key Design Decisions

**Why Lua for rate limiting over Java-side Redis operations?**
Java-side operations require multiple round trips to Redis — read tokens, compute, write back. Between the read and write, another request could read the same stale value. Lua executes atomically on the Redis server — one round trip, no race condition.

**Why circuit breaker on the adapter, not the service?**
`@CircuitBreaker` is a Spring AOP annotation — a framework concern. The application service must stay framework-agnostic. The adapter is the correct boundary for infrastructure resilience patterns.

**Why partitioned tables before the Analytics API?**
Building the API on top of a non-partitioned table and migrating later would require rewriting queries or accepting performance debt. Partitioning first means the API was always built against the optimized schema.

**Why `127.0.0.1` instead of `localhost` for consumer in Docker/WSL2?**
WSL2 resolves `localhost` to IPv6 `::1` inconsistently. Docker's port binding listens on IPv4 `0.0.0.0`. Using `127.0.0.1` explicitly forces IPv4 and bypasses the WSL2 networking quirk.

---

## Infrastructure

Complete stack runs with `docker compose up`:
- PostgreSQL 16 — `public` schema (backend links), `analytics` schema (consumer click events)
- Redis 7.2 — Cache-Aside for redirects, Token Bucket for rate limiting
- Kafka + Zookeeper — async click event streaming
- Backend — Spring Boot 3.5, port 8080
- Consumer — Spring Boot 3.5, port 8081

---

## What's Next — Sprint 4

- **Dead Letter Queue** — replay failed Kafka events after outages
- **API Key authentication** — per-client rate limiting and access control
- **Link ownership** — user accounts, protected link management endpoints
- **Real-time dashboard** — WebSocket-based live click counter
- **Cloud deployment** — deploy to AWS/GCP with managed Kafka and RDS