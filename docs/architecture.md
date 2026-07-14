# Architecture

## Overview

Shorthand is engineered as a multi-module Maven system comprising three independent service modules:

- **`backend`:** The core redirection engine. It manages short-code generation, link registration, routing resolution, local and distributed caching, and non-blocking click event publication.
- **`consumer`:** The asynchronous analytics processor. It processes raw click streams from Kafka, enriches the payloads with geolocation and user-agent metadata, persists them to PostgreSQL, and exposes an analytical reporting API.
- **`common`:** A shared schema library containing the unified serializable model (`LinkClickEvent`) utilized for communication across the messaging tier.

Each module deploys independently and adheres strictly to **Hexagonal Architecture (Ports and Adapters)**.

---

## Hexagonal Architecture

The central architectural rule governing Shorthand is the strict separation of concerns and the enforcement of the dependency inversion principle: the outer infrastructure layer depends on the application use cases, and the application use cases depend on the core domain models - never the reverse. Spring framework details, database mappers, and messaging drivers are kept completely out of the domain.

```
┌──────────────────────────────────────────────────────┐
│                    Infrastructure                    │
│     (Spring, JPA, Redis, Kafka, Web Controllers)     │
│                                                      │
│   ┌──────────────────────────────────────────────┐   │
│   │              Application Layer               │   │
│   │    (Use case implementations / Services)     │   │
│   │                                              │   │
│   │   ┌──────────────────────────────────────┐   │   │
│   │   │           Domain Layer               │   │   │
│   │   │  (Models, Ports, Domain Logic)       │   │   │
│   │   └──────────────────────────────────────┘   │   │
│   └──────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────┘
```

### Ports

Ports are plain Java interfaces located inside the domain layer. They define the behavioral contracts required by the domain without specifying how those behaviors are implemented.

**Inbound Ports** declare the available application use cases and serve as entry points:
- `CreateLinkUseCase`
- `RedirectLinkUseCase`
- `ProcessClickEventUseCase`
- `ClickAnalyticsUseCase`

**Outbound ports** represent external dependencies that the domain must interact with:
- `LinkRepository`: Link persistence abstractions.
- `LinkCachePort`: High-speed cache interfaces.
- `LinkIdentifierPort`: Identifier and short-code generation engines.
- `LinkClickEventPublisherPort`: Event streaming interfaces.
- `ClickEventRepository`: Analytics write operations.
- `ClickAnalyticsRepository`: Analytics read operations.

### Adapters

Adapters implement (or invoke) the contracts defined by ports and reside entirely within the infrastructure layer.

**Inbound (Driving) Adapters** receive external inputs and trigger use cases:
- `LinkController` / `RedirectController`: Web REST controllers.
- `ClicksAnalyticsController`: Analytical report controllers.
- `LinkClickEventConsumer`: Kafka message listeners.

**Outbound (Driven) Adapters** execute actions triggered by the use cases:
- `LinkRepositoryAdapter` / `ClickEventRepositoryAdapter`: JPA data access adapters.
- `LinkCacheAdapter`: Redis cache interfaces.
- `LinkIdentifierAdapter`: Coordinate Snowflake ID generation paired with Base62 encoding.
- `LinkClickEventPublisherAdapter`: Kafka message producers.
- `GeoIpAdapter`: MaxMind GeoLite2 metadata engines.

### Manual Wiring

Application service implementations (`CreateLinkService`, `RedirectLinkService`, `ProcessClickEventService`, and `ClickAnalyticsService`) are deliberately kept free of Spring's `@Service` or `@Component` annotations. Instead, they are explicitly instantiated as `@Bean` structures inside `ApplicationConfig`. This architecture keeps the core business logic uncoupled from DI framework annotations, making the wiring explicit and facilitating isolated unit testing.

---

## Package Structure

### Core Routing Module (`backend`)

```
com.shorthand.backend
├── application
│   └── service
│       ├── CreateLinkService
│       └── RedirectLinkService
├── domain
│   ├── exception
│   │   ├── LinkExpiredException
│   │   └── LinkNotFoundException
│   ├── model
│   │   └── Link
│   └── port
│       ├── inbound
│       │   ├── CreateLinkUseCase
│       │   └── RedirectLinkUseCase
│       └── outbound
│           ├── LinkCachePort
│           ├── LinkClickEventPublisherPort
│           ├── LinkIdentifierPort
│           └── LinkRepository
└── infrastructure
    ├── adapter
    │   ├── inbound
    │   │   └── web
    │   │       ├── exception
    │   │       │   ├── ErrorResponse
    │   │       │   └── GlobalExceptionHandler
    │   │       ├── filter
    │   │       │   └── RateLimitFilter
    │   │       └── v1
    │   │           ├── dto
    │   │           ├── mapper
    │   │           ├── LinkController
    │   │           └── RedirectController
    │   └── outbound
    │       ├── cache
    │       │   └── LinkCacheAdapter
    │       ├── database
    │       │   ├── LinkEntity
    │       │   ├── LinkEntityMapper
    │       │   ├── LinkJpaRepository
    │       │   └── LinkRepositoryAdapter
    │       ├── generator
    │       │   ├── Base62Encoder
    │       │   ├── LinkIdentifierAdapter
    │       │   └── SnowflakeIdGenerator
    │       └── messaging
    │           └── LinkClickEventPublisherAdapter
    └── config
        ├── ApplicationConfig
        ├── AsyncConfig
        └── ShorthandProperties
```

### Analytics Processing Module (`consumer`)

