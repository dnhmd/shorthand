# System Resilience & Production Readiness

## Objective

The primary objective of this Sprint was to secure the architecture against erratic traffic spikes, enrich telemetry data, expose analytical metrics via a REST layer, and establish full production deployment readiness, all while keeping the core URL redirection flow completely unburdened.

---

## Architecture

Sprint 3 focused on hardening every layer of the platform through targeted infrastructure and security patterns:

- **GeoIP Enrichment:** Real-time country resolution was integrated directly into the ingestion worker pipeline.
- **Database Partitioning:** PostgreSQL tables were restructured into time-series optimized range partitions for sustainable data growth.
- **Analytics API:** Seven dedicated REST endpoints were exposed to provide granular and composite redirection metrics.
- **Distributed Rate Limiting:** A Token Bucket algorithm filter was built on top of Redis, utilizing atomic Lua scripting to prevent abuse.
- **Circuit Breaker Pattern:** Resilience4j was introduced to insulate core routing logic from potential downstream Kafka anomalies.
- **Container-First Deployment:** Docker configurations were introduced to establish an orchestrated, single-command runtime ecosystem.

---

## What Was Built

### 1. GeoIP Enrichment

To add geographic context to incoming telemetry without incurring performance overhead, the MaxMind GeoLite2 (`geoip2` Java client) was integrated into the consumer microservice.

- **Implementation Detail:** The infrastructure layer features a `GeoIpAdapter` initialized at application startup with a `DatabaseReader` targeting `GeoLite2-Country.mmdb`.
- **Execution Flow:** The adapter exposes a `resolveCountry(String ipAddress)` method that maps IPs to country names. It safely treats private IP ranges, localhost loops, and lookup anomalies as `"Unknown"`.
- **Data Processing:** During event ingestion, the `ProcessClickEventService` invokes this adapter alongside the User-Agent analyzer. Unresolved operating system values containing `"??"` are normalized to `"Unknown"` to maintain data cleanliness.

---

### 2. Analytical Metric Discovery

The consumer microservice (operating on port 8081) was expanded to host seven dedicated REST endpoints for processing user metadata:

```
GET /api/v1/analytics/{code}/summary          → Complete metric summary in a single payload
GET /api/v1/analytics/{code}/clicks/total     → Aggregate redirection volume
GET /api/v1/analytics/{code}/clicks/by-date   → Redirections grouped by calendar date
GET /api/v1/analytics/{code}/clicks/by-country → Redirections grouped by geographic country
GET /api/v1/analytics/{code}/clicks/by-browser → Redirections grouped by browser type
GET /api/v1/analytics/{code}/clicks/by-os     → Redirections grouped by operating system
GET /api/v1/analytics/{code}/clicks/by-device → Redirections grouped by hardware profile
```

- **Data Modeling:**
Domain models are structured around a generic `ClickMetric(String label, Long count)` representation, alongside an `AnalyticsSummary` composition model.

**Database Ingestion:**
JPA queries leverage JPQL constructor expressions to project database rows directly into domain models. Date truncation operations are handled natively via the database's `DATE_TRUNC` function executed through JPQL `FUNCTION()`.

**Service Responsibility:**
The `ClickAnalyticsService` implements the primary interaction logic, delegating queries to the persistent layer and mapping the unified summary responses.

---

### 3. Redis-Backed Token Bucket Rate Limiter

To shield the application from script-driven creation abuse, an automated `RateLimitFilter` was positioned at the front of the request chain.

- **Filter Placement:** Configured with a priority rank of `@Order(1)`, the filter targets `POST /api/v1/links` exclusively. User redirection endpoints (`GET /{code}`) remain completely bypassable to maximize speed.
- **Client Identification:** Client IPs are extracted through the `X-Forwarded-For` header, using `getRemoteAddr()` as an immediate fallback.
- **Atomic Scripting:** The filter runs an underlying Lua script (`rate_limiter.lua`) directly within Redis:
  1. Retrieves `{tokens, last_updated}` out of a dedicated hash key.
  2. Measures the duration since the previous invocation.
  3. Continuous mathematical replenishment occurs on-the-fly, preventing boundary-burst manipulation.
  4. Bounds tokens to maximum bucket limits.
  5. Deducts a single token when available and returns an indicator (`1` for approval, `0` for rejection).
  6. Apples a sliding TTL to clean up stagnant IPs automatically.
```yaml
shorthand:
  rate-limiter:
    capacity: 10
    refill-rate: 0.1   # Grants 1 token every 10 seconds (~6 tokens per minute)
    key-prefix: rate-limit
```

