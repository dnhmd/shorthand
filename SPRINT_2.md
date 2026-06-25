# Sprint 2 — The Event-Driven Analytics Pipeline

## Objective

Process high volumes of click-stream metadata without degrading core redirect performance. Every link redirection fires an analytics event asynchronously — the HTTP 302 response is never blocked by analytics concerns.

---

## Architecture

### The Core Principle — Async Isolation

The redirect flow is sacred. Sub-millisecond response times cannot be sacrificed for analytics. Sprint 2 introduces a clean separation between the **critical path** (redirect) and the **analytics path** (event processing):

```
Client → GET /{code} → 302 (immediate)
                    ↓ (async, non-blocking)
              LinkClickEvent → Kafka → Consumer → analytics.click_events
```

### Multi-Module Maven Structure

The project was restructured into a proper multi-module Maven build:

```
shorthand/
  pom.xml          ← parent POM (Spring Boot BOM, shared versions)
  common/          ← shared event schema
  backend/         ← core routing service
  consumer/        ← analytics consumer microservice
```

The parent POM manages all dependency versions via `<dependencyManagement>`. Child modules declare dependencies without versions.

### The Event Contract — `common` Module

`LinkClickEvent` is defined once in `common` and shared by both services. This enforces a single source of truth for the Kafka message schema — if either service drifts, compilation fails.

```java
public record LinkClickEvent(
    String code,
    String ipAddress,
    String userAgent,
    Instant clickedAt
) {}
```

Raw data only. Enrichment happens in the consumer, not the backend.

---

## What Was Built

### Task 1 — Asynchronous Isolation

**`AsyncConfig`** in `backend/infrastructure/config`:
- `@EnableAsync` activates Spring's async execution framework
- Backed by `Executors.newVirtualThreadPerTaskExecutor()` — Java 21 virtual threads
- Virtual threads are cheap enough to create per task — no pool tuning, no bottleneck

**`@Async` on `LinkClickEventPublisherAdapter.publishMessage()`**:
- The adapter method fires on a virtual thread
- The request thread returns the 302 immediately
- Analytics publishing is transparent to the application layer

### Task 2 — Kafka Pipeline

**`LinkClickEventPublisherPort`** in `backend/domain/port/outbound`:
```java
void publishMessage(LinkClickEvent event);
```

**`LinkClickEventPublisherAdapter`** in `backend/infrastructure/adapter/outbound/messaging`:
- Serializes `LinkClickEvent` to JSON via Jackson
- Publishes to `link-click-events` Kafka topic with `code` as the message key
- Uses `CompletableFuture` for non-blocking send confirmation
- Topic name externalized via `ShorthandProperties.KafkaProperties`

**`RedirectController` updated**:
- Extracts real client IP via `X-Forwarded-For` header with fallback to `getRemoteAddr()`
- Extracts `User-Agent` with `"Unknown"` fallback
- Passes both to use case — domain layer builds and publishes the event

### Task 3 — The Consumer Microservice

A standalone Spring Boot application running on port 8081 with its own:
- Flyway migrations managing the `analytics` schema
- JPA persistence layer
- Kafka consumer group `shorthand-consumer`

**Domain:**
- `ClickEvent` — enriched domain record (no `id` — database-generated)
- `ProcessClickEventUseCase` — inbound port
- `ClickEventRepository` — outbound port

**Application:**
- `ProcessClickEventService` — enriches raw `LinkClickEvent` using YAUAA, builds `ClickEvent`, persists

**Infrastructure:**
- `LinkClickEventConsumer` — `@KafkaListener` on `link-click-events` topic, deserializes JSON, delegates to use case
- `ClickEventRepositoryAdapter` — JPA persistence to `analytics.click_events`
- `ClickEventEntity` — JPA entity with `@Table(schema = "analytics")`
- `ApplicationConfig` — manual wiring of `ProcessClickEventService` and `UserAgentAnalyzer`

**YAUAA enrichment:**
- `DeviceName` → `device` (Desktop/Mobile/Tablet)
- `OperatingSystemNameVersion` → `os` (Windows NT, iOS, Android etc.)
- `AgentNameVersion` → `browser` (Chrome 149, Safari 17 etc.)
- `country` → `null` placeholder for Sprint 3 GeoIP enrichment

---

## Key Design Decisions

**Why virtual threads over a fixed thread pool?**
Event publishing is I/O-bound — it waits on Kafka network calls. Virtual threads are purpose-built for I/O-bound concurrency. No pool size tuning, no thread starvation under load.

**Why enrich in the consumer, not the backend?**
GeoIP lookups and UA parsing are expensive operations. Doing them synchronously in the backend — even before the async publish — would add latency to the redirect. The consumer processes events independently, keeping the backend lean.

**Why pass `ipAddress` and `userAgent` through the use case signature?**
Filters store data in `ThreadLocal` which doesn't transfer across threads. Since `@Async` fires on a different thread, `ThreadLocal` data would be lost. Explicit method parameters are safe and clear.

**Why `common` module over duplicating `LinkClickEvent`?**
Independent definitions in both services can drift silently — the backend serializes a field the consumer doesn't know about and deserialization fails at runtime with no compile-time warning. A shared module makes schema drift a compilation error.

---

## Infrastructure

All services run on a shared Docker network (`shorthand-network`):
- PostgreSQL 16 — `public` schema (backend), `analytics` schema (consumer)
- Redis 7.2 — cache layer for backend
- Kafka + Zookeeper — event streaming backbone
- Flyway manages both schemas independently

---

## What's Next — Sprint 3: System Resilience & Live Proof

**Task 1 — GeoIP Enrichment**
Resolve `country` from `ipAddress` using MaxMind GeoLite2. The `country` column in `analytics.click_events` is already a placeholder waiting for this.

**Task 2 — Analytics API**
Expose click metrics via REST endpoints on the consumer service — total clicks per link, clicks by country, clicks by device, time-series click counts.

**Task 3 — Rate Limiting**
Build a custom filter implementing the Token Bucket Algorithm backed by Redis. Protect link creation endpoints from script-driven API abuse without relying on third-party rate limiting libraries.

**Task 4 — Partitioned Tables**
Optimize `analytics.click_events` for time-series query performance using PostgreSQL table partitioning by `clicked_at`. Queries scoped to a time range scan only the relevant partition.

**Task 5 — Fault Tolerance**
Integrate Resilience4j Circuit Breakers. If Kafka or the consumer drops offline, the core routing service must degrade gracefully — continuing to handle redirections without throwing unhandled exceptions or risking memory leaks.

**Task 6 — Dockerization & Documentation**
Write individual Dockerfiles for backend and consumer. Orchestrate everything via `docker-compose.yml` — one command brings up both services, PostgreSQL, Redis, and Kafka. Embed Springdoc OpenAPI (Swagger). Place the live API URL in the README so anyone can hit an endpoint from their browser.
