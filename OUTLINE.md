# Distributed System with AI Hackathon

**Course Outline**: [Original Link][1]

A 2-day, fully hands-on hackathon where teams will build an MVP e-commerce platform using a modern
DDD + CQRS + Event Sourcing architecture. Participants will implement the full workflow end-to-end: 
product catalog, pricing, inventory, cart, order placement, payments, and fulfillment, with a 
reliable outbox pattern and RabbitMQ event-driven integration.

* This isn’t “developers coding faster.”
* This is AI agents collaborating with engineers to design and implement a production-grade architecture in days, not weeks.
* What we’ll build (MVP): Catalog, Pricing, Inventory, Cart, Orders, Payments, and Fulfillment
* How we’ll build it: DDD + CQRS + Event Sourcing, Outbox Pattern, and RabbitMQ for event-driven integration

What makes it different: We’ll use AI coding agents to accelerate every step, requirements breakdown, bounded-context 
design, aggregate modeling, event design, API contracts, test scaffolding, and implementation.

* Cleaner boundaries
* Fewer architectural mistakes
* Higher test coverage
* Safer refactoring
* and much faster iteration.

## Why does this matter for businesses?

Because the same approach enables teams to move from a traditional, tightly-coupled monolith to a scalable, fault-tolerant distributed system, where services evolve independently, failures are isolated, and new features can be shipped without breaking the whole platform.
This is where software development is heading:
Human architecture + AI acceleration + event-driven systems = speed and resilience.

## Subsystem to build

1. **User & Admin Access**
   * Customer signup/login
   * Admin login
   * Basic roles: Customer, Admin

2. **Customer Profile & Addresses**
   * Minimal profile (name, phone/email)
   * Add/update/delete delivery address
   * Set default address

3. **Product Catalog**
   * Browse product list + product details
   * Admin can create/update product and publish/unpublish
   * Product images (URL) + category (single)

4. **Pricing**
    * Set a price per product (single currency)
    * Show price everywhere (catalog/cart/order)
   
5. **Inventory**
   * Track stock per product (single warehouse)
   * Prevent overselling
   * Reserve stock when order is placed
   * Release reservation on cancel/payment failure
   
6. **Cart**
   * One active cart per customer
   * Add/remove items, change quantity
   * Show totals (using pricing) + availability check (using inventory)

7. **Orders**
   * Place order from cart + selected address
   * Order statuses: PendingPayment, Confirmed, Cancelled
   * Customer can view order list/details
   * Admin can view/manage orders

8. **Payments**
   * MVP option A (simplest): **Cash on Delivery**
   * MVP option B: One online gateway with success/fail callback
   * Record payment status per order
   
9. **Fulfillment / Shipping**
   * Create shipment after order is confirmed
   * Shipment statuses: ReadyToShip, Shipped, Delivered
   * Tracking number (manual allowed)

10. **Support (Lightweight)**
    * Customer can open a ticket for an order 
    * Ticket statuses: Open, Resolved 
    * Add messages/notes

## Architecture goals

### Hard requirements

* **DDD:**
  * Model each subsystem as one **Bounded Context** with explicit boundaries.
  * Business rules must live in the **Domain layer**, primarily inside **Aggregate Roots**.
  * Aggregates must protect invariants and define transactional consistency boundaries.
  * Domain must be persistence-ignorant (no EF, no RabbitMQ, no DB calls in Domain).

* **CQRS:**
  * Separate **Command/Write model** from **Query/Read model**.
  * Commands mutate state and emit events; Queries never mutate state.
  * Read side is **eventually consistent** and built from events.

* **Event Sourcing:**
  * Aggregate state is derived by **replaying events** (or from snapshots + events).
  * Every state change must produce one or more **Domain Events**.
  * The source of truth for write-side is the **event stream**, not the read DB.

* **Outbox Pattern:**
  * Persist events in an **Outbox** table/collection in the **same transaction** as write-side persistence.
  * Publishing to RabbitMQ must be **asynchronous** from the transaction (outbox dispatcher), ensuring reliability.

* **Messaging:**
  * Publish events to **RabbitMQ broadcast (fanout) exchange**.
  * Events must be versioned; consumers must be backward-compatible.
  * Event handlers must be idempotent and resilient.

* **Interfaces + Generics:**
  * All key components must be defined via **interfaces**.
  * Use **generic interfaces** for commands, handlers, repositories, event store, unit of work, outbox, etc.
  * Avoid service-locator patterns; prefer DI composition.


[1]: https://docs.google.com/document/d/1Vhc8NfT0mFhCdYb7X6r12t_QDiXOFgv_86vw9hRy378/edit?tab=t.0
