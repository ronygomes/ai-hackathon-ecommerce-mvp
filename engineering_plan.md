# Distributed System Engineering Plan (Microservice Edition)

This plan outlines the architectural design for a distributed system based on Domain-Driven Design (DDD), CQRS, Clean Architecture, and a multi-process microservice structure.

> [!IMPORTANT]
> **Source of Truth Clarification**: Per the primary objective, this system uses a **State-Based Source of Truth**. Aggregates are persisted as complete state snapshots in a database. Domain Events are used for cross-subsystem integration and projection into read models, but NOT for reconstructing aggregate state (no Event Sourcing).

---

## A. Architecture Overview
The system is a **microservice-based distributed system** where each subsystem represents a Bounded Context (DDD) and is deployed independently.

### 1. Macro Architecture
- **Microservices**: Each subsystem is independent.
- **Multi-Process Subsystems**: Every subsystem is split into separate deployable units (containers/executables) to isolate concerns and scale independently.
- **Asynchronous Communication**: 
  - Commands flow via RabbitMQ command queues.
  - Events propagate via RabbitMQ broadcast/fanout exchanges.
- **SAGA Coordination**: Cross-subsystem workflows are managed by dedicated SAGA Orchestrator processes.

### 2. Layers (Internal to each Process)
- **Domain Layer**: Pure business logic (Aggregates, Entities, VOs, Events).
- **Application Layer**: Use cases, Handlers, and Port interfaces.
- **Infrastructure Layer**: DB, Messaging, and External API implementations.

---

## B. Standard Subsystem Blueprint (The Four Processes)
Each subsystem must deploy at least these four distinct processes.

### 1. Command API Process (Ingress)
- **Purpose**: Exposes REST endpoints for write requests.
- **Responsibilities**:
  - AuthN/AuthZ and tenant context extraction.
  - Boundary validation (DTO shape and constraints).
  - Metadata generation (CorrelationId, CausationId, UserId).
  - Enqueue command to the subsystem's RabbitMQ command queue.
  - Return `202 Accepted` + tracking ID.
- **Non-Responsibilities**: No domain logic, no direct DB writes.

### 2. CommandHandler Process (Write Model Executor)
- **Purpose**: Executes domain behavior from the command queue.
- **Responsibilities**:
  - Consume commands from RabbitMQ.
  - Idempotency check (De-duplication using a `ProcessedCommand` marker).
  - Load Aggregate state from state store.
  - Execute behavior and produce Domain Events.
  - Save state and publish events (Outbox pattern recommended).
- **Non-Responsibilities**: No query APIs, no read-model generation.

### 3. EventHandler Process (Projection/Integration)
- **Purpose**: Updates read models and handles side effects.
- **Responsibilities**:
  - Subscribe to RabbitMQ broadcast exchange.
  - Update denormalized Read Models in MongoDB.
  - Optionally publish "Integration Events" for public consumption.
- **Non-Responsibilities**: No aggregate writes, no command endpoints.

### 4. Query API Process (Read Model Service)
- **Purpose**: Exposes queryable REST endpoints.
- **Responsibilities**:
  - Query MongoDB read collections only.
  - Apply row-level/tenant security filters.
  - Handle filtering, paging, sorting.
- **Non-Responsibilities**: No command handling, no event consumption.

---

## C. Cross-Subsystem SAGA Process (Orchestrator)
Separate processes per "business workflow family" to coordinate multi-step processes.

### 1. Responsibilities
- **Listen**: Subscribe to events from multiple subsystems.
- **State Management**: Persist SAGA state in MongoDB (one document per instance).
  - Fields: `SagaId`, `CorrelationId`, `CurrentStep`, `Status`, `Timeouts`, `Compensations`.
- **Act**: Emit commands to appropriate subsystem command queues based on the state machine logic.
- **Error Handling**: Manage timeouts, retries, and trigger compensation commands for rollbacks.

### 2. Boundary Rule
- SAGA is for **orchestration**, not business logic.
- Invariants remain in Aggregates; SAGA only encodes "what happens next" policies.

---

## D. Core Abstractions
Defined as generic interfaces in the Application layer.
- `ICommand<TResult>`, `ICommandHandler<TCommand, TResult>`
- `IAggregateRoot<TId>`, `IDomainEvent`, `EventEnvelope<T>`
- `IRepository<TAggregate, TId>`, `IMessageBus`
- `ISagaState<TData>`, `ISagaOrchestrator<TEvent>`

---

## E. Command Handling Lifecycle (Multi-Process)
1. **Command API**: Validates and enqueues to RabbitMQ.
2. **CommandHandler**: Pops command, checks idempotency.
3. **Domain Logic**: Load Aggregate state → Execute → Produce Events.
4. **Persistence**: Atomic Save (State + Outbox).
5. **Publish**: Outbox processor emits events to broadcast exchange.

---

## F. Event Handling & Data Storage
- **Command DB**: State + Outbox (PostgreSQL).
- **Read DB**: Denormalized Query Collections (MongoDB).
- **Saga DB**: Workflow state (MongoDB).

---

## G. Observability & Security
- **Tracing**: Mandatory `CorrelationId` across all processes (API → Queue → Handler → Event → Saga).
- **Metrics**: Monitor queue depth per process and SAGA completion rates.
- **Security**: JWT tokens propagated/validated at both API layers (Command/Query).

---

## H. Testing Strategy
- **Process Isolation Tests**: Test each process (API, Handler, Projector) in isolation.
- **SAGA Simulation**: Unit test SAGA state transitions without real messaging.
- **Integration Tests**: Verify RabbitMQ routing across multiple subsystem processes.

---

## I. Deliverables Checklist
1. **Subsystem Process Map**: Logical deployment units per bounded context.
2. **SAGA State Machines**: Diagrams for cross-subsystem workflows.
3. **Queue/Exchange Topology**: RabbitMQ naming and routing conventions.
4. **Command/Event Catalog**: Registry of all messages.
