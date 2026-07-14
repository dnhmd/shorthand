# Configuration

# Configuration

All configurations are externalized using standard environment variables paired with localized fallbacks. Each service reads from its own dedicated `application.yaml` file, resolving variables via the `${VAR:default}` interpolation pattern (utilizing the specified environment variable if present, or defaulting to the safe fallback value).

---

## Backend Redirection Service

### Complete `application.yaml`

```yaml
spring:
  application:
    name: backend
  profiles:
    active: local
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/shorthand_db}
    username: ${DB_USERNAME:shorthand_user}
    password: ${DB_PASSWORD:shorthand_password}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
  data:
    jpa:
      repositories:
        bootstrap-mode: default
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 60000
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

server:
  port: 8080

shorthand:
  link:
    default-expiry-days: ${DEFAULT_EXPIRY_DAYS:7}
    base-url: ${BASE_URL:http://localhost:8080/}
  snowflake:
    machine-id: ${DEFAULT_MACHINE_ID:1}
  kafka:
    topic: ${KAFKA_TOPIC:link-click-events}
  rate-limiter:
    capacity: ${RATE_LIMITER_CAPACITY:10}
    refill-rate: ${RATE_LIMITER_REFILL_RATE:0.1}
    key-prefix: ${RATE_LIMITER_KEY_PREFIX:rate-limit}

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

logging:
  level:
    org.apache.kafka: WARN
    org.apache.kafka.clients.producer.ProducerConfig: ERROR
    com.shorthand.backend: DEBUG
```

### Backend Environment Variable Matrix

| Variable                   | Default Value                                   | Description                                                        |
|----------------------------|-------------------------------------------------|--------------------------------------------------------------------|
| `DB_URL`                   | `jdbc:postgresql://localhost:5432/shorthand_db` | Relational PostgreSQL JDBC connection endpoint.                    |
| `DB_USERNAME`              | `shorthand_user`                                | Primary database connection username.                              |
| `DB_PASSWORD`              | `shorthand_password`                            | Primary database connection password.                              |
| `REDIS_HOST`               | `localhost`                                     | Hostname for the distributed Redis cache tier.                     |
| `REDIS_PORT`               | `6379`                                          | Network port for the Redis instance.                               |
| `KAFKA_BOOTSTRAP_SERVERS`  | `localhost:9092`                                | Network routing address for the Kafka brokers.                     |
| `DEFAULT_EXPIRY_DAYS`      | `7`                                             | Standard link lifespan (in days) if unspecified.                   |
| `BASE_URL`                 | `http://localhost:8080/`                        | Base URL appended to the short code in response bodies.            |
| `DEFAULT_MACHINE_ID`       | `1`                                             | Snowflake ID machine identifier (must be unique across instances). |
| `KAFKA_TOPIC`              | `link-click-events`                             | Target Kafka topic for streaming async redirect events.            |
| `RATE_LIMITER_CAPACITY`    | `10`                                            | Maximum token depth allowed in the rate limit bucket.              |
| `RATE_LIMITER_REFILL_RATE` | `0.1`                                           | Tokens added per second ($0.1$ equals ~6 requests per minute).     |
| `RATE_LIMITER_KEY_PREFIX`  | `rate-limit`                                    | Namespacing prefix used to isolate keys inside Redis.              |

### Resilience4j Circuit Breaker Configurations

| Property                                       | Value         | Description                                                                             |
|------------------------------------------------|---------------|-----------------------------------------------------------------------------------------|
| `failure-rate-threshold`                       | `50`          | Automatically transitions circuit state to OPEN when failure rates cross $50\%$.        |
| `minimum-number-of-calls`                      | `5`           | Minimum evaluations needed before calculating the target error percentage.              |
| `wait-duration-in-open-state`                  | `30s`         | Active cooldown duration before switching states from **OPEN** to **HALF_OPEN**.        |
| `permitted-number-of-calls-in-half-open-state` | `3`           | Number of test requests allowed to execute when the breaker evaluates recovery.         |
| `sliding-window-size`                          | `10`          | Total transactional iterations analyzed in the sliding window.                          |
| `sliding-window-type`                          | `COUNT_BASED` | Base evaluation model (triggers on request thresholds, not on chronological intervals). |

