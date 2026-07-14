# Engineering Decisions

This document serves as the technical decision record for Shorthand. It outlines the core architectural and design choices made during development, detailing the reasoning, alternative solutions considered, and the structural tradeoffs accepted.

---

## Architecture & Framework Design

### Hexagonal Architecture over Layered Architecture

**Decision:** Implement Hexagonal Architecture (Ports and Adapters) rather than a traditional layered design (`Controller` $\rightarrow$ `Service` $\rightarrow$ `Repository`).

**Reasoning:** In classic layered architectures, the service layer becomes tightly coupled to framework-specific dependencies (such as JPA entities, Spring annotations, and transaction managers). This makes business logic highly difficult to test in isolation and locks the domain to specific infrastructure technologies. Hexagonal Architecture strictly inverts this relationship: the outer infrastructure layer depends on the inner domain layer. Swapping out Postgres for jOOQ, or Redis for Memcached, only requires rewriting the outbound adapter; the core business logic remains completely untouched.

**Tradeoff:** Requires more upfront design structure, boilerplate interfaces, and mapper files. However, this architectural investment pays off as the code scales.

### Manual Bean Wiring over `@Service` on Application Services

**Decision:** Avoid annotating application services (`CreateLinkService`, `RedirectLinkService`, etc.) with Spring's stereotype annotations (like `@Service` or `@Component`). Instead, instantiate them manually as `@Bean` declarations inside an infrastructure-based `ApplicationConfig`.

**Reasoning:** Using framework stereotypes inside the application layer violates the Hexagonal boundary by introducing external framework dependencies. Keeping services as plain Java classes ensures they remain framework-agnostic. The `ApplicationConfig` class in the infrastructure layer acts as the dedicated composition root, making the application dependency graph explicit, clean, and testable without bootstrapping a Spring context.

**Tradeoff:** The `ApplicationConfig` configuration class grows as new services are introduced. However, explicit wiring is favored over framework "magic."

---

## Identity & Database Engineering

### Snowflake ID as Primary Key over UUID

**Decision:** Use a 64-bit Snowflake ID as the primary key for the `links` table instead of a random UUID.

**Reasoning:** Random UUIDs cause severe B-tree index fragmentation inside relational databases. Because UUIDs are non-sequential, inserts land at random physical locations within the index, causing frequent page splits and degrading write throughput. Snowflake IDs are time-ordered, ensuring inserts are appended sequentially near the end of the index. This results in high-speed, sequential write performance. Additionally, Snowflake IDs occupy 64 bits (`BIGINT` / `Long`) compared to the 128 bits required by UUIDs, halving the storage overhead for foreign key references in downstream analytics tables.

**Tradeoff:** Requires running a dedicated, machine-ID-configured Snowflake generator. Clock skew in distributed environments can cause ID collisions, which Shorthand handles by throwing an `IllegalStateException` and refusing to generate IDs if the system clock moves backward.

### Base62 Encoding over Random String Generation for Short Codes

**Decision:** Generate short redirect codes by Base62-encoding the unique Snowflake ID instead of generating random alphanumeric strings.

**Reasoning:** Generating random strings introduces collision risks. To guarantee uniqueness, the application would have to perform a database lookup on every write to verify that the generated string does not already exist. Under high load, this pattern quickly becomes a major database bottleneck. Base62-encoding a sequential Snowflake ID is deterministic and mathematically collision-free - two distinct Snowflake IDs will always resolve to two entirely unique short codes, eliminating the need for a pre-insertion database check.

**Tradeoff:** Short codes look slightly sequential in character patterns, which indirectly encodes timing information. This is an acceptable tradeoff for a high-performance Link Shortener.

### Code as a Unique Index, Not the Primary Key

**Decision:** Retain the Snowflake ID as the primary key of the `links` table and define the short `code` as a unique indexed column.

**Reasoning:** Utilizing the alphanumeric short `code` (a `VARCHAR`) as the primary key would require all foreign key references in the high-volume analytics tables to store and query against string values. At scale (millions of click events), executing joins and index scans on `VARCHAR` foreign keys is substantially slower than performing numerical `BIGINT` comparisons. Using a BIGINT Snowflake ID as the primary key optimizes relational joins, while indexing the short `code` guarantees sub-millisecond lookups during redirection.

