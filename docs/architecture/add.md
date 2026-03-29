📄  ARCHITECTURE DECISION DOCUMENT

AD-00: Architecture Style Selection
Decision
Adopt an event-driven modular monolith architecture.

Problem
Flash sale systems must handle:
extremely high concurrency
prevention of overselling
low latency requirements
controlled database load

Rationale
A modular monolith provides:
faster development and simpler deployment
strong domain separation (Product, Sale, Order modules)
An event-driven approach:
decouples request handling from order processing
absorbs traffic spikes using Kafka
enables asynchronous and scalable processing

Why NOT Microservices
Microservices architecture was considered but rejected for the following reasons:

1. Premature Complexity
   The current system scope does not justify distributed system overhead.

2. Operational Overhead
   Requires service discovery, API gateway, distributed tracing,
   inter-service communication, and complex deployment pipelines.

3. Distributed Consistency Challenges
   Handling transactions across services would require patterns like Saga,
   increasing implementation complexity.

4. Team Size Constraint
   Microservices are effective with larger teams; for a single-developer project,
   a modular monolith provides better productivity.

5. Network Latency
   Inter-service communication adds latency, which is critical in flash sale scenarios.

Why Not Layered Monolith
A traditional layered monolith (synchronous architecture) was rejected due to scalability limitations under high concurrency.

1. Tight Coupling Between Layers
   Controllers directly invoke services and repositories synchronously,
   making it difficult to isolate high-load components like order processing.

2. Database Bottleneck
   All requests result in immediate database writes.
   Under flash sale conditions (e.g., thousands of concurrent users),
   this leads to:
- lock contention
- connection pool exhaustion
- increased latency

3. Lack of Asynchronous Processing
   Synchronous request-response flow cannot absorb traffic spikes.
   This increases the risk of system failure during peak load.

4. Poor Load Handling
   No buffering mechanism exists between request intake and persistence,
   leading to uneven load distribution.

5. Limited Scalability
   Scaling requires scaling the entire application,
   rather than isolating critical components.

AD-01: Redis for Inventory Management
Decision
Use Redis for stock validation and decrement.

Problem
Relational databases suffer from:
lock contention
slow writes under high concurrency

Rationale
Redis provides:
in-memory speed
atomic operations
reduced DB load

Trade-offs
High performance
Requires synchronization with database

AD-02: Atomic Stock Operation
Decision
Use Redis atomic operations (Lua script).

Problem
Concurrent requests → race conditions → overselling

Rationale
Atomic execution ensures:
check + decrement in one step
no inconsistent stock

Trade-offs
Strong correctness
Slight complexity with Lua

AD-03: Asynchronous Order Processing
Decision
Use Kafka for async order processing.

Problem
Direct DB writes cannot handle flash sale spikes.

Rationale
Kafka:
buffers burst traffic
decouples API from DB
smooths write load
Partitioning by productId:
preserves order per product
enables parallel processing

Trade-offs
High scalability
Eventual consistency

AD-04: Message Broker Selection (Kafka)
Decision
Use Kafka.

Rationale
high throughput
partition-based ordering
replay capability
durability

Alternatives
RabbitMQ → simpler but lower throughput at scale

Trade-offs
scalable
operational complexity

AD-05: Idempotency Strategy
Decision
Ensure one order per user per product.

Implementation
Redis check → fast rejection
DB unique constraint → final guarantee

Rationale
Prevents duplicate orders under retries and concurrent requests.

Trade-offs
Strong correctness
Slight overhead

AD-06: Consistency Model
Decision
Use eventual consistency

Rationale
improves throughput
reduces latency
supports async processing

Trade-offs
temporary inconsistency
requires compensation logic

AD-07: Failure Handling
Decision
Implement compensation mechanism.

Problem
Redis decrement succeeds but DB write fails → stock lost

Solution
increment stock back in Redis
mark order as FAILED

Trade-offs
ensures consistency
adds complexity
AD-08: API Response Strategy
Decision
Return HTTP 202 Accepted for order placement.

Rationale
request validated → pushed to Kafka → return immediately
reduces latency
avoids blocking

Trade-offs
client must poll for status
eventual consistency visible to users

AD-09: Internal Architecture Pattern
Decision
Use layered architecture with clear module boundaries.

Rationale
separates business logic from infrastructure
keeps implementation simple
avoids over-engineering

Trade-offs
less strict than hexagonal
easier to maintain


