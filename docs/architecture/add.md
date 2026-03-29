# 📄 Architecture Decision Document (ADD)

---

## AD-00: Architecture Style Selection

### Decision
Adopt an **event-driven modular monolith architecture**.

---

### Problem
Flash sale systems must handle:
- Extremely high concurrency
- Prevention of overselling
- Low latency requirements
- Controlled database load

---

### Rationale

#### Modular Monolith
- Faster development and simpler deployment
- Clear domain separation (Product, Sale, Order modules)

#### Event-Driven Approach
- Decouples request handling from order processing
- Absorbs traffic spikes using Kafka
- Enables asynchronous and scalable processing

---

### Why NOT Microservices

Microservices architecture was rejected due to:

- **Premature Complexity**  
  The current system scope does not justify distributed system overhead

- **Operational Overhead**  
  Requires:
   - API Gateway
   - Service discovery
   - Distributed tracing
   - Complex deployment pipelines

- **Distributed Consistency Challenges**  
  Requires Saga pattern → increases complexity

- **Team Size Constraint**  
  Not suitable for a single-developer project

- **Network Latency**  
  Inter-service communication adds latency (critical in flash sale systems)

---

### Why NOT Layered Monolith

A traditional synchronous layered monolith was rejected due to:

- **Tight Coupling Between Layers**  
  Hard to isolate high-load components

- **Database Bottleneck**
   - Lock contention
   - Connection pool exhaustion
   - Increased latency

- **Lack of Asynchronous Processing**  
  Cannot absorb traffic spikes

- **Poor Load Handling**  
  No buffering between API and DB

- **Limited Scalability**  
  Requires scaling entire application

---

## AD-01: Redis for Inventory Management

### Decision
Use Redis for stock validation and decrement.

---

### Problem
Relational databases suffer from:
- Lock contention
- Slow writes under high concurrency

---

### Rationale
Redis provides:
- In-memory speed
- Atomic operations
- Reduced database load

---

### Trade-offs
- High performance
- Requires synchronization with database

---

## AD-02: Atomic Stock Operation

### Decision
Use Redis atomic operations (**Lua script preferred**).

---

### Problem
Concurrent requests → race conditions → overselling

---

### Rationale
Atomic execution ensures:
- Check + decrement in one step
- No inconsistent stock

---

### Trade-offs
- Strong correctness
- Slight complexity with Lua

---

## AD-03: Asynchronous Order Processing

### Decision
Use Kafka for asynchronous order processing.

---

### Problem
Direct database writes cannot handle flash sale spikes.

---

### Rationale
Kafka:
- Buffers burst traffic
- Decouples API from database
- Smooths write load

Partitioning by `productId`:
- Preserves order per product
- Enables parallel processing

---

### Trade-offs
- High scalability
- Eventual consistency

---

## AD-04: Message Broker Selection (Kafka)

### Decision
Use Kafka as the message broker.

---

### Rationale
- High throughput
- Partition-based ordering
- Replay capability
- Durability

---

### Alternatives
- RabbitMQ → simpler but lower throughput at scale

---

### Trade-offs
- High scalability
- Increased operational complexity

---

## AD-05: Idempotency Strategy

### Decision
Ensure **one order per user per product**.

---

### Implementation
- Redis check → fast rejection
- Database unique constraint → final guarantee

---

### Rationale
Prevents duplicate orders under retries and concurrent requests.

---

### Trade-offs
- Strong correctness
- Slight overhead

---

## AD-06: Consistency Model

### Decision
Use **eventual consistency**.

---

### Rationale
- Improves throughput
- Reduces latency
- Supports asynchronous processing

---

### Trade-offs
- Temporary inconsistency
- Requires compensation logic

---

## AD-07: Failure Handling

### Decision
Implement a **compensation mechanism**.

---

### Problem
Redis decrement succeeds but database write fails → stock inconsistency

---

### Solution
- Increment stock back in Redis (compensation action)
- Mark order as **FAILED**

---

### Trade-offs
- Ensures consistency
- Adds complexity

---

## AD-08: API Response Strategy

### Decision
Return **HTTP 202 Accepted** for order placement.

---

### Rationale
- Request validated → pushed to Kafka → return immediately
- Reduces latency
- Avoids blocking

---

### Trade-offs
- Client must poll for order status
- Eventual consistency visible to users

---

## AD-09: Internal Architecture Pattern

### Decision
Use a **modular layered architecture** with clear boundaries.

---

### Rationale
- Separates business logic from infrastructure
- Keeps implementation simple
- Avoids over-engineering

---

### Trade-offs
- Less strict than hexagonal architecture
- Easier to maintain  