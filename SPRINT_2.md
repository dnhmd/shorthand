# The Event-Driven Analytics Pipeline

## Objective

Our primary focus for this sprint was capturing high-volume click-stream metadata without compromising the performance of our core redirection engine. To ensure the HTTP 302 Found response is never held up by telemetry concerns, every link redirection triggers an analytics event asynchronously, moving it completely out of the request-response lifecycle.

---

## Architecture & System Design

### 1. Asynchronous Isolation

The redirection flow is sacred. To guarantee sub-millisecond response times, we strictly decouple the critical path (the user's redirect) from the analytics path (event processing).

```
Client → GET /{code} → 302 Redirect (Immediate Response)
                    ↓ (Async, Non-blocking Execution)
              LinkClickEvent → Kafka → Consumer Microservice → analytics.click_events
```

### Multi-Module Maven Restructuring

To accommodate the growing ecosystem, we transition the project into a multi-module Maven build. This allows us to share code cleanly while keeping our services independently deployable:

```
shorthand/
  pom.xml          ← Parent POM (Manages Spring Boot BOM & shared dependency versions)
  common/          ← Shared module containing event schemas and data contracts
  backend/         ← The core routing engine (from Sprint 1)
  consumer/        ← The new standalone analytics consumer microservice
```

By utilizing `<dependencyManagement>` in the parent POM, we ensure version consistency across all services without child modules needing to declare version numbers explicitly.

### Enforcing the Data Contract (`common` Module)

To prevent the backend and consumer services from drifting out of sync, the `LinkClickEvent` is defined strictly within the `common` module. This serves as our single source of truth; if either service changes the event structure, compilation fails immediately.

```java
public record LinkClickEvent(
    String code,
    String ipAddress,
    String userAgent,
    Instant clickedAt
) {}
```

_Note: This record captures raw data only. Heavy lifting like data enrichment is intentionally offloaded to the consumer to keep the backend footprint minimal._

---

## What Was Built

### 1. Non-Blocking Async Execution

To keep the application highly responsive, we configure Spring’s asynchronous framework to leverage modern concurrency features:

- **Virtual-Thread-Backed Executor:** In `AsyncConfig` (`backend/infrastructure/config`), `@EnableAsync` is enabled using Java 21's `Executors.newVirtualThreadPerTaskExecutor()`. Because virtual threads are lightweight and cheap to create, we completely bypass traditional thread-pool tuning and avoid bottlenecks under sudden traffic spikes.
- **Transparent Execution:** Annotating `LinkClickEventPublisherAdapter.publishMessage()` with `@Async` pushes the Kafka publishing task onto a virtual thread. The request thread drops down and delivers the `302` response immediately, making telemetry capturing entirely transparent to the end user.

### 2. The Kafka Event Pipeline

- **Decoupled Ports:** `LinkClickEventPublisherPort` is introduced in the domain layer to outline messaging capabilities without tying us to a specific broker.
- **Optimized Publishing:** The infrastructure implementation (`LinkClickEventPublisherAdapter`) serializes the event to JSON via Jackson and writes to the `link-click-events` Kafka topic. It uses the short URL `code` as the message key to maintain partition ordering and leverages `CompletableFuture` for completely non-blocking Kafka send acknowledgments.
- **Context Preservation:** In `RedirectController`, the service pulls the true client IP from the `X-Forwarded-For` header (falling back to `getRemoteAddr()`) and grabs the `User-Agent`. These are explicitly passed down through the use-case parameters, ensuring data isn't lost during the transition across threads.

### 3. The Analytics Consumer Microservice

The `consumer` is a standalone, lightweight Spring Boot application running on port `8081`. It operates within its own domain boundary and features:
- **Isolated Data Layer:** It manages its own PostgreSQL schema (`analytics.click_events`) via independent Flyway migrations and a dedicated JPA persistence layer.
- **Real-Time Consumption:** A `@KafkaListener` hooks into the `shorthand-consumer` group, picking up raw events from the Kafka topic and passing them to the internal `ProcessClickEventService`.
- **User-Agent Enrichment:** Using the YAUAA (Yet Another UserAgent Analyzer) library, the consumer parses raw user-agent strings into clean analytical dimensions before storing them:
  * `DeviceName` $\rightarrow$ `device` (Desktop, Mobile, Tablet)
  * `OperatingSystemNameVersion` $\rightarrow$ `os` (Windows 11, iOS 17, Android 14)
  * `AgentNameVersion` $\rightarrow$ `browser` (Chrome 126, Safari 17)
  * `country` $\rightarrow$ Set as a `null` placeholder, reserved for upcoming GeoIP integration.

---

## Pragmatic Engineering Decisions

**Why Virtual Threads over Fixed Thread Pools?**
Publishing events to Kafka is inherently $I/O$-bound. Traditional fixed thread pools require tedious sizing calculations and risk thread starvation under high load. Virtual threads excel at blocking $I/O$, allowing the application to scale concurrently without heavy resource overhead.

**Why Enrich in the Consumer rather than the Backend?** 
Parsing complex User-Agent strings and executing GeoIP lookups are CPU and memory-intensive. Performing these steps synchronously during the redirect window would directly degrade core performance. Offloading this work to the consumer ensures the redirection engine remains lean.

**Why Pass IP and User-Agent as Method Parameters?**
Common approaches like relying on filters or interceptors store request metadata in `ThreadLocal` storage. Since `@Async` context-switches onto a separate virtual thread, `ThreadLocal` properties are instantly lost. Explicit parameters keep our data pipeline predictable, thread-safe, and transparent.

---

## Infrastructure Stack

The entire local ecosystem is managed via a shared Docker network (`shorthand-network`):
- **PostgreSQL 16:** Segregated into a `public` schema for the backend core and an `analytics` schema for the consumer data.
- **Redis 7.2:** Serving as the high-speed caching tier for the core router.
- **Kafka + Zookeeper:** Acting as our high-throughput message streaming backbone.

---

## Next Steps

With our core streaming pipeline stable, the next sprint focuses on hardening the platform, expanding feature sets, and ensuring production readiness:

**GeoIP Enrichment:**
Integrate MaxMind GeoLite2 within the consumer service to resolve the pending `country` field from raw IP addresses.

**Analytics API:**
Build out REST endpoints on the consumer service to expose aggregate metrics, such as total link clicks, device distributions, and time-series click intervals.

**Distributed Rate Limiting:**
Implement a custom Token Bucket algorithm filter backed by Redis on the backend to protect link creation endpoints from automated abuse.

**Database Partitioning:**
Optimize the `analytics.click_events` table for long-term time-series querying by partitioning PostgreSQL tables by the `clicked_at` timestamp.

**Fault Tolerance & Graceful Degradation:**
Layer in Resilience4j Circuit Breakers. If Kafka or the consumer suffers an outage, the core routing service should downgrade gracefully, continuing to redirect users without leaking memory or throwing unhandled errors.

**End-to-End Orchestration:**
Standardize individual `Dockerfiles` for both applications and write a root `docker-compose.yml` to bring up the full environment with a single command. We will also integrate Springdoc OpenAPI (Swagger) so developers can interact with the live API right from their browser.