---

## Consumer Analytics Service

### Complete `application.yaml`

```yaml
spring:
  application:
    name: consumer
  profiles:
    active: local
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/shorthand_db}
    username: ${DB_USERNAME:shorthand_user}
    password: ${DB_PASSWORD:shorthand_password}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
    default-schema: analytics
    schemas: analytics
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: shorthand-consumer
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

server:
  port: 8081

shorthand:
  geo-ip:
    database-path: ${GEOIP_DATABASE_PATH:../utils/bin/geo-ip/GeoLite2-Country.mmdb}
  kafka:
    topic: ${KAFKA_TOPIC:link-click-events}

logging:
  level:
    org.apache.kafka: WARN
    com.shorthand.consumer: DEBUG
```

### Consumer Environment Variable Matrix

| Variable                  | Default                                         | Description                                                        |
|---------------------------|-------------------------------------------------|--------------------------------------------------------------------|
| `DB_URL`                  | `jdbc:postgresql://localhost:5432/shorthand_db` | Target PostgreSQL JDBC connection URL.                             |
| `DB_USERNAME`             | `shorthand_user`                                | Primary database connection username.                              |
| `DB_PASSWORD`             | `shorthand_password`                            | Primary database connection password.                              |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`                                | Network routing address for the Kafka broker.                      |
| `KAFKA_TOPIC`             | `link-click-events`                             | Target Kafka topic for ingestion.                                  |
| `GEOIP_DATABASE_PATH`     | `../utils/bin/geo-ip/GeoLite2-Country.mmdb`     | Path location referencing the MaxMind GeoLite2 binary lookup file. |

### Isolation of Migration Schemas

The consumer manages and provisions its analytical schema tables independently of the backend transactional storage. To maintain isolation, Flyway schema metadata is targeted directly to the `analytics` namespace rather than the standard `public` schema space:

```yaml
flyway:
  default-schema: analytics
  schemas: analytics
```

---

## Containerized Network Environments (Docker Compose)

Under Docker Compose orchestrations, target configuration variables are mapped to cross-container infrastructure configurations. This ensures internal network DNS names are utilized rather than standard loopback (`localhost`) patterns.

### Backend Container Stack

```yaml
environment:
  DB_URL: jdbc:postgresql://shorthand-postgres:5432/shorthand_db
  DB_USERNAME: shorthand_user
  DB_PASSWORD: shorthand_password
  REDIS_HOST: shorthand-redis
  REDIS_PORT: 6379
  KAFKA_BOOTSTRAP_SERVERS: kafka:29092
```

### Consumer Container Stack

```yaml
environment:
  DB_URL: jdbc:postgresql://shorthand-postgres:5432/shorthand_db
  DB_USERNAME: shorthand_user
  DB_PASSWORD: shorthand_password
  KAFKA_BOOTSTRAP_SERVERS: kafka:29092
  GEOIP_DATABASE_PATH: /app/utils/bin/geo-ip/GeoLite2-Country.mmdb
volumes:
  - ./utils/bin/geo-ip:/app/utils/bin/geo-ip