```
com.shorthand.consumer
├── application
│   └── service
│       ├── ClickAnalyticsService
│       └── ProcessClickEventService
├── domain
│   ├── model
│   │   ├── AnalyticsSummary
│   │   ├── ClickEvent
│   │   └── ClickMetric
│   └── port
│       ├── inbound
│       │   ├── ClickAnalyticsUseCase
│       │   └── ProcessClickEventUseCase
│       └── outbound
│           ├── ClickAnalyticsRepository
│           └── ClickEventRepository
└── infrastructure
    ├── adapter
    │   ├── inbound
    │   │   ├── messaging
    │   │   │   └── LinkClickEventConsumer
    │   │   └── web
    │   │       ├── exception
    │   │       ├── filter
    │   │       └── v1
    │   │           ├── dto
    │   │           ├── mapper
    │   │           └── ClicksAnalyticsController
    │   └── outbound
    │       ├── database
    │       │   ├── ClickAnalyticsJpaRepository
    │       │   ├── ClickAnalyticsRepositoryAdapter
    │       │   ├── ClickEventEntity
    │       │   ├── ClickEventEntityMapper
    │       │   ├── ClickEventJpaRepository
    │       │   └── ClickEventRepositoryAdapter
    │       └── geoip
    │           └── GeoIpAdapter
    └── config
        ├── ApplicationConfig
        └── ShorthandProperties
```

---

## Operational Data Flows

### Link Creation Lifecycle

```
POST /api/v1/links
      │
      ▼
RateLimitFilter (Token Bucket / Redis)
      │
      ▼
LinkController → CreateLinkUseCase
      │
      ▼
CreateLinkService
  ├── LinkIdentifierPort.generateSnowflakeId() → SnowflakeIdGenerator
  ├── LinkIdentifierPort.generateCode()        → Base62Encoder
  └── LinkRepository.save()                    → PostgreSQL
      │
      ▼
LinkWebMapper → CreateLinkResponse
      │
      ▼
201 Created { code, shortUrl, originalLink }
```

### Redirection & Routing Resolution

```
GET /{code}
      │
      ▼
RedirectController
  ├── Extract IP (X-Forwarded-For → getRemoteAddr())
  └── Extract User-Agent (fallback: "Unknown")
      │
      ▼
RedirectLinkService
  ├── LinkCachePort.get(code)     → Redis HIT  → return originalUrl
  │                                  MISS
  │                                   ↓
  ├── LinkRepository.findByCode() → PostgreSQL
  ├── LinkCachePort.put(code, ttl = remaining lifetime)
  └── LinkClickEventPublisherPort.publishMessage()  [async]
      │                                              │
      ▼                                              ▼
302 Found                                      Kafka Producer
Location: {originalUrl}                    link-click-events topic
```

### Analytics Aggregation Pipeline

```
Kafka topic: link-click-events
      │
      ▼
LinkClickEventConsumer (@KafkaListener)
      │
      ▼
ProcessClickEventService
  ├── GeoIpAdapter.resolveCountry(ipAddress) → MaxMind GeoLite2
  ├── UserAgentAnalyzer.parse(userAgent)     → YAUAA
  │     ├── DeviceName
  │     ├── OperatingSystemNameVersion
  │     └── AgentNameVersion
  └── ClickEventRepository.save(ClickEvent)  → analytics.click_events
```

---

## Database Architecture & Schemas

### Public Schema (Managed by `backend` Flyway Migrations)

Represents the core transactional data model optimized for lookup speed.

```sql
CREATE TABLE links (
    id          BIGINT PRIMARY KEY,       -- Snowflake ID (App-Generated)
    code        VARCHAR(15) NOT NULL UNIQUE,
    original_url TEXT NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  TIMESTAMP WITH TIME ZONE
);
```

### Analytics Schema (Managed by `consumer` Flyway Migrations)

Stores event telemetry optimized for high-write volumes. The table leverages monthly range partitioning to ensure performant scale-out capabilities and efficient database cleaning.

```sql
CREATE TABLE analytics.click_events (
    id          BIGSERIAL,                -- DB-generated surrogate key
    link_code   VARCHAR(15) NOT NULL,
    ip_address  VARCHAR(45) NOT NULL,
    country     VARCHAR(100),
    device      VARCHAR(50),
    os          VARCHAR(100),
    browser     VARCHAR(100),
    user_agent  TEXT NOT NULL,
    clicked_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id, clicked_at)          -- Composite PK required for partition indexing
) PARTITION BY RANGE (clicked_at);

-- Monthly partitions
CREATE TABLE analytics.click_events_2026_06
    PARTITION OF analytics.click_events
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE TABLE analytics.click_events_2026_07
    PARTITION OF analytics.click_events
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');
```

---

## Infrastructure Topology

All containers run inside an isolated Docker bridge network, ensuring secure routing paths and structured access controls.

```
┌─────────────────────────────────────────────────────────────┐
│                    shorthand-network (Docker Bridge)        │
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐   │
│  │   backend    │    │   consumer   │    │  zookeeper   │   │
│  │   :8080      │    │   :8081      │    │   :2181      │   │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘   │
│         │                   │                   │           │
│         ▼                   ▼                   ▼           │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐   │
│  │  postgresql  │    │    kafka     │    │    redis     │   │
│  │   :5432      │    │   :9092      │    │   :6379      │   │
│  └──────────────┘    └──────────────┘    └──────────────┘   │
└─────────────────────────────────────────────────────────────┘
```