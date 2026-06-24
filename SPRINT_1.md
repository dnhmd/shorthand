# Sprint 1 — The Core Routing Service & Data Layer

## Objective

Build a high-performance URL shortening and redirection service capable of sub-millisecond response times on cache hits, with a clean, maintainable architecture that can scale into an analytics platform in subsequent sprints.

---

## Architecture

### Hexagonal Architecture (Ports & Adapters)

The project is structured around strict layer separation:

- **Domain** — pure Java, zero framework dependencies. Contains the `Link` model, inbound/outbound port interfaces, domain exceptions, and the Base62 encoder.
- **Application** — use case implementations (`CreateLinkService`, `RedirectLinkService`). Depends only on domain ports, never on infrastructure.
- **Infrastructure** — Spring, JPA, Redis, and web adapters. The only layer allowed to import framework-specific code.

Dependency arrows point inward. Infrastructure knows about the application layer. The application layer knows nothing about infrastructure.

### Key Design Decisions

- **No `@Service` on application services** — they are manually constructed as `@Bean` instances in `ApplicationConfig`. This keeps Spring annotations out of the application layer entirely.
- **DTOs never cross the adapter boundary** — `CreateLinkRequest` and `CreateLinkResponse` live in the web adapter. Mappers translate at the boundary before the use case is ever called.
- **Configuration injected as primitives** — `CreateLinkService` receives `defaultExpiryDays` as a plain `int`, not a `@ConfigurationProperties` class. The service stays framework-agnostic.

---

## What Was Built

### Task 1 — Clean Architecture

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

### Task 2 — ID Generation

- **Snowflake ID** — 64-bit time-ordered ID composed of a 41-bit timestamp, 10-bit machine ID, and 12-bit sequence counter. Guarantees 4096 unique IDs per millisecond per node with no coordination overhead. Machine ID is externally configurable.
- **Base62 Encoding** — encodes the Snowflake ID into a short alphanumeric string (`[0-9A-Za-z]`). Produces clean, collision-free short codes without random UUIDs.

### Task 3 — Cache-Aside Redirection

Redirection endpoint: `GET /{code}` → `302 Found`

Cache-Aside sequence:
1. Check Redis for the short code — O(1) lookup
2. **Cache hit** → return `originalUrl`, redirect immediately
3. **Cache miss** → query PostgreSQL, populate Redis with TTL equal to the link's remaining lifetime, redirect

TTL is computed as `Duration.between(Instant.now(), link.expiresAt())` — cache entries never outlive their links.

---

## Additional Work

- **Domain exceptions** — `LinkNotFoundException` (404), `LinkExpiredException` (410 Gone)
- **Global exception handler** — `@RestControllerAdvice` with structured `ErrorResponse` (timestamp, status, error, message). Catch-all handler prevents stack traces leaking to clients.
- **Structured logging** — Logstash JSON encoder for production, human-readable pattern for local. Profile-based via `logback-spring.xml`. Trace ID propagation via Micrometer.
- **Flyway migrations** — schema versioned from V1. JPA set to `validate` mode — Hibernate never touches the schema.

---

## Infrastructure

- PostgreSQL 16 — primary data store
- Redis 7.2 — cache layer
- All infrastructure managed via Docker Compose

---

## What's Next — Sprint 2

**The Event-Driven Analytics Pipeline**

Every redirect will fire a `LinkClickEvent` asynchronously via `@Async` — the 302 response is never blocked. The event carries raw metadata (IP address, User-Agent, timestamp) and is published to a Kafka topic.

A standalone consumer microservice will subscribe to the topic, enrich events (IP → country, User-Agent → device type), and write to a time-series optimised analytics schema in PostgreSQL.

The project will evolve into a multi-module Maven structure:
- `backend` — the core routing service (this module)
- `consumer` — the analytics consumer microservice
- `common` — shared event schema (`LinkClickEvent`)