```

The GeoIP database binary file is mounted from the host system file system into the consumer runtime container as a bound volume. It is not bundled directly inside the production Docker image layers; the underlying database target is ignored by Git and must be provisioned independently.

### Kafka Inter-Network Listener Protocol

To facilitate interactions from both the host system and within isolated Docker container bridge contexts, Kafka uses two distinct networking listeners:

- **From the host machine** (for local development tools) — via `localhost:9092`
- **From other containers** (backend, consumer) — via `kafka:29092`

This is handled with two listener configurations:

```yaml
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092
```

- **`PLAINTEXT` (localhost:9092):** Interfaces directly with external developer host utilities.  
- **`PLAINTEXT_INTERNAL` (kafka:29092):** Handles internal inter-container communication on the isolated Docker network bridge.

---

## Structured Telemetry & Logging

Both modules coordinate logging output via profile-based definitions inside `logback-spring.xml`.

### Dev & Local Profiles

Outputs human-readable, colorized terminal logs. General application logging verbosity is mapped directly to the `DEBUG` tier to simplify diagnostics.

### Production Profile (`prod`)

Funnels logs using the standard `LogstashEncoder` format into structured JSON streams. Every recorded log line is written as a structured JSON record containing consistent key-value structures: timestamp, level, logger class, message payload, and tracing context identifiers (`traceId`, `spanId`).

An integrated file-appender applies the following rolling maintenance policies:
- **Max Segment File Size:** 10MB
- **Log Retention Period:** 30 days
- **Aggregate Storage Ceiling:** 2GB

### Unified Log Formatting Standard

To simplify cross-tier diagnostics, every log message produced within the application uses a structured pattern:

```
{Component} | {Key}: {Value} | {Status/Result}
```

Examples:
```
Link Creation | Code: In52vyTSef | Saved
Link Redirect | Code: In52vyTSef | Cache Miss
Link Redirect | Code: In52vyTSef | Found in DB
Kafka Publish | Code: In52vyTSef | Success
Kafka Circuit Breaker | Code: In52vyTSef | Kafka Unavailable | Error: ...
Click Event | Code: In52vyTSef | Enriched & Processed
GeoIP | IP: 192.168.1.1 | Country Resolution Failed | Error: ...
Exception | Type: LinkNotFoundException | Message: Link Unavailable
Exception | Unhandled
```

### Trace Context Propagation

Core tracing is orchestrated via Micrometer (`micrometer-tracing-bridge-brave`) and injected into the log output using the Mapped Diagnostic Context (MDC) pipeline. Under production workloads (JSON-mode configurations), `traceId` and `spanId` are surfaced as top-level JSON fields, making log consolidation and transactional correlation simple when ingesting logs into centralized indexing tools like ELK or Grafana Loki.

---

## Performance Tuning

### Rate Limiter Configurations

The Token Bucket algorithm's throughput metrics are controlled using three primary properties:

| Property      | Effect                                                                  |
|---------------|-------------------------------------------------------------------------|
| `capacity`    | Sets the maximum request burst limit allowed in a single moment.        |
| `refill-rate` | Dictates the sustained recovery threshold (units refilled per second).  |
| `key-prefix`  | Isolates internal bucket states in Redis from other transactional keys. |

#### Scaling Rate-Limiter Scenarios

| Configured Capacity | Refill Rate | Effective Real-World Limit                                         |
|---------------------|-------------|--------------------------------------------------------------------|
| 10                  | 0.1         | Permits up to `10` burst calls, stabilizing to `~6` requests/min.  |
| 20                  | 0.33        | Permits up to `20` burst calls, stabilizing to `~20` requests/min. |
| 5                   | 0.083       | Permits up to `5` burst calls, stabilizing to `~5` requests/min.   |

Redis keys tracked by the rate limiter include an auto-expiration window when a bucket remains idle and completely replenished. This automatic TTL is calculated using the following ceil function:

$$\text{TTL} = \left\lceil \frac{\text{Capacity}}{\text{Refill Rate}} \right\rceil \text{ seconds}$$

### User-Agent Parsing (YAUAA) Cache

The User-Agent parsing analyzer inside the consumer application is instantiated with a dedicated high-speed cache of 1000 items:

```java
UserAgentAnalyzer.newBuilder()
    .withCache(1000)
    .build();
```

User-agent parsing is a resource-intensive operation. At system startup, YAUAA initializes 116 local definition rule files and compiles complex regex parsing maps. Running this parse logic sequentially on every event stream message degrades system throughput. Caching the 1000 most frequently observed user-agent strings bypasses parsing overhead for identical client environments and bots.
