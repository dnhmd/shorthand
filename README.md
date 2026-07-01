# Shorthand

> A high-performance link management platform designed to process massive redirection volume while capturing real-time user metrics. By isolating the critical redirection path from the data-enrichment pipeline, the core engine achieves sub-millisecond response times on cached links, utilizing Kafka and Java 21 virtual threads to handle analytical ingestion completely out of the request-response lifecycle.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?style=flat-square&logo=springboot)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.9-black?style=flat-square&logo=apachekafka)
![Redis](https://img.shields.io/badge/Redis-7.2-red?style=flat-square&logo=redis)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)

---

## What is Shorthand?

Shorthand operates as a high-throughput link management and real-time click analytics platform engineered to demonstrate enterprise-grade patterns, specifically caching, event-driven streaming, distributed rate limiting, fault tolerance, and time-series data storage.

Every redirection triggers an asynchronous analytics event via Kafka. A standalone consumer microservice enriches that payload with GeoIP and user-agent data before persisting it to a range-partitioned PostgreSQL table. The core routing service handles the redirect immediately and never blocks on downstream processing.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Request                           │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Backend Service (:8080)                       │
│                                                                 │
│  Rate Limit Filter (Token Bucket / Redis)                       │
│         │                                                       │
│         ▼                                                       │
│  RedirectController                                             │
│         │                                                       │
│         ▼                                                       │
│  RedirectLinkService                                            │
│    ├── LinkCachePort ──────────────► Redis (Cache-Aside)        │
│    ├── LinkRepository ─────────────► PostgreSQL (links)         │
│    └── LinkClickEventPublisherPort                              │
│              │ @Async + @CircuitBreaker                         │
│              ▼                                                  │
│         KafkaProducer ────────────► link-click-events topic     │
└─────────────────────────────────────────────────────────────────┘
                                              │
                                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Consumer Service (:8081)                       │
│                                                                 │
│  KafkaListener                                                  │
│         │                                                       │
│         ▼                                                       │
│  ProcessClickEventService                                       │
│    ├── GeoIpAdapter ──────────────► MaxMind GeoLite2            │
│    ├── YAUAA ─────────────────────► Device / OS / Browser       │
│    └── ClickEventRepository ──────► PostgreSQL (analytics)      │
│                                                                 │
│  ClicksAnalyticsController                                      │
│    └── 7 REST endpoints for click metrics                       │
└─────────────────────────────────────────────────────────────────┘
```

### Project Structure

```
shorthand/
├── common/          # Shared Kafka event schema (LinkClickEvent)
├── backend/         # Core routing service — links, redirects, rate limiting
├── consumer/        # Analytics microservice — Kafka consumer, metrics API
├── utils/           # Binary assets (GeoIP database — git-ignored)
└── docker-compose.yml
```

---

## Tech Stack

| Layer            | Technology                            |
|------------------|---------------------------------------|
| Language         | Java 21 (Virtual Threads)             |
| Framework        | Spring Boot 3.5                       |
| Architecture     | Hexagonal (Ports & Adapters)          |
| Database         | PostgreSQL 16 (Partitioned Tables)    |
| Cache            | Redis 7.2 (Cache-Aside, Token Bucket) |
| Messaging        | Apache Kafka 3.9                      |
| ID Generation    | Snowflake ID + Base62 Encoding        |
| GeoIP            | MaxMind GeoLite2                      |
| UA Parsing       | YAUAA 7.28                            |
| Fault Tolerance  | Resilience4j Circuit Breakers         |
| Migrations       | Flyway                                |
| Build Tool       | Maven (Multi-Module)                  |
| Containerization | Docker Compose                        |
| API Docs         | Springdoc OpenAPI (Swagger)           |

---

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 21
- Maven 3.9+
- MaxMind GeoLite2 database (free account required)

### 1. Clone the repository

```bash
git clone https://github.com/dnhmd/shorthand.git
cd shorthand
```

### 2. Download GeoIP database

1. Create a free account at [maxmind.com](https://www.maxmind.com/en/geolite-free-ip-geolocation-data)
2. Download `GeoLite2-Country.mmdb`
3. Place it at `utils/bin/geo-ip/GeoLite2-Country.mmdb`

### 3. Build the project

```bash
mvn clean package -DskipTests
```

### 4. Initialize the stack

```bash
docker compose up -d
```

This command provisions PostgreSQL, Redis, Kafka, Zookeeper, the backend routing service, and the consumer worker inside a single shared network context.

### 5. Verify deployments

```bash
docker compose ps
```

---

## API Reference

### Backend Core Routing Engine (`http://localhost:8080`)

Interactive Swagger UI Playground: [`http://localhost:8080/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html)

#### Create a Short Link

```bash
curl -X POST http://localhost:8080/api/v1/links \
  -H "Content-Type: application/json" \
  -d '{"originalLink": "https://example.com", "expiresInDays": 7}'
```
_Response:_

```json
{
  "code": "In52vyTSef",
  "shortUrl": "http://localhost:8080/In52vyTSef",
  "originalLink": "https://example.com"
}
```

#### Redirect

```bash
curl -I http://localhost:8080/In52vyTSef
```

_Response:_

```Plaintext
HTTP/1.1 302 Found
Location: https://example.com
```

### Analytical Consumer Microservice (`http://127.0.0.1:8081`)

Interactive Swagger UI Playground: [`http://127.0.0.1:8081/swagger-ui/index.html`](http://127.0.0.1:8081/swagger-ui/index.html)

#### Retrieve Complete Analytics Summary

```bash
curl http://127.0.0.1:8081/api/v1/analytics/In52vyTSef/summary
```

_Response:_

```json
{
  "total": 42,
  "dates": [{ "label": "2026-06-29", "count": 42 }],
  "countries": [{ "label": "India", "count": 38 }, { "label": "Unknown", "count": 4 }],
  "browsers": [{ "label": "Chrome 149", "count": 30 }],
  "operatingSystems": [{ "label": "Windows NT 10.0", "count": 25 }],
  "devices": [{ "label": "Desktop", "count": 38 }]
}
```

#### All analytics endpoints

| Endpoint | Description |
|---|---|
| `GET /api/v1/analytics/{code}/summary` | Consolidates all available operational matrices into a single payload. |
| `GET /api/v1/analytics/{code}/clicks/total` | Returns total aggregate click volume. |
| `GET /api/v1/analytics/{code}/clicks/by-date` | Groups redirect performance over calendar day intervals. |
| `GET /api/v1/analytics/{code}/clicks/by-country` | Segregates traffic indicators by originating country. |
| `GET /api/v1/analytics/{code}/clicks/by-browser` | Filters event distribution by end-user client applications. |
| `GET /api/v1/analytics/{code}/clicks/by-os` | Outlines performance across distinct operating systems. |
| `GET /api/v1/analytics/{code}/clicks/by-device` | Isolates historical metrics based on client hardware profiles. |

---

## Key Engineering Patterns

**Hexagonal Architecture:** Domain models and core use cases remain entirely decoupled from Spring Boot frameworks, database engines, and cache layers. Core services are constructed manually via specialized configuration files (`@Beans`) rather than using container-bound annotations.

**Dynamic Cache-Aside Redirection:** — The edge proxy evaluates Redis memory structures prior to executing queries. When cache failures occur, transactions fall back to PostgreSQL and populate Redis using a variable TTL synchronized with the target link's remaining operational lifetime.

**Time-Ordered ID Formats:** High-scale, collision-free short codes are computed by transforming time-ordered 64-bit Snowflake IDs into alpha-numeric Base62 values, avoiding the storage overhead of random UUID variants.

**Atomic Rate Control:** Incoming API abuse prevention is evaluated directly inside Redis via optimized Lua script. The evaluation steps execute within a single thread loop to maintain exact consistency across independent service nodes.

**Asynchronous Message Ingestion:** Telemetry tracking logic utilizes Spring's `@Async` abstraction running atop lightweight Java 21 virtual threads, letting the edge gateway return `302 Found` responses without awaiting broker responses.

**Fault Isolation Over Message Buses:** Outbound telemetry publishers are monitored via specialized Resilience4j circuit breakers. Downstream messaging interruptions trigger structural fallback captures, letting the core edge controller maintain routing continuity.

**Time-Series Schemas:** Large-scale analytics logs are split natively inside PostgreSQL using range partitions indexed by time variables, letting metrics evaluation logic prune unneeded chunks automatically.

---

## Environment Variables

| Variable                   | Default                                          | Description                                                           |
|----------------------------|--------------------------------------------------|-----------------------------------------------------------------------|
| `DB_URL`                   | `jdbc:postgresql://localhost:5432/shorthand_db`  | Connection target details for the core database instance.             |
| `DB_USERNAME`              | `shorthand_user`                                 | Authorized administration account name.                               |
| `DB_PASSWORD`              | `shorthand_password`                             | Access credential for the target database schema.                     |
| `REDIS_HOST`               | `localhost`                                      | Target network location for memory caching arrays.                    |
| `REDIS_PORT`               | `6379`                                           | Operational network port for Redis interactions.                      |
| `KAFKA_BOOTSTRAP_SERVERS`  | `localhost:9092`                                 | Core bootstrap details for active Kafka instances.                    |
| `GEOIP_DATABASE_PATH`      | `../utils/bin/geo-ip/GeoLite2-Country.mmdb`      | Absolute file destination mapping regional lookup databases.          |
| `DEFAULT_EXPIRY_DAYS`      | `7`                                              | Fallback operational lifespan assigned to standard shortened indices. |
| `RATE_LIMITER_CAPACITY`    | `10`                                             | Maximum token depth allocated per individual client bucket.           |
| `RATE_LIMITER_REFILL_RATE` | `0.1`                                            | Fractional allocation speed calculating token replenishment.          |

---

## Live Demo

> Deployment preparation is underway. Public platform endpoints will be updated here upon completion.

---

## Sprint History

| Sprint       | Focus                                                                                                                             | Status   |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------|----------|
| **Sprint 1** | Edge Routing Engines, Cache-Aside Layouts, Base62 Transformations.                                                                | Complete |
| **Sprint 2** | Event Streaming Buses, Autonomous Metric Consumers.                                                                               | Complete |
| **Sprint 3** | GeoIP Engines, REST Analytics Layer, Token-Bucket Abstraction, Resilience Integrations, Container Orchestration.                  | Complete |
| **Sprint 4** | Dead Letter Replay Implementations, Dynamic Security Credentials, Account Boundaries, Live Stream WebSockets, Cloud Definitions.  | Planned  |
