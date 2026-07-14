# Shorthand

Enterprise-grade URL shortener with real-time click analytics, built with Spring Boot 3, Kafka, Redis, and PostgreSQL using Hexagonal Architecture.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?style=flat-square&logo=springboot)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.9-black?style=flat-square&logo=apachekafka)
![Redis](https://img.shields.io/badge/Redis-7.2-red?style=flat-square&logo=redis)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![Resilience4j](https://img.shields.io/badge/Resilience4j-2.2-green?style=flat-square)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)

---

## What is Shorthand?

Shorthand is a high-throughput link management and real-time click analytics platform designed to demonstrate enterprise-grade backend engineering patterns across three planned development phases.

Every redirect execution fires an asynchronous analytics event to Kafka. A standalone consumer microservice enriches that event with GeoIP and user-agent metadata before persisting it to a partitioned PostgreSQL table. The core routing service remains non-blocking to ensure low-latency redirection. A Redis-backed Token Bucket rate limiter guards the creation endpoint, while a Resilience4j circuit breaker isolates the redirect flow if the Kafka broker experiences downtime.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Request                           │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Backend Service (:8080)                      │
│                                                                 │
│  Rate Limit Filter ──── Token Bucket / Redis (Lua, atomic)      │
│         │                                                       │
│         ▼                                                       │
│  RedirectController                                             │
│         │                                                       │
│         ▼                                                       │
│  RedirectLinkService                                            │
│    ├── Redis ─────────────── Cache-Aside (O(1) lookup)          │
│    ├── PostgreSQL ─────────── links table (on cache miss)       │
│    └── Kafka Producer ──────── @Async + @CircuitBreaker         │
└──────────────────────────────────────┬──────────────────────────┘
                                       │ link-click-events topic
                                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Consumer Service (:8081)                      │
│                                                                 │
│  @KafkaListener                                                 │
│         │                                                       │
│         ▼                                                       │
│  ProcessClickEventService                                       │
│    ├── MaxMind GeoLite2 ──── IP → Country                       │
│    ├── YAUAA ──────────────── UA → Device / OS / Browser        │
│    └── PostgreSQL ─────────── analytics.click_events            │
│                               (partitioned by clicked_at)       │
│                                                                 │
│  REST Analytics API ── 7 endpoints for click metrics            │
└─────────────────────────────────────────────────────────────────┘
```

The system is structured as a Maven multi-module project:

```
shorthand/
├── common/          # Shared Kafka event schema (LinkClickEvent)
├── backend/         # Core routing service
├── consumer/        # Analytics microservice
├── docs/            # Architecture, decisions, API reference, configuration
│   └── sprints/     # Sprint-by-sprint engineering narrative
└── docker-compose.yml
```

Both microservices strictly follow Hexagonal Architecture principles, keeping domain logic entirely decoupled from Spring, JPA, Redis, and Kafka.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3.5 |
| Architecture | Hexagonal (Ports & Adapters) |
| Database | PostgreSQL 16 (Range-Partitioned Tables) |
| Cache | Redis 7.2 (Cache-Aside, Token Bucket) |
| Messaging | Apache Kafka 3.9 |
| ID Generation | Snowflake ID + Base62 Encoding |
| GeoIP | MaxMind GeoLite2 |
| UA Parsing | YAUAA 7.28 |
| Fault Tolerance | Resilience4j Circuit Breakers |
| Migrations | Flyway (Independent per service) |
| Build | Maven (Multi-Module) |
| Containerization | Docker Compose |
| API Docs | Springdoc OpenAPI (Swagger) |

---

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 21
- Maven 3.9+
- A MaxMind account (required to retrieve the free GeoIP lookup database)

### 1. Clone the Repository

```bash
git clone https://github.com/dnhmd/shorthand.git
cd shorthand
```

### 2. Configure the GeoIP Database

1. Sign up for a free account at [maxmind.com](https://www.maxmind.com/en/geolite-free-ip-geolocation-data).
2. Download the `GeoLite2-Country.mmdb` database file.
3. Save the file to the following path: `utils/bin/geo-ip/GeoLite2-Country.mmdb`

### 3. Build the Binaries

```bash
mvn clean package
```

### 4. Boot the Infrastructure Stack

```bash
docker compose up -d
```

This command initializes PostgreSQL, Redis, Kafka, Zookeeper, the core backend, and the consumer service simultaneously. Database schemas migrate automatically via Flyway on startup.

### 5. Verify the Deployment

```bash
docker compose ps
```

Confirm that all six containers report a running status. The backend web server bind is accessible at `http://localhost:8080` and the analytics consumer api at `http://127.0.0.1:8081`.

> **WSL2 Compatibility Note:** Target the explicit loopback IP `127.0.0.1` instead of `localhost` when dispatching calls to the consumer container from Windows or WSL2 terminal environments.

---

## Quick API Tour

### Generate a Short Link

```bash
curl -X POST http://localhost:8080/api/v1/links \
  -H "Content-Type: application/json" \
  -d '{"originalLink": "https://example.com", "expiresInDays": 7}'
```

*Response:*

```json
{
  "code": "In52vyTSef",
  "shortUrl": "http://localhost:8080/In52vyTSef",
  "originalLink": "https://example.com"
}
```

### Perform a Redirection

```bash
curl -I http://localhost:8080/In52vyTSef
```

*Response:*

```
HTTP/1.1 302 Found
Location: https://example.com
```

### Fetch Click Analytics Summary

```bash
curl http://127.0.0.1:8081/api/v1/analytics/In52vyTSef/summary
```

*Response:*

```json
{
  "total": 42,
  "dates": [{ "label": "2026-06-29 00:00:00+00", "count": 42 }],
  "countries": [{ "label": "Kuwait", "count": 38 }],
  "browsers": [{ "label": "Chrome 149", "count": 30 }],
  "operatingSystems": [{ "label": "Windows NT 10.0", "count": 25 }],
  "devices": [{ "label": "Desktop", "count": 30 }]
}
```

---

## Key Engineering Patterns

**Hexagonal Architecture:** Domain structures remain isolated from infrastructure frameworks. Application services are wired as explicit `@Bean` instances within the configuration layer rather than relying on framework-specific `@Service` stereotypes.

**Cache-Aside with Lifespan Alignment:** Redirect actions check the Redis cache layer first. On cache misses, the backend queries PostgreSQL and registers the entry in Redis with a dynamic TTL matching the remaining life of the record.

**Collision-Free Code Generation:** Short links are derived by converting time-ordered Snowflake IDs into Base62 representations. This ensures generation is deterministic and mathematically collision-free without requiring round-trip database uniqueness checks.

**Atomic Token Bucket Throttling:** Rate-limiting policies run natively inside Redis using a custom Lua script. Token replenishment is continuous, closing boundary burst loopholes and avoiding multi-thread race conditions.

**Asynchronous Event Handlers:** Tracking data is dispatched using non-blocking `@Async` proxies bound to Java 21 virtual threads. This configuration allows the backend to redirect client traffic immediately while streaming telemetry tasks run out-of-band.

**Circuit Breaker Boundaries:** The Kafka publishing client is wrapped by a Resilience4j circuit breaker. If the broker is unavailable, the circuit transitions to an open state and gracefully diverts tracking data to fallback loggers to protect the primary redirect loop.

**Database Partitioning:** The analytics database leverages range partitioning across the `clicked_at` timestamp. Query engines utilize partition pruning to isolate execution paths to specific target monthly tables.

---

## Platform Documentation Directory

| Document                                                         | Description                                                                          |
|------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| [`docs/architecture.md`](docs/architecture.md)                   | Core components, package boundaries, data flows, and database definitions.           |
| [`docs/engineering-decisions.md`](docs/engineering-decisions.md) | Architectural decision records, structural options, and technical tradeoffs.         |
| [`docs/api-reference.md`](docs/api-reference.md)                 | Complete REST specifications, payload shapes, and global error codes.                |
| [`docs/configuration.md`](docs/configuration.md)                 | Environment variables, local properties, networks, and diagnostic logging.           |
| [`docs/sprints/SPRINT_1.md`](docs/sprints/SPRINT_1.md)           | Technical breakdown of core routing, cache resolution, and identity generation.      |
| [`docs/sprints/SPRINT_2.md`](docs/sprints/SPRINT_2.md)           | Architecture of the Kafka streaming pipeline and consumer microservice integration.  |
| [`docs/sprints/SPRINT_3.md`](docs/sprints/SPRINT_3.md)           | Implementation of GeoIP resolution, rate limiters, circuit breakers, and containers. |

---

## Development Roadmap

| Phase    | Milestone Focus                                                                                                       | Status   |
|----------|-----------------------------------------------------------------------------------------------------------------------|----------|
| Sprint 1 | Core Redirection Engine, Cache-Aside Caching, Base62 Encoding, and Snowflake Keys                                     | Complete |
| Sprint 2 | Non-blocking Kafka Pipeline, Standalone Consumer Microservice                                                         | Complete |
| Sprint 3 | GeoIP Resolution, Analytics API, Redis Token Bucket Limiter, Circuit Breakers, Dockerization                          | Complete |
| Sprint 4 | Integeration Tests, Dead Letter Queue, API Key Authentication, Multi-tenant Profiles, Cloud Infrastructure Deployment | Planned  |