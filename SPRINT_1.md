# The Core Routing Service & Data Layer

## Objective

The goal of this sprint was to build a high-performance URL shortening and redirection service that delivers sub-millisecond response times on cache hits. We focus heavily on establishing a clean, modular architecture from day one, ensuring the core routing logic is completely decoupled and ready to scale into an event-driven analytics platform in the upcoming sprints.

---

## Architecture & Design Philosophy

### Hexagonal Architecture

To prevent framework lock-in and keep the core business logic predictable, we structure the project around a strict Hexagonal architecture. The boundaries are enforced as follows:

- **Domain:** Written in pure, framework-agnostic Java. It owns the core `Link` model, inbound/outbound port interfaces, and custom business exceptions.
- **Application:** Implements the core use cases (`CreateLinkService`, `RedirectLinkService`). It talks exclusively to domain ports and remains entirely unaware of how data is stored or exposed.
- **Infrastructure:** Houses the heavy lifting. Spring Boot, JPA, Redis, and HTTP adapters. This is the _only_ layer permitted to import external framework code.
Dependency arrows point inward. Infrastructure knows about the application layer. The application layer knows nothing about infrastructure.

Because dependency arrows point strictly inward, the infrastructure layer adapts to the application layer, while the core business logic remains entirely isolated and easily testable.

### Pragmatic Decisions for True Decoupling

- **Zero Spring Annotations in Core Logic:** We intentionally keep `@Service` annotations out of the application layer. Instead, services are wired manually as `@Bean` instances within `ApplicationConfig`. This keeps the core logic independent of the Spring container.
- **Strict Adapter Boundaries:** Data Transfer Objects (DTOs) like `CreateLinkRequest` and `CreateLinkResponse` never leak into the application layer. Translation happens strictly at the HTTP boundary via mappers before the use case is invoked.
- **Primitive Configuration Injection:** Instead of passing heavy `@ConfigurationProperties` objects deep into the business logic, configurations (like `defaultExpiryDays`) is injected as plain primitives.

---

## What Was Built

### 1. Project Layout & Clean Boundaries

The codebase is organized to clearly separate concerns, making it easy for any developer to navigate:

```
domain/
  model/         Link 
  port/
    inbound/     CreateLinkUseCase, RedirectLinkUseCase
    outbound/    LinkRepository, LinkCachePort, LinkIdentifierPort
  exception/     LinkNotFoundException, LinkExpiredException

application/
  service/       CreateLinkService, RedirectLinkService

infrastructure/
  adapter/
    inbound/
      web/v1/    LinkController, RedirectController, DTOs, Mapper
      web/exception/ GlobalExceptionHandler, ErrorResponse
    outbound/
      database/  LinkJpaRepository, LinkRepositoryAdapter, LinkEntity, LinkEntityMapper
      cache/     LinkCacheAdapter
      generator/ SnowflakeIdGenerator, Base62Encoder, LinkIdentifierAdapter
  config/        ApplicationConfig, ShorthandProperties
```

### 2. High-Scale ID Generation

To handle massive throughput without relying on database-generated sequences or bulky UUIDs, We implement a two-part ID generation strategy:

- **Snowflake IDs:** A 64-bit, time-ordered identifier (41-bit timestamp, 10-bit worker ID, 12-bit sequence). This allows the service to generate up to 4,096 unique, collision-free IDs per millisecond per node without any internode coordination overhead.
- **Base62 Encoding:** The resulting Snowflake ID is encoded into a short, clean alphanumeric string (`[0-9A-Za-z]`), giving us highly optimized, user-friendly short URLs.

### Cache-Based Redirection

The redirection endpoint (`GET /{code}`) is optimized to return a `302 Found` status with minimal latency using a classic Cache-Aside approach:

1. **Look Up Short Code:** Hit Redis for an _O_(1) lookup.
2. **Cache Hit:** Immediately return the `originalUrl` and redirect the user. 
3. **Cache Miss:** Fall back to PostgreSQL, stream the record, populate Redis with calculated dynamic TTL (`Duration.between(Instant.now(), link.expiresAt())`), and then redirect.

_Note: Calculating the TTL dynamically ensures that cache entries naturally expire at the exact moment the link becomes invalid, preventing stale data overhead._

---

## Enhancements

- **Robust Exception Handling:** Business exceptions map directly to semantic HTTP statuses (e.g., `LinkNotFoundException` yields a `404 Not Found`, and `LinkExpiredException` returns a `410 Gone`). A `@RestControllerAdvice` interceptor structures these cleanly while preventing raw stack traces from leaking to the client.
- **Observability & Structured Logging:** Configured a profile-based logging system using Logstash to output JSON in production (for easy log aggregation) and human-readable lines locally. Distributed tracing is handled via Micrometer to ensure trace IDs propagate flawlessly across boundaries.
- **Reliable Schema Evolution:** Integrated Flyway to manage schema migrations cleanly. Hibernate's DDL generation is set to `validate` mode, ensuring production schemas are strictly controlled via versioned SQL scripts rather than automated ORM guesswork.

---

## Infrastructure Stack

The entire local environment is containerized and spun up instantly via Docker Compose:
- PostgreSQL 16 as our reliable source of truth.
- Redis 7.2 acting as our high-speed caching tier.

---

## Next Steps

**The Event-Driven Analytics Pipeline**

With the core routing layer stabilized, the next phase is capturing real-time metrics without degrading redirection performance.

1. **Asynchronous Telemetry:** Every redirect will trigger a `LinkClickEvent` handled off the main request thread via Spring's `@Async` framework, ensuring the `302` response remains lightning fast.
2. **Message Brokering:** These events (containing metadata like IP, User-Agent, and timestamps) will be pushed directly to a Kafka topic.
3. **Dedicated Ingestion:** A standalone consume microservice will be introduced to ingest these events, enrich them (e.g., parsing IPs into countries and User-Agents into device types), and write them to a time-series optimized schema in Postgres.

To support this ecosystem, the project will transition into a multi-module Maven build:
- `backend`: The core redirection engine built in this sprint.
- `consumer`: The new analytical worker service.
- `common`: A shared module holding our event contracts (`LinkClickEvent`).