**Tradeoff:** The data model manages two distinct unique identifiers per link. This complexity is isolated inside the data mapper layers.

---

## Caching & Rate Limiting

### Cache-Aside over Write-Through Caching

**Decision:** Apply the Cache-Aside pattern for Redis cache operations instead of a Write-Through caching mechanism during link creation.

**Reasoning:** Write-through caching writes every new link to Redis at creation time, which wastes valuable in-memory storage on links that may never receive traffic. Cache-Aside only populates Redis upon the first redirection (on a cache miss), ensuring that only active, highly accessed links occupy memory. The TTL is programmatically calculated to match the link's remaining lifetime, ensuring cache entries never outlive their source data.

**Tradeoff:** The first redirection attempt for any newly generated link must perform a PostgreSQL database read. This minor latency hit is acceptable to preserve memory efficiency in Redis.

### TTL Based on Remaining Lifetime over Fixed TTL

**Decision:** When caching a link in Redis on a cache miss, dynamically calculate the TTL as the exact difference:

$$\text{TTL} = \text{Duration.between}(\text{Instant.now()}, \text{link.expiresAt()})$$

rather than applying a fixed duration (such as 60 minutes).

**Reasoning:** Applying a fixed TTL can result in data drift. For example, a link cached with a 60-minute TTL that expires in the database in 10 minutes would continue to resolve successfully from the cache for another 50 minutes. Binding the cache TTL directly to the link's remaining lifetime ensures that the cache entry and the source database record expire simultaneously.

**Tradeoff:** Calculated TTLs are slightly imprecise due to the small millisecond latency gap between the database read and the Redis write. In practice, this minimal drift is negligible.

### Lua Scripts for Atomic Rate Limiting

**Decision:** Implement the Token Bucket rate limiter as a Redis-native Lua script instead of handling the arithmetic on the Java application side.

**Reasoning:** The token bucket algorithm requires a read-compute-write execution loop (fetching the current token balance, recalculating the balance based on time elapsed, and persisting the updated value). Running this sequence in Java introduces race conditions: two concurrent application threads could read the same token count before either commits their update, allowing both requests through when only one should have been permitted. Because Redis executes Lua scripts atomically on a single thread, the entire read-compute-write cycle runs without interleaving operations, blocking out race conditions entirely.

**Tradeoff:** Introduces a language boundary. Testing and debugging require evaluating Redis Lua script runtimes. The strict consistency guarantee under heavy concurrency justifies the complexity.

---

## Event Streaming & Analytics

### Async Event Publishing with Virtual Threads

**Decision:** Configure `@Async` tasks using a dedicated virtual thread task executor (`Executors.newVirtualThreadPerTaskExecutor()`) to publish `LinkClickEvent` payloads to Kafka.

**Reasoning:** Emitting click events to Kafka is highly I/O-bound, spending most of its execution time waiting on network handshakes. Java Virtual Threads are specifically optimized for I/O-bound workloads. By mounting each asynchronous publishing task to its own lightweight virtual thread, the footprint per task is reduced to just a few kilobytes of memory. The primary request thread is freed to return a 302 Redirect response immediately, avoiding the need to tune thread pool parameters.

**Tradeoff:** Spring’s `@Async` mechanism uses AOP proxies, meaning internal method calls within the same class bypass the proxy wrapper and run synchronously. To mitigate this, the `@Async` annotation is placed explicitly on the outbound adapter class rather than inside the application service.

### Analytics Enrichment in the Consumer Service, Not the Backend

**Decision:** Isolate heavy processing tasks (such as MaxMind GeoIP country lookups and YAUAA user-agent parsing) to the `consumer` microservice rather than executing them synchronously during the redirection step in the `backend`.

**Reasoning:** GeoIP resolution requires executing binary file lookups, and parsing detailed user-agent strings using YAUAA initializes a large memory cache of over 100 definition files. Executing these operations during the redirect path would add massive latency overhead to every redirection. Offloading these tasks to the asynchronous Kafka consumer keeps the `backend` redirection engine lean, fast, and optimized for sub-millisecond performance.