_Requests breaking the configured capacity threshold are immediately rejected with a `429 Too Many Requests` response wrapper._

---

### 4. PostgreSQL Table Partitioning

To maintain responsive query timelines as analytical volume expands, `analytics.click_events` was converted from a standard table to a range-partitioned database layout structured around the `clicked_at` timestamp.
- **Schema Inversion:** Migration scripts safely recreate the core table with an explicit `PARTITION BY RANGE (clicked_at)` directive.
- **Constraints:** Primary keys are adjusted to use a composite pairing of (`id, clicked_at`), with the timestamp strictly marked as `NOT NULL` to comply with PostgreSQL's partitioning rules.
- **Time Windows:** Monthly physical storage boundaries are generated upfront (e.g., `click_events_2026_06`, `click_events_2026_07`).

---

### 5. Fault Isolation via Circuit Breakers

To prevent slow or crashing messaging brokers from introducing latency or taking down the critical routing application, the `@CircuitBreaker` pattern was integrated over the messaging infrastructure.
- **Boundary Separation:** The resilience layer guards `LinkClickEventPublisherAdapter.publishMessage()`. If an exception occurs, a generic fallback captures the failure, fires a warning log (`Kafka Circuit Breaker | Kafka Unavailable`), and exits gracefully.
- **Execution Priority:** Because `@Async` activates before the circuit breaker interceptor, fault checking is containerized safely inside the allocated virtual thread, allowing the user's redirection to resolve cleanly.
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

---

### 6. Container Infrastructure & OpenAPI Documentation

The entire microservice matrix can now be initialized cleanly using a centralized `docker-compose.yml` file.
- **Optimized Image Footprint:** Lean, multi-platform Docker files isolate applications inside minimal alpine environments:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- **Orchestration Matrix:** The single-command setup coordinates PostgreSQL 16 (mapping segregated schemas), Redis 7.2, Kafka, Zookeeper, the backend core, and the analytical consumer over an isolated network (`shorthand-network`). The GeoIP binaries are linked to the consumer container through an isolated bind volume mount.
- **Interactive Documentation:** Springdoc OpenAPI auto-generates interactive API playgrounds accessible via the standard Swagger UI path (`/swagger-ui/index.html`) on both operational ports (`8080` for backend routing, `8081` for analytics exploration).

---

## Key Design Decisions

- **Atomic Scripting Over Application Computations:** Calculating rate limits inside the Java application layer introduces race conditions under high concurrent volume due to multiple Redis roundtrips (read, update, verify). Bundling logic into a Lua script guarantees atomicity inside Redis, making the evaluation process thread-safe and limited to a single network call.
- **Placing Circuit Breakers on Adapters:** Applying resilience annotations to core application services introduces framework dependencies into pure business logic. Isolating `@CircuitBreaker` strictly within the messaging adapters keeps core interfaces completely decoupled from framework runtime behaviors.
- **Partitioning Before API Exposure:** Implementing range-partitioned tables prior to releasing the analytics API prevents the need for database refactoring down the line. It ensures index layouts and query executions are built to utilize PostgreSQL's built-in partition pruning right from the start.
- **Explicit IPv4 Loopback Over Localhost Naming:** In containerized development environments—particularly when using WSL2—the term `localhost` can evaluate inconsistently between IPv4 loops and IPv6 `::1` structures. Explicitly binding to `127.0.0.1` ensures reliable networking across internal network layers.

---

## Infrastructure Summary

The runtime matrix consists of the following components running under a shared container network:
- **PostgreSQL 16:** Houses transactional URL tables inside the `public` schema and analytical logs inside the `analytics` schema.
- **Redis 7.2:** Powers high-speed data caching and tracks rate-limiting tokens.
- **Kafka + Zookeeper:** Operates as the core high-throughput message bus.

---

## Next Steps

- **Dead Letter Queue (DLQ):** Construct a reliable Kafka retry pipeline to capture, store, and replay failed messages following systemic broker outages.
- **API Key Management:** Introduce client authentication scopes to provide personalized rate limits and access credentials.
- **Link Ownership Foundations:** Build secure relational mappings linking short codes to authentic user accounts and administrative interfaces.
- **Real-Time Data Streaming:** Introduce WebSocket bridges into the analytics layer to stream live metric counters straight to front-end clients.
- **Cloud Architecture Blueprinting:** Prepare deployment definitions for automated continuous integration to managed cloud services.
