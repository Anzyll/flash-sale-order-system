# ⚡ Flash Sale System — High-Concurrency Backend

> A production-oriented backend built to handle extreme concurrency during flash sales, ensuring **zero overselling**, fault tolerance, and automatic system recovery.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot)
![Redis](https://img.shields.io/badge/Redis-Lua_Scripting-DC382D?style=flat-square&logo=redis)
![Kafka](https://img.shields.io/badge/Apache_Kafka-Event_Driven-231F20?style=flat-square&logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Source_of_Truth-4169E1?style=flat-square&logo=postgresql)
![Keycloak](https://img.shields.io/badge/Keycloak-OAuth2_/_JWT-4D4D4D?style=flat-square&logo=keycloak)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)

---

## 📌 Problem Statement

Flash sales trigger sudden traffic spikes where thousands of users attempt to purchase limited stock simultaneously. Without proper system design, this leads to:

- **Overselling** due to race conditions
- **Database overload** from synchronous writes
- **Duplicate orders** caused by retries
- **System inconsistency** during failures

---

## 🎯 Design Goals

This system is designed with production-level considerations:

- Handle **high concurrency** without overselling
- Ensure **fault tolerance** using retry and DLQ
- Maintain **data consistency** with idempotency
- Support **event-driven scalability** using Kafka
- Recover safely from failures (Redis cache rebuild)


## 💡 Solution Overview

| Layer | Technology | Role |
|---|---|---|
| Concurrency Control | Redis + Lua | Atomic stock check & prevent overselling |
| Traffic Buffer | Apache Kafka | Async processing at scale |
| Source of Truth | PostgreSQL + Flyway | Durable order persistence |
| Idempotency | Custom token logic | Prevent duplicate orders |
| Fault Tolerance | Retry + DLQ | Handle consumer failures |
| Redis State Recovery | Rebuild Redis from DB on startup | Ensures consistency after Redis failure |
| Auth | Keycloak (OAuth2/JWT) | Role-based access control |

---

## 🏗️ Architecture

**Style:** Modular Monolith + Hexagonal (Order Module) + Event-Driven

```
Client
  └──► Spring Boot Api
         └──► Redis Lua Script (Atomic stock check + decrement)
                └──► Kafka Producer (OrderPlacedEvent)
                       └──► Kafka Consumer
                              ├──► PostgreSQL (persist order)
                              └──► Retry → Dead Letter Queue (on failure)
```

---

## 🔥 Key Features

### ⚡ Concurrency Control
- Redis Lua script performs **atomic check + decrement** in a single operation
- Eliminates race conditions at the cache layer
- Guarantees **zero overselling** under concurrent load

### 📡 Event-Driven Processing
- Kafka producer publishes `OrderPlacedEvent`
- Partitioned by `productId` for ordered processing guarantees
- API returns `HTTP 202 Accepted` — fully async

### 🔁 Fault Tolerance
- Kafka consumer retry with exponential backoff
- Dead Letter Topic (DLT) for failed events
- **Idempotent consumers** — safe for reprocessing
- Compensation logic restores stock on confirmed failure

### ♻️ Redis Recovery Mechanism
Redis is used as an in-memory concurrency control layer, **not as the source of truth**.

On application restart or Redis failure:
1. Active sales are loaded from PostgreSQL
2. Stock is recomputed: `stock = initial_stock - confirmed_orders`
3. Redis keys are rebuilt: `stock:<saleId>:<productId>`, `sale_active:<saleId>`

This ensures full system consistency even after complete cache loss.

### 🔐 Security
- Keycloak-based authentication and authorization
- Role-based access: `ADMIN` / `CUSTOMER`
- Token-based security via OAuth2 / JWT

---

## 🔄 Purchase Flow

```
1. User sends purchase request
2. Request authenticated via Keycloak (JWT validation)
3. Sale & business rules validated
4. Redis Lua script: atomic stock check + decrement
5. Success → OrderPlacedEvent published to Kafka
6. API responds: HTTP 202 Accepted
7. Kafka consumer processes event asynchronously
8. Order persisted to PostgreSQL
9. On failure → retry with backoff → DLQ
```

---

## 🔄 Order Lifecycle

```
PENDING ──► CONFIRMED
   └──────► FAILED ──► EXPIRED
```

---

## 🧱 Module Structure

```
src/
├── modules/
│   ├── product/
│   ├── sale/
│   └── order/
│       ├── application/       # Use cases, ports (Hexagonal)
│       ├── domain/            # Business logic & entities
│       ├── infrastructure/    # Redis, Kafka, DB adapters
│       └── presentation/      # Controllers, DTOs, request/response
├── common/                    # Shared config, exceptions, utilities
└── config/

docs/
├── purchase-flow.jpeg         # Purchase flow Diagram
├── ADD.md                     # Architecture Decision Document
└── er-diagram.png             # Entity Relationship Diagram
```

---

## 🧰 Tech Stack

| Category | Technology        |
|---|-------------------|
| Language | Java 17           |
| Framework | Spring Boot 3.x   |
| Security | Keycloak (OAuth2) |
| Database | PostgreSQL        |
| Migrations | Flyway            |
| In-Memory Store | Redis (Atomic Lua scripting for concurrency control) |
| Messaging | Apache Kafka      |
| Containerization | Docker + Docker Compose |



## ▶️ Getting Started

### Prerequisites

Make sure Docker and Docker Compose are installed:

```bash
docker --version
docker compose version

### Run the system

```bash
git clone https://github.com/your-username/flash-sale-system.git
cd flash-sale-system
docker-compose up --build
```

This starts:

| Service | Port |
|---|---|
| Spring Boot App | `8080` |
| PostgreSQL | `5432` |
| Redis | `6379` |
| Kafka | `9092` |
| Keycloak | `8180` |

---

## 📊 Progress

| Status | Feature |
|---|---|
| ✅ | System Design & Core APIs |
| ✅ | Keycloak Authentication |
| ✅ | Redis Atomic Stock Handling (Lua) |
| ✅ | Kafka Event-Driven Processing |
| ✅ | Idempotency |
| ✅ | Retry & Dead Letter Queue (DLQ) |
| ✅ | Redis Recovery Mechanism |
| ✅ | Dockerization |
| ⬜ | Load Testing & Stress Testing (k6) |
| ⬜ | Monitoring (Prometheus + Grafana) |
| ⬜ | Kubernetes Deployment & Orchestration |
| ⬜ | Deployment (AWS EC2 + RDS) |

---

## ⚠️ Current Limitations

- Load testing not yet completed (planned using k6)
- Concurrency validation under real traffic is pending (planned with k6)
- Monitoring and observability not yet integrated

---

## 🧪 Planned Enhancements

- [ ] High-concurrency load testing with **k6**
- [ ] Redis recovery validation under failure scenarios
- [ ] **Prometheus + Grafana** dashboards
- [ ] Rate limiting (Redis-based token bucket)
- [ ] **AWS deployment** (EC2 + RDS)

---

## 🚧 Future Scope

- Microservices migration
- Real-time notifications
- Payment integration
- Advanced analytics via Kafka Streams

## 🎯 Design Highlights

| Concern | Solution |
|---|---|
| designed to prevent overselling | Redis Lua atomic execution |
| Async scalability | Apache Kafka |
| Resilient messaging | Retry + Dead Letter Queue |
| Cache resilience | Redis rebuild from PostgreSQL |
| Clean architecture | Modular Monolith + Hexagonal |
| Portability | Docker Compose |

---

## 📖 Documentation

See the [`/docs`](./docs) directory for:

- **Architecture Diagram** — Architecture Diagram
- **ADD** — Architecture Decision Document
- **ER Diagram** — Database schema and relationships
- **Flow Diagram** — Purchase Flow Diagram
---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## 👨‍💻 Author

**Muhammed Anzil M**  
Backend Developer — Java · System Design · Distributed Systems

---

> This project tackles real-world backend challenges: high concurrency handling, distributed system reliability, event-driven architecture, and fault tolerance — all in one cohesive system.