# System Architecture

This document describes the overall architecture, request lifecycle, asynchronous processing flow, recovery mechanisms, and infrastructure design of the Flash Sale System.

---

#  System Overview

The system is designed to handle high-concurrency flash-sale traffic while ensuring:

- Inventory correctness
- High throughput
- Controlled database load
- Failure recovery
- Distributed observability

The architecture combines:

- Modular Monolith design
- Event-driven asynchronous processing
- Redis-based concurrency control
- Kafka-based traffic buffering
- Distributed monitoring and tracing

---

#  High-Level Architecture

![Architecture Diagram](images/architecture-diagram.jpeg)

---

#  Architectural Style

The system follows a **Modular Monolith** architecture with clear domain boundaries:

- Product Module
- Sale Module
- Order Module
- User Module

The Order module contains the core high-concurrency and asynchronous processing logic.

Internally, the system follows a lightweight **Hexagonal Architecture (Ports & Adapters)** approach to separate:

- Business logic
- Infrastructure concerns
- External integrations

This improves:

- Maintainability
- Testability
- Separation of concerns

while avoiding the operational complexity of microservices.

---

#  Request Lifecycle

```text
Client
  │
  ▼
Spring Boot API
  │
  ▼
Redis Lua Script
(atomic inventory reservation)
  │
  ▼
Kafka Producer
(OrderPlacedEvent)
  │
  ▼
Kafka Broker (6 Partitions)
  │
  ▼
Kafka Consumer
  ├──► PostgreSQL
  ├──► Retry Logic
  └──► Dead Letter Queue
```

---

#  Purchase Flow

![Purchase Flow](images/purchase-flow.jpeg)

## Flow Summary

1. User submits purchase request
2. User authenticated via Keycloak
3. Redis Lua script atomically reserves inventory
4. `OrderPlacedEvent` published to Kafka
5. API immediately returns `HTTP 202 Accepted`
6. Kafka consumer processes order asynchronously
7. Order persisted to PostgreSQL
8. Recovery workflows preserve consistency during failures

---

#  Inventory Management

Redis is used as the high-performance concurrency layer for inventory operations.

## Responsibilities

- Inventory validation
- Atomic stock reservation
- Duplicate request protection
- Rate limiting support
- Hot-path caching

---

## Atomic Inventory Reservation

Inventory reservation uses Redis Lua scripting to ensure atomic execution.

```text
check stock → decrement stock
```

Both operations execute as a single atomic operation, preventing overselling during concurrent purchase attempts.

---

#  Asynchronous Order Processing

Kafka is used to decouple API traffic from database writes.

## Benefits

- Burst traffic buffering
- Reduced database pressure
- Improved throughput
- Asynchronous scalability
- Ordered event processing

Kafka events are partitioned using:

```text
productId
```

to preserve ordering per product while enabling parallel processing across partitions.

---

#  Idempotency Strategy

Flash-sale systems must prevent duplicate order creation under retries and concurrent requests.

The system uses multiple protection layers:

## Redis Validation

Fast duplicate rejection before event publication.

---

## Database Constraints

Database uniqueness constraints provide the final consistency guarantee.

---

## Event Tracking

Processed events are tracked to prevent duplicate event execution during retries or recovery workflows.

---

#  Failure Recovery Architecture

The system includes multiple retry and recovery workflows to preserve consistency during failures.

## Recovery Components

- Producer retry queue
- Redis-backed producer DLQ
- Kafka consumer retry
- Kafka Dead Letter Queue (DLQ)
- Compensation-based stock restoration
- Scheduler-driven recovery workflows

Detailed recovery workflows are documented in:

```text
recovery-strategy.md
```

---

# ️ Redis Recovery Strategy

Redis is treated as a high-performance concurrency layer — not the source of truth.

Recovery mechanisms include:

- Redis AOF persistence
- Lazy rebuild using persisted order data
- Recalculation using persisted order data

```text
remaining_stock = initial_stock - confirmed_orders
```

This ensures inventory consistency after Redis restart or cache loss.

---

#  Observability Architecture

The system integrates distributed observability tooling for monitoring, tracing, and bottleneck analysis.

## Stack

- Prometheus
- Grafana
- OpenTelemetry
- Tempo

---

## Monitoring Dashboard

![Grafana Dashboard](images/grafana-dashboard.png)

Monitored metrics include:

- API latency
- Request throughput
- Error rates
- JVM memory usage
- Infrastructure resource utilization

---

## Distributed Tracing

![Distributed Trace](images/distributed-trace.png)

Distributed tracing was used to analyze asynchronous request flow and identify latency bottlenecks during high-concurrency scenarios.

---

#  Rate Limiting

Bucket4j-based rate limiting is used to protect infrastructure during burst traffic and excessive retries.

The current policy limits users to:

```text
5 requests / 10 seconds
```

This reduced infrastructure pressure and stabilized latency during load testing.

---

#  Load Testing & Bottleneck Analysis

Load testing was performed using k6 to simulate flash-sale traffic.

## Validated Scenarios

- Stress testing
- Spike testing
- Burst traffic simulation
- Flash-sale start concurrency validation
- Out-of-stock race condition testing
- Idempotency verification

---

## Performance Findings

### Redis Cache Optimization

Sale-data fetching initially introduced high latency during concurrency testing.

Redis cache-aside optimization significantly reduced hot-path latency under load.

---

### Infrastructure Bottlenecks

High-concurrency testing revealed:

- CPU saturation
- Thread contention
- Increased latency under excessive retries

These findings were used to improve:

- caching
- retry workflows
- rate limiting
- asynchronous processing stability

---

#  Deployment Architecture

Infrastructure is provisioned using Terraform-based Infrastructure as Code (IaC).

## Infrastructure Components

- AWS EC2
- AWS RDS PostgreSQL
- AWS ElastiCache Redis
- AWS ECR
- Kafka + Zookeeper
- Dockerized services

---

## Deployment Diagram

![Deployment Diagram](images/deployment-diagram.png)

---

#  Module Structure

```text
src/
└── main/
    └── java/
        └── com.flashsale.ordersystem/
            ├── order/
            │   ├── adapter/
            │   ├── domain/
            │   ├── port/
            │   ├── scheduler/
            │   └── service/
            │
            ├── product/
            ├── sale/
            ├── shared/
            └── user/
```

---

# 📌 Summary

The architecture focuses on solving real distributed systems problems including:

- concurrency control
- overselling prevention
- asynchronous scalability
- idempotency
- recovery orchestration
- infrastructure protection
- distributed observability
- failure handling

while maintaining operational simplicity through a modular monolith architecture.