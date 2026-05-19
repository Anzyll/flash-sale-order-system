#  Architecture Decisions

This document captures the major architectural decisions, tradeoffs, and design rationale used in the Flash Sale System.

---

# 1. Modular Monolith over Microservices

## Decision

Adopt a **Modular Monolith** architecture with event-driven asynchronous processing instead of microservices.

---

## Why?

The system requires:

- High-concurrency request handling
- Fast development iteration
- Operational simplicity
- Easier debugging
- Lower infrastructure overhead

A modular monolith provides strong separation between business domains while avoiding distributed system complexity too early.

Core modules include:

- Product
- Sale
- Order
- User

The Order module contains the primary concurrency and asynchronous processing workflows.

Internally, the system follows a lightweight **Hexagonal Architecture (Ports & Adapters)** approach to separate business logic from infrastructure concerns.

---

## Why NOT Microservices?

Microservices were rejected because they introduce:

- Distributed transaction complexity
- API gateway requirements
- Service discovery overhead
- Complex deployment pipelines
- Increased observability requirements
- Additional network latency

For the current system scope, the operational complexity outweighed the architectural benefits.

---

## Tradeoffs

| Benefit | Tradeoff |
|---|---|
| Simpler deployment | Less independent scaling |
| Faster development | Shared deployment lifecycle |
| Easier debugging | Tighter module coupling |

---

# 2. Redis + Lua over Database Locking

## Decision

Use Redis with Lua scripting for inventory reservation instead of direct database locking strategies.

---

## Why?

Flash-sale systems experience extremely high write contention during inventory reservation.

Redis provides:

- In-memory performance
- Very low latency
- Reduced database pressure
- Atomic execution support

Inventory reservation uses Redis Lua scripting to execute:

```text
check stock → decrement stock
```

as a single atomic operation.

This prevents:

- Overselling
- Race conditions
- Negative inventory

under concurrent purchase attempts.

---

## Why NOT Optimistic Locking?

Optimistic locking was rejected because high-concurrency flash-sale traffic created:

- Frequent update conflicts
- Retry storms
- Increased latency
- Reduced throughput under contention

Under burst traffic, excessive retries degraded system stability.

---

## Why NOT Pessimistic Locking?

Pessimistic locking was rejected because:

- Row-level locks reduced concurrency
- Database throughput degraded significantly
- Lock contention increased latency
- Database became the primary bottleneck

---

## Tradeoffs

| Benefit | Tradeoff |
|---|---|
| Very low latency | Recovery complexity |
| Strong concurrency control | Redis synchronization overhead |
| Reduced DB contention | Additional infrastructure dependency |

---

# 3. Kafka & Asynchronous Processing

## Decision

Use Kafka for asynchronous order processing instead of direct synchronous persistence.

---

## Why?

Direct database writes become a bottleneck during flash-sale spikes.

Kafka acts as a durable traffic buffer between the API layer and database persistence.

This provides:

- Burst traffic buffering
- Improved throughput
- Reduced database pressure
- Better scalability under spikes
- Parallel event processing

Kafka events are partitioned using:

```text
productId
```

to preserve ordering per product while enabling parallel processing across partitions.

---

## Why NOT Direct Synchronous Persistence?

Synchronous database writes were rejected because they:

- Increased API latency
- Exhausted database connections
- Reduced throughput under concurrency
- Scaled poorly during burst traffic

---

## Why NOT RabbitMQ?

RabbitMQ is excellent for traditional task queues, but Kafka was better aligned with the system requirements:

- High-throughput event streaming
- Replay capability
- Partition-based ordering
- Burst traffic scalability

Kafka also provided stronger support for traffic buffering during flash-sale spikes.

---

## Tradeoffs

| Benefit | Tradeoff |
|---|---|
| High throughput | Eventual consistency |
| Burst traffic handling | Operational complexity |
| Better scalability | Async recovery requirements |

---

# 4. Eventual Consistency Model

## Decision

Adopt an eventual consistency model for order processing.

---

## Why?

Strong synchronous consistency significantly reduced throughput during concurrency testing.

Eventual consistency enables:

- Faster API responses
- Higher throughput
- Better scalability
- Improved traffic absorption

Purchase requests return:

```text
HTTP 202 Accepted
```

after successful validation and Kafka event publication.

Order persistence occurs asynchronously.

---

## Tradeoffs

| Benefit | Tradeoff |
|---|---|
| Faster request handling | Temporary inconsistency |
| Higher throughput | Recovery complexity |
| Better scalability | Async coordination overhead |

---

# 5. Compensation-Based Recovery Strategy

## Decision

Implement retry, DLQ, and compensation-based recovery workflows.

---

## Why?

Distributed systems inevitably experience:

- Partial failures
- Transient infrastructure failures
- Network interruptions
- Consumer processing failures

Recovery workflows are required to preserve:

- Inventory correctness
- Order consistency
- Event reliability

---

## Recovery Components

### Producer Recovery

- Retry queue
- Redis-backed producer DLQ
- Scheduler-driven compensation workflows

---

### Consumer Recovery

- Kafka retry workflows
- Kafka Dead Letter Queue (DLQ)
- Scheduler-driven recovery processing

---

### Compensation Logic

If order persistence fails after inventory reservation:

```text
inventory += reserved_stock
```

This restores inventory consistency safely.

---

## Tradeoffs

| Benefit | Tradeoff |
|---|---|
| Improved fault tolerance | Increased operational complexity |
| Better consistency guarantees | More recovery orchestration |
| Safer failure handling | Additional background workflows |

---

# 6. Rate Limiting Strategy

## Decision

Use Bucket4j-based rate limiting for infrastructure protection.

---

## Why?

Load testing revealed that excessive retries and burst traffic increased:

- Infrastructure pressure
- Thread contention
- Request latency

Rate limiting stabilized traffic behavior during high-concurrency scenarios.

Current policy:

```text
5 requests / 10 seconds
```

---

## Tradeoffs

| Benefit | Tradeoff |
|---|---|
| Infrastructure protection | Potential request rejection |
| Improved latency stability | Additional request filtering |
| Reduced retry storms | Slightly stricter request control |

---

#  Summary

The architecture prioritizes:

- concurrency correctness
- asynchronous scalability
- fault tolerance
- operational simplicity
- infrastructure protection
- distributed observability

while balancing tradeoffs between performance, consistency, and operational complexity.