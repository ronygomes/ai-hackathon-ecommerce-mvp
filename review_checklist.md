# Architectural Review Checklist

Use this checklist to verify that a subsystem or feature implementation adheres to the architectural standards.

## 1. Clean Architecture & Process Separation
- [ ] **Dependency Direction**: Does the Domain layer have zero dependencies on Infrastructure/API layers?
- [ ] **Process Boundaries**: Are Command API, CommandHandler, EventHandler, and QueryAPI deployed as separate processes?
- [ ] **Ingress Responsibility**: Does the Command API ONLY validate and enqueue? (No DB touches, no logic).
- [ ] **Interface Isolation**: Are all RabbitMQ and DB interactions handled via Application layer interfaces?

## 2. DDD & State-Based Integrity
- [ ] **Aggregate Boundaries**: Does the aggregate manage its own state?
- [ ] **State-Based Truth**: Is the aggregate loaded from the state database, NOT an event stream?
- [ ] **Ubiquitous Language**: Do class/method names align with the domain glossary?

## 3. CQRS & Read Models
- [ ] **No Direct Read**: Does the CommandHandler avoid reading from the Query/Read database?
- [ ] **Independency**: Does the Query API read ONLY from MongoDB denormalized collections?
- [ ] **Projection Idempotency**: Can the projection handler process duplicate events safely?

## 4. Messaging & SAGA Orchestration
- [ ] **Outbox Pattern**: Are state changes and event creation atomicity guaranteed?
- [ ] **SAGA Isolation**: Does the SAGA process handle ONLY orchestration (emit commands) and not business invariants?
- [ ] **SAGA Idempotency**: Is the SAGA state update idempotent against duplicate event reception?
- [ ] **Correlation IDs**: Does every message (Command/Event/Salsa) carry a mandatory `CorrelationId` and `CausationId`?
- [ ] **Failover**: Are compensation commands defined for multi-step workflow failures?

## 5. Security & Observability
- [ ] **Tenant Isolation**: Is `TenantId` enforced in all DB queries and command metadata?
- [ ] **Tracing**: Can a request be traced across all 4+ processes via the same `CorrelationId`?
- [ ] **Auditability**: Are all state-changing actions captured as events on the broadcast exchange?