**Tradeoff:** Analytics dashboards are not real-time; reports update after the consumer processes the Kafka event stream. For analytics reporting, a delay of a few seconds is perfectly acceptable.

### Shared common Module for Event Schemas

**Decision:** Define the `LinkClickEvent` schema within a shared `common` Maven module that both the `backend` and `consumer` modules import.

**Reasoning:** If the event payload structure was duplicated independently in both microservices, the two data models would be prone to drift. A field renamed or dropped in the `backend` would result in silent runtime failures inside the `consumer` consumer loop. Moving the schema to a shared dependency ensures that any incompatible API or schema changes trigger compile-time errors instead of runtime bugs.

**Tradeoff:** Requires both modules to be updated and built in tandem when event schema updates occur. For this application's scope, the safety of tight compile-time coupling is preferred over runtime message-parsing errors.

---

## Data Tier & Infrastructure

### Partitioned Tables for Analytics

**Decision:** Structure the `analytics.click_events` table as a range-partitioned database table divided by the `clicked_at` timestamp into monthly child partitions.

**Reasoning:** Analytics queries are naturally scoped to specific time windows (e.g., "clicks over the past week"). In a standard table, PostgreSQL must scan the entire index, including historical data, to aggregate these results. Range partitioning enables automatic partition pruning: a query scanning the last 7 days only searches the active monthly partition and skips historical data tables entirely.

**Tradeoff:** Demands a composite primary key structure (`id, clicked_at`), as PostgreSQL cannot enforce globally unique indexes across partitions using the surrogate ID alone. Additionally, future monthly partition tables must be provisioned proactively; attempting to write an event to a month with no active partition throws a database error.

### Separate Database Schema for Analytics

**Decision:** Isolate analytics data inside a dedicated database schema (`analytics`) separate from the transactional `public` schema.

**Reasoning:** The `backend` and `consumer` are separate microservices with different operational footprints. Consolidating their tables within a single schema couples the services at the data layer, making independent migrations difficult. Maintaining isolated schemas allows for granular security and access controls. In production, the `backend` service is restricted to the `public` schema, while the `consumer` retains write privileges to the `analytics` schema.

**Tradeoff:** Requires configuring explicit `@Table(schema = "analytics")` mappings on JPA entities and configuring separate Flyway migrations for both schemas.

---

## Environment Configuration

### Resilience4j Circuit Breakers on the Adapter, Not the Service

**Decision:** Bind Resilience4j `@CircuitBreaker` annotations to the outbound adapter class (`LinkClickEventPublisherAdapter`) rather than placing them on the service layer.

**Reasoning:** `@CircuitBreaker` is an infrastructure and framework-specific concern. Under the Hexagonal Architecture paradigm, the application service layer must remain framework-agnostic. Placing the annotation directly on the outbound adapter isolates framework-specific concerns to the infrastructure layer.

**Tradeoff:** Both `@Async` and `@CircuitBreaker` annotations are stacked on the same adapter execution method. This requires careful attention to the execution order: the `@Async` proxy must trigger first to allocate a virtual thread, and the `@CircuitBreake`r then monitors the downstream network call within that thread.

### Static IP (127.0.0.1) over `localhost` for WSL2 Deployments

**Decision:** Target local service configurations to run against `127.0.0.1:8081` instead of resolving via `localhost:8081` within Windows Subsystem for Linux (WSL2) environments.

**Reasoning:** WSL2 frequently experiences network quirks where `localhost` resolves inconsistently to the IPv6 loopback address (`::1`), while Docker’s internal port bindings map exclusively to IPv4 (`0.0.0.0`). When `localhost` resolves to `::1`, traffic fails to reach the active Docker containers. Explicitly using `127.0.0.1` forces IPv4 loopback resolution, bypassing this common networking issue.

**Tradeoff:** This is a local network configuration detail rather than a code-level constraint, but documenting it ensures consistent development environments across Windows and Linux machines.
