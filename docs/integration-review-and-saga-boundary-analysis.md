# Team-Wide Integration Review and Saga Boundary Analysis

**Review basis:** current source tree on the main branch at review time. Existing documentation is treated as context only when it agrees with code; source code controls all current-state conclusions. This document is analysis and design guidance only. No Java, SQL, configuration, Docker, Maven, API, schema, Kafka, Saga, or test changes are included.

## Executive Summary

The current purchase path is a synchronous, Order-led workflow:

```text
Client -> API Gateway -> Order
                         -> User (verify)
                         -> Cart (read active cart)
                         -> Product (validate every product)
                         -> Inventory (reserve every item)
                         -> local Order persistence
                         -> Payment (create mock payment)
```

The path does not call Notification. It does not confirm successful Inventory reservations. It does not persist reservation IDs in Order data. It leaves a successfully paid Order in `PENDING`, because Payment success is not translated into an Order status update. Order cancellation only changes the local Order status; it does not release Inventory or refund Payment.

No Kafka or event-driven code exists in the current source. No correlation ID, idempotency key, durable retry, outbox, consumer, or retry/dead-letter implementation exists.

The recommended future boundary is an Order-centered orchestration Saga with Order as the business-process owner, Inventory and Payment as transactional participants, and Notification as an asynchronous downstream consumer after business completion. Product and Cart remain supporting read/validation services outside the transaction. User remains the identity authority and is not a purchase Saga participant.

## 1. Current Service Inventory

### 1.1 Runtime topology

| Service | Port | Database/schema | Primary responsibility |
|---|---:|---|---|
| API Gateway | 8080 | None | External HTTP routing and management endpoints |
| User Service | 8081 | PostgreSQL `user_schema` | Registration, authentication, users, roles, account status |
| Product Service | 8082 | PostgreSQL `product_schema` | Books, authors, categories, listings, loans, authoritative book price |
| Inventory Service | 8083 | PostgreSQL `inventory_schema` | Stock quantities and reservation lifecycle |
| Cart Service | 8084 | PostgreSQL `cart_schema` | Active carts and cart items |
| Order Service | 8085 | PostgreSQL `order_schema` | Orders, order items, order status, current synchronous checkout orchestration |
| Payment Service | 8086 | PostgreSQL `payment_schema` | Mock payment records and refund state transition |
| Notification Service | 8087 | PostgreSQL `notification_schema` | Notification records and immediate `SENT` state |

All services use a single PostgreSQL deployment with schema-per-service configuration. Cross-service foreign keys are not used. The services use schema validation rather than having each service own a shared table. The API Gateway has no business database.

### 1.2 API Gateway

**Responsibility.** The Gateway is the external entry point and forwards HTTP requests to services. The current YAML exposes management health/info endpoints. It does not contain business logic or visible JWT validation logic.

**Implemented routes.**

| Route predicate | Target |
|---|---|
| `/api/auth/**`, `/api/users/**` | User, `http://localhost:8081` |
| `/api/products/**`, `/api/books/**` | Product, `http://localhost:8082` |
| `/api/inventory/**` | Inventory, `http://localhost:8083` |
| `/api/carts/**` | Cart, `http://localhost:8084` |
| `/api/orders/**` | Order, `http://localhost:8085` |
| `/api/payments/**` | Payment, `http://localhost:8086` |
| `/api/notifications/**` | Notification, `http://localhost:8087` |

The current Gateway does not define routes for `/api/authors/**`, `/api/categories/**`, `/api/book-listings/**`, or `/api/loans/**`, despite those APIs existing in Product and being described by older documentation.

**Authentication and dependencies.** The Gateway forwards requests. There is no service-to-service call, local business state, status enum, or event code in the Gateway. Downstream services receive the request headers unless the Gateway configuration changes them.

**Error behavior.** Gateway-level routing/transport errors are handled by the gateway framework. Domain error behavior belongs to the target service. There is no current Saga or event error path here.

### 1.3 User Service

**Responsibility and data.** User owns registration, login, profiles, roles, and active/inactive account status. Its important tables/entities are `users` and `roles`; `User` owns the user UUID, identity data, password state, role, and active status.

**Important APIs.**

| Method | Endpoint | Behavior |
|---|---|---|
| POST | `/api/auth/register` | Creates a user and returns registration data |
| POST | `/api/auth/login` | Verifies credentials and active status; returns JWT/login data |
| GET | `/api/users/me` | Reads the authenticated user's profile |
| PUT | `/api/users/me` | Updates the authenticated user's profile |
| PUT | `/api/users/me/password` | Changes the authenticated user's password |
| GET | `/api/users/{id}` | Reads a user by UUID |
| GET | `/api/users` | Lists users, subject to service authorization |
| PUT | `/api/users/{id}/status` | Changes user status, subject to admin authorization |
| GET | `/api/internal/users/{id}/exists` | Internal user existence check |

**Authentication.** JWT subject is the user UUID. The token includes email, role, issue time, and expiry. User JWT validation checks signature, expiry, UUID subject, current user existence, and current active status. Authorization uses the current database role. The internal existence endpoint requires `X-Internal-Service-Secret`; Cart supplies this header when it checks a user.

**Dependencies.** User does not synchronously call another service. Its identity is consumed by Cart and Order. No event code exists.

**Statuses and errors.** The important account state is active/inactive. Registration and profile operations have duplicate, not-found, authentication, and authorization exceptions mapped by the User global handler. No distributed retry or idempotency behavior is present.

### 1.4 Product Service

**Responsibility and data.** Product owns catalog data and lending-related data. Important entities/tables are:

- `Book`: ISBN, title, description, author, category, and authoritative `price`.
- `Author` and `Category`.
- `BookListing`: owner, condition, fee, duration, and listing status.
- `Loan`: listing, borrower, fee, dates, and loan status.

**Important APIs.**

- Books: `POST/GET /api/books`, `GET/PUT/DELETE /api/books/{id}`.
- Authors: `POST/GET /api/authors`, `GET/PUT/DELETE /api/authors/{id}`.
- Categories: `POST/GET /api/categories`, `GET/PUT/DELETE /api/categories/{id}`.
- Listings: `POST/GET /api/book-listings`, `GET/PUT/DELETE /api/book-listings/{id}`, `GET /api/book-listings/book/{bookId}`, and `GET /api/book-listings/owner/{ownerId}`.
- Loans: `POST /api/loans`, `GET /api/loans/{id}`, borrower/listing queries, and activate/return/cancel operations.

The purchase flow uses `GET /api/books/{productId}`. Product is authoritative for product existence and the catalog book price. Current Order validates the product but persists the Cart item price, not the Product response price. Cart obtains and stores Product price when adding an item; the client-supplied price is not authoritative after Product validation.

**Dependencies.** Cart synchronously calls Product for product lookup. Inventory synchronously calls Product when creating an inventory row to validate that a product exists. Order synchronously calls Product once per Cart item during order creation. Product has static service URL configuration for other services but no demonstrated purchase-flow call to them. No event code exists.

**Statuses and errors.** Listing statuses are `AVAILABLE`, `BORROWED`, and `UNAVAILABLE`. Loan statuses are `REQUESTED`, `ACTIVE`, `RETURNED`, and `CANCELLED`. Resource-not-found, conflict, invalid state, and validation errors are mapped by the Product global handler. No retry/idempotency behavior is present.

### 1.5 Inventory Service

**Responsibility and data.** Inventory owns one inventory row per product and reservation records. Important entities/tables are `Inventory` and `Reservation`.

`Inventory` contains an inventory UUID, product UUID, total `quantity`, `reservedQuantity`, status, and timestamps. Available stock is calculated as:

```text
availableStock = quantity - reservedQuantity
```

Inventory status is `AVAILABLE` or `OUT_OF_STOCK` based on that calculation. `Reservation` contains `reservationId`, `productId`, optional `orderId`, reservation quantity, status, and timestamps. Reservation statuses are `ACTIVE`, `RELEASED`, and `CONFIRMED`.

**Important APIs.**

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/inventory` | Create inventory after Product existence validation |
| GET | `/api/inventory/{inventoryId}` | Read inventory by inventory UUID |
| GET | `/api/inventory/product/{productId}` | Read inventory by product UUID |
| PUT | `/api/inventory/stock/{productId}` | Set total stock |
| POST | `/api/inventory/stock/add/{productId}?quantity=...` | Add stock |
| POST | `/api/inventory/reserve` | Create an `ACTIVE` reservation |
| POST | `/api/inventory/release/{reservationId}` | Release one exact reservation |
| POST | `/api/inventory/confirm/{reservationId}` | Confirm one exact reservation |
| POST | `/api/inventory/release?productId=...&quantity=...` | Quantity-based release |
| POST | `/api/inventory/confirm?productId=...&quantity=...` | Quantity-based confirmation |

The reservation-ID endpoints are the only suitable exact-compensation boundary for a multi-item order. The quantity-based endpoints do not identify a particular reservation and are unsafe for distributed compensation when multiple orders reserve the same product.

**Concurrency.** Reservation, release, confirm, and stock updates use a database transaction and a locked inventory-row lookup (`findByProductIdForUpdate`). Reserve checks available stock, increments `reservedQuantity`, and creates an `ACTIVE` reservation. Release and confirm lock the reservation and then the inventory row. Confirm decreases both total and reserved quantity; release decreases only reserved quantity.

**Idempotency and errors.** There is no idempotency key. An `ACTIVE` reservation may transition once to `RELEASED` or `CONFIRMED`; repeated operations fail with an invalid reservation state. Missing inventory/reservation, insufficient stock, invalid reservation state, invalid quantities, and Product availability errors are mapped to structured 400/404/409/503-style responses by the local exception handler. Inventory itself has no durable retry or event consumer.

**Dependencies.** Inventory synchronously calls Product only during inventory creation. Reservation operations do not call Product. Order and Cart call Inventory. No event code exists.

### 1.6 Cart Service

**Responsibility and data.** Cart owns `Cart` and `CartItem`. Cart status is primarily `ACTIVE`. The active cart is found or created per user; cart items hold product UUID, quantity, and the Product-derived unit price.

**Important APIs.**

- `POST /api/carts/{userId}/active`: create or get an active cart.
- `GET /api/carts/{userId}/active`: read active cart.
- `POST /api/carts/{userId}/active/items`: add item.
- `PUT /api/carts/{userId}/active/items/{productId}`: set item quantity.
- `DELETE /api/carts/{userId}/active/items/{productId}`: remove item.
- `DELETE /api/carts/{userId}/active`: delete/clear active cart behavior.
- `GET /api/carts/{userId}/active/total`: read cart total.

**Synchronous dependencies.** Cart checks User existence through `GET /api/internal/users/{id}/exists` with `X-Internal-Service-Secret`. Cart reads Product through `GET /api/books/{productId}` and stores the returned price. Cart reads Inventory through `GET /api/inventory/product/{productId}` and checks requested quantity against `quantity - reservedQuantity`.

Cart never reserves, confirms, or releases stock. It has no checkout/finalization endpoint and no direct Order call. Order later reads the active cart. No event code exists.

**Authentication and errors.** The external Cart endpoints do not declare the internal header requirement. The Cart-to-User call uses the configured internal secret. Product/Inventory/User not-found, invalid input, insufficient stock, and transport failures are translated to Cart integration exceptions and local error responses. There is no durable retry or idempotency behavior.

### 1.7 Order Service

**Responsibility and data.** Order owns `Order`, `OrderItem`, `ShippingAddress`, and `Money` value data. `Order` persists UUID, user UUID, total, currency, status, shipping address, timestamps, and items. `OrderItem` persists product UUID, quantity, unit price, and subtotal. There is no persisted reservation ID, payment ID, Saga state, attempt number, or idempotency key.

**Important APIs.**

- `POST /api/orders`: create from the user's active Cart.
- `GET /api/orders`: list orders.
- `GET /api/orders/my`: current-user-oriented query path.
- `GET /api/orders/{orderId}`: read one order.
- `GET /api/orders/user/{userId}`: list by user.
- `PUT /api/orders/{orderId}/cancel`: set local status to cancelled.
- `PUT /api/orders/{orderId}/status`: set a supplied status.

**Statuses.** `PENDING`, `CREATED`, `CONFIRMED`, `PAID`, and `CANCELLED` exist. The current create flow sets `PENDING` and never updates it after Payment success.

**Authentication.** Order may validate the incoming Bearer JWT and compare its subject with `request.userId`; an ADMIN role may act for another user. The current request shape requires an explicit `userId` and shipping address. The Authorization header is forwarded by Order clients to User, Cart, Product, Inventory, and Payment when available. Downstream services do not all require that header. There is no internal service identity distinct from the end-user JWT for the Order calls.

**Dependencies.** Order synchronously calls User, Cart, Product, Inventory, and Payment. It does not call Notification. No event code exists.

### 1.8 Payment Service

**Responsibility and data.** Payment owns `Payment` records. A payment contains payment UUID, reference type, reference ID, user UUID, amount, currency, payment method, gateway transaction ID, status, paid timestamp, and audit timestamps.

**Important APIs.**

- `POST /api/payments`: create a payment.
- `GET /api/payments/{id}`: read payment.
- `GET /api/payments/order/{orderId}`: read payment by `referenceType=ORDER` and reference ID.
- `POST /api/payments/{id}/refund`: refund a successful payment.

**Statuses.** `PENDING`, `SUCCESS`, `FAILED`, and `REFUNDED` exist. The current implementation is a mock: every created payment is immediately `SUCCESS`, with a generated `MOCK-TXN-{UUID}` transaction ID. Refund changes `SUCCESS` to `REFUNDED`. There is no external gateway call and no actual failed-payment path in the Payment implementation.

**Dependencies and errors.** Payment does not call another service and has no event code. Order calls Payment synchronously. Payment creation validates only its local request shape; there is no uniqueness constraint or idempotency key preventing duplicate payments for the same Order reference.

### 1.9 Notification Service

**Responsibility and data.** Notification owns `Notification` records containing notification UUID, user UUID, type, event type string, message, status, sent timestamp, and creation timestamp. Types are `EMAIL`, `SMS`, and `PUSH`; statuses are `PENDING`, `SENT`, and `FAILED`.

**Important APIs.**

- `POST /api/notifications`: create a notification record.
- `GET /api/notifications/{id}`: read one notification.
- `GET /api/notifications/user/{userId}`: list a user's notifications.

Creation immediately persists `SENT` and sets `sentAt`. No email/SMS/push provider call exists. No other service calls Notification today, and Notification does not consume events. Failure in Notification therefore cannot currently affect Order because there is no current Order-Notification interaction.

## 2. Current End-to-End Purchase Flow

### 2.1 Implemented sequence

```text
Client
  |
  | POST /api/orders
  | body includes userId and shippingAddress
  v
API Gateway :8080
  |
  | route /api/orders/**
  v
Order Service :8085
  |
  | GET /api/users/{userId}
  v
User Service :8081
  |
  | user exists/valid response
  v
Order Service
  |
  | GET /api/carts/{userId}/active
  v
Cart Service :8084
  |
  | active cart with items, quantities, unit prices
  v
Order Service
  |
  | GET /api/books/{productId}, once per cart item
  v
Product Service :8082
  |
  | product exists response
  v
Order Service
  |
  | POST /api/inventory/reserve, once per cart item
  | body: productId, quantity, orderId=null
  v
Inventory Service :8083
  |
  | reservation response with reservationId and ACTIVE status
  v
Order Service
  |
  | local transaction: save Order and OrderItems
  v
Order database, order_schema
  |
  | POST /api/payments
  | referenceType=ORDER, referenceId=Order.id, userId, total, INR, MOCK
  v
Payment Service :8086
  |
  | SUCCESS mock payment response
  v
Order Service
  |
  | returns OrderResponse, normally still PENDING
  v
Client
```

### 2.2 Interaction details

| Caller | Receiver | Method/endpoint | Request data | Success behavior | Failure behavior |
|---|---|---|---|---|---|
| Client/Gateway | Order | `POST /api/orders` | Explicit `userId`, shipping address; DTO also has items but implementation reads active Cart | Starts synchronous checkout | Local validation, JWT mismatch, or downstream exceptions are returned by Order error handling |
| Order | User | `GET /api/users/{userId}` | Path user UUID; forwarded Authorization when present | Verifies user exists | User not found, unauthorized/forbidden, 5xx/transport become Order exceptions |
| Order | Cart | `GET /api/carts/{userId}/active` | Path user UUID; forwarded Authorization | Receives active cart and items | Missing/inactive/empty cart is rejected; service failures become Cart service exceptions |
| Order | Product | `GET /api/books/{productId}` per item | Product UUID path; forwarded Authorization | Validates each product exists | Product not found or unavailable aborts before reservation |
| Order | Inventory | `POST /api/inventory/reserve` per item | Product UUID, quantity, and currently `orderId=null` | Creates ACTIVE reservation; Order keeps only reservation IDs in memory | Earlier reservations are released on later reservation failure; release failure is printed only |
| Order | local Order DB | JPA save | User, address, cart-derived items, Cart unit prices, `PENDING` | Order and items persisted | Persistence failure occurs after reservation and is outside the reservation catch block, so reservation cleanup is not guaranteed |
| Order | Payment | `POST /api/payments` | ORDER reference to saved Order UUID, user, total, INR, MOCK | Mock Payment is saved as SUCCESS | Order tries to release reservations, marks local Order CANCELLED, saves, then rethrows; transaction rollback can prevent that save from being durable |
| Client/Gateway | Notification | None | None | No current interaction | No current effect on Order |

### 2.3 Classification

- **Implemented:** Gateway to Order routing; Order calls User, Cart, Product, Inventory, and Payment; Inventory reservation; local Order persistence; mock Payment persistence; reservation release during selected failures.
- **Partially implemented:** Compensation for reservation failures and Payment exceptions; Order cancellation; Payment relationship; Inventory reservation correlation. These have local pieces but no durable distributed workflow.
- **Not implemented:** Successful reservation confirmation; persisted reservation association; Order-to-Notification call; refund compensation from Order; Kafka events; Saga coordinator/state; durable retries and idempotency.

## 3. Order Lifecycle and Inconsistency Analysis

### 3.1 Actual create behavior

1. Validate request and, when a Bearer token is present, validate that the requested user matches the JWT subject unless the role is ADMIN.
2. Verify the user through User.
3. Read and validate the active Cart.
4. Validate every Product.
5. Reserve every Cart item in Inventory. Reservation requests currently omit `orderId`, even though Inventory accepts it.
6. Build and save a local Order with status `PENDING`.
7. Create Payment synchronously.
8. Return the local Order response without changing status based on Payment response.

The persisted Order contains the item snapshot and Cart unit prices, not Product authoritative prices and not reservation IDs. The Payment contains a relationship through `referenceType=ORDER` and `referenceId=Order.id`, but its payment ID is not stored on Order.

### 3.2 Actual cancellation behavior

`PUT /api/orders/{orderId}/cancel` loads the Order, rejects only an already-cancelled Order, sets status to `CANCELLED`, and saves it. It does not check owner authorization in the shown service method, release Inventory, locate or refund Payment, or publish an event. The generic status endpoint can set any supplied `OrderStatus`; it likewise has no distributed side effects.

### 3.3 Failure states that can become inconsistent

```text
Inventory reservation succeeds
  -> reservation is ACTIVE
  -> Order save fails
  -> Order may not exist, reservation can remain ACTIVE
```

```text
Order save succeeds
  -> Payment call fails
  -> Order attempts release in memory
  -> release can fail and is only printed
  -> Order is set CANCELLED locally, but the surrounding runtime transaction may roll back
  -> reservation may remain ACTIVE; Payment may be absent
```

```text
Payment succeeds
  -> Payment is SUCCESS
  -> Order remains PENDING
  -> Inventory reservation remains ACTIVE
  -> no confirm call; stock is held rather than consumed
```

```text
Order is cancelled later
  -> Order becomes CANCELLED
  -> Inventory is not released
  -> successful Payment is not refunded
```

```text
Client retries create after timeout
  -> no idempotency key exists
  -> a second Order, reservation set, and Payment can be created
```

The current `@Transactional` boundary is local to Order's database transaction; it cannot atomically include remote Inventory or Payment commits. Remote effects therefore cannot be rolled back by the Order transaction.

## 4. Inventory Saga Boundary

### 4.1 Current reservation contract

The forward operation is `POST /api/inventory/reserve` with `productId`, positive `quantity`, and optional `orderId`. It increments `reservedQuantity` without decrementing total quantity and creates an `ACTIVE` reservation with a generated UUID.

The preferred compensating operation is `POST /api/inventory/release/{reservationId}`. It releases exactly one active reservation and transitions `ACTIVE -> RELEASED`. It is safer than quantity-based release because it identifies the original reservation. The successful business operation after Payment is `POST /api/inventory/confirm/{reservationId}`, which transitions `ACTIVE -> CONFIRMED`, decrements total and reserved quantity, and leaves available stock unchanged at the transition.

### 4.2 Boundary data

At minimum, a future Saga command/event crossing the Inventory boundary needs:

- `sagaId` or workflow/correlation ID.
- `orderId`.
- `reservationId` for release/confirm after reserve succeeds.
- `productId`.
- `quantity`.
- Operation type and expected reservation transition.
- Event/command ID for duplicate detection.
- Attempt metadata where retries are allowed.

Inventory must persist its local inventory and reservation state. The Saga coordinator must durably persist the returned reservation ID and its relationship to the Order. Inventory's current reservation row already persists `orderId` as an optional field, but Order currently sends null and does not retain the response.

### 4.3 Suitability assessment

**Already suitable or close to suitable:** row-level locking, separate reservation entity, generated reservation ID, explicit `ACTIVE/RELEASED/CONFIRMED` states, and reservation-ID release/confirm endpoints. These provide a good local participant boundary.

**Not yet production-Saga suitable:** no order correlation is sent by current Order; no idempotency key or duplicate-command behavior exists; no durable event publication/outbox; no timeout/expiry policy for abandoned `ACTIVE` reservations; no participant-level event status or retry record; and the REST error mapping does not by itself guarantee safe duplicate retries. Quantity-based release/confirm should not be used for Saga compensation.

## 5. Payment Saga Boundary

### 5.1 Current behavior

Order calls `POST /api/payments` after it saves the Order. The request uses `referenceType=ORDER`, `referenceId=Order.id`, user ID, total, currency `INR`, and payment method `MOCK`. Payment persists an immediately `SUCCESS` record and a generated mock gateway transaction ID. Order does not store the returned payment ID or update its status.

Payment exposes `POST /api/payments/{id}/refund`. It changes only a successful Payment to `REFUNDED`; it rejects already-refunded or non-successful payments. This is the available compensation operation, but current Order never invokes it.

### 5.2 Future boundary

The future forward action is authorize/capture or process payment for one Order, with a stable idempotency key and the Order/Saga correlation. The future compensation is refund or void, depending on the payment provider state. The current mock does not expose the distinctions required for a real provider, such as authorization versus capture, provider failure reason, or a stable client-supplied payment operation key.

Required correlation includes `sagaId`, `orderId`/reference ID, `userId`, amount, currency, payment method, command/event ID, and an idempotency key. Payment must persist its local payment state and provider transaction reference. Before production-quality Saga work, the team must decide whether Payment creates one payment per Order, how duplicate create commands resolve, and how a refund command is correlated to the original payment.

## 6. Notification Boundary

Today Notification is a synchronous CRUD endpoint. A caller posts a complete `CreateNotificationRequest`; Notification stores it directly as `SENT`. There is no provider delivery and no Order call. Its required business input is user ID, type, event type, and message.

Notification should be **B: a downstream event consumer after the business transaction**, not a core transactional Saga participant. A notification is not required to reserve stock, charge/refund money, or establish the business completion invariant. Making checkout wait on Notification would allow an email/SMS/push storage or provider problem to fail an otherwise successful purchase and would add a non-compensating side effect to the core transaction. A future `OrderCompleted` or `PaymentCompleted` integration event can drive Notification. Notification should retry independently and use a dead-letter/manual-recovery path; its failure should not release stock or refund a completed Order.

The current direct notification endpoint can remain an administrative/manual path until an event consumer exists. Future payloads need user ID, order ID, event type, message/template data, event ID, and correlation ID. A notification delivery key is needed for deduplication.

## 7. Product and Cart Boundaries

### 7.1 Product

Product is authoritative for catalog product existence and book price. Current lookup is `GET /api/books/{id}`. Cart consumes it when adding/updating an item, Inventory consumes it when creating inventory, and Order consumes it for per-item validation. Order currently does not use the Product response price when creating its OrderItem; it uses the Cart snapshot price. The team must agree whether checkout revalidates and snapshots current Product price or treats Cart price as the checkout price. That is a business contract decision, not a Saga transport detail.

Product should not be a purchase Saga participant: its lookup is a read/validation step and there is no compensating Product mutation in checkout.

### 7.2 Cart

Cart provides the active basket and stores Product-derived unit prices. It validates user existence, product existence, and current availability on cart mutations. It checks available stock but does not reserve it. It has no checkout/finalization operation and no relationship to Order other than Order reading the active cart.

Cart should remain outside the transactional purchase Saga. Order should snapshot the cart into its own OrderItems at Saga start. Cart changes after snapshot should not mutate the in-flight Order. Cart clearing/finalization, if desired, is a separate workflow decision and must not be confused with Inventory reservation compensation.

## 8. User and Gateway Boundaries

User owns authenticated identity. The JWT subject is the User UUID. Order validates the incoming JWT subject against the explicit request `userId`, with ADMIN exception, and forwards the Authorization header to downstream clients. Cart's internal User existence call uses `X-Internal-Service-Secret`. Product, Inventory, and Cart do not consistently require the JWT for their current endpoints; Payment and other services have local handlers for unauthorized responses as observed by clients.

Current inconsistencies that affect future correlation:

- Order requests explicitly carry `userId`, while the architecture documentation describes JWT-derived identity in places. The implementation currently permits an explicit user ID after Order validation.
- User identity is in JWT/path/body, but there is no standard propagated correlation ID or service principal.
- Order forwards an end-user JWT as the downstream credential rather than using a clearly defined service-to-service credential model.
- Inventory reservations can persist `orderId`, but current Order sends null.
- Payment correlates through `referenceId=Order.id`, but there is no shared Saga/correlation ID.
- Gateway route coverage does not match all Product controller APIs.

Before Kafka/Saga implementation, the team must agree whether `orderId` is the business correlation key, whether a separate `sagaId` is required, and how both are propagated without trusting arbitrary client-supplied identifiers.

## 9. Distributed Transaction Classification

| Business operation | Current classification | Future classification |
|---|---|---|
| Create Order | Local Order transaction plus synchronous REST calls to User, Cart, Product, Inventory, Payment | Saga start/orchestration command owned by Order |
| Validate Product | Synchronous REST read | Pre-Saga validation or Order snapshot step; not a Saga participant |
| Read active Cart | Synchronous REST read | Pre-Saga snapshot; not a participant |
| Reserve Inventory | Synchronous REST participant operation | Saga forward action; command/event with order and correlation |
| Persist Order | Local Order transaction | Persist Saga state and Order state together where possible |
| Process Payment | Synchronous REST mock payment | Saga forward action; idempotent payment command |
| Confirm Inventory | Not currently called | Saga forward action after successful payment |
| Cancel Order | Local synchronous status update only | Saga-triggering command with release/refund rules based on current state |
| Release Inventory | Synchronous REST compensation on selected failures | Saga compensation for every successful reservation |
| Refund Payment | Implemented local Payment REST operation, never called by Order | Saga compensation after a captured/successful payment |
| Send Notification | Not connected to purchase | Kafka/integration event consumer after business completion |

## 10. Failure Matrix

“Future Kafka/Saga” describes the recommended target behavior, not current behavior.

| Scenario | Current behavior | Desired future behavior | Compensation | Kafka | Saga |
|---|---|---|---|---|---|
| Product not found | Order aborts before reservation; local Product-not-found error | Reject checkout before Saga reservation and expose stable reason | None | Not required for rejection | Start only after validation, or mark failed start |
| Product Service unavailable | Order client maps transport/5xx to Product unavailable; no reservation yet in normal sequence | Fail fast with retry policy before committing Saga work | None | Optional request retry, not domain event | No participant compensation |
| Inventory not found | Reservation call fails; earlier reservations are synchronously released | Record failed reserve and compensate every prior reservation durably | Release each acquired reservation | Command/result events or REST per agreed design | Yes, if any reservation succeeded |
| Insufficient stock | Inventory returns business error; earlier reservations are released | Mark reservation step failed and finish compensation reliably | Release prior reservations | Optional failure event | Yes when prior steps succeeded |
| Inventory Service unavailable | Client throws; release attempts can also fail and are only logged | Persist pending compensation and retry until resolved or operator action | Release acquired reservations | Yes for durable retry/result if event design chosen | Yes |
| Inventory reservation failure | Current method rethrows after best-effort releases | No success until all items are reserved; durable per-item state | Release successful item reservations | Saga command/result | Yes |
| Payment failure | Current Payment mock does not normally fail; client would release reservations and attempt local Order cancellation | Mark payment failed, compensate reservations, then mark Order failed/cancelled | Release reservations | Payment result/failure event | Yes |
| Payment Service unavailable | Order attempts releases; release failures only print; transaction may roll back local cancellation | Keep Saga pending, retry payment according to policy or compensate after timeout | Release reservations if payment will not retry | Yes | Yes |
| Order cancellation | Only local status changes; no release/refund | Route cancellation through state-aware coordinator | Release active reservations; refund successful payment | Cancellation command/event | Yes after external work exists |
| Inventory release failure | Error is printed; no durable task | Persist compensation pending and retry; alert after exhaustion | Retry exact reservation release | Yes/retry/DLQ as agreed | Yes |
| Inventory confirm failure | No current confirm call; reservations stay active | Retry confirm idempotently or enter operator-recovery state | Release only if payment is not retained; otherwise business-specific recovery | Yes | Yes |
| Notification failure | Impossible in current purchase path; direct endpoint failure affects only caller | Retry consumer independently; do not change completed Order | No stock/payment compensation | Yes, consumer retry/DLQ | No |
| Duplicate event | No events exist; duplicate HTTP requests can create duplicates | Deduplicate by event/command ID and business key | Return prior result or safely no-op | Yes, central to consumer | Yes where command is a Saga step |
| Consumer retry | No consumer/retry behavior | At-least-once handlers with bounded retry and durable pending state | Step-specific idempotent compensation | Yes | Yes for Saga handlers |
| Service restart during processing | No durable workflow state; remote/local partial effects can remain | Resume from persisted Saga state and reconcile participant status | Continue pending action or compensation | Yes with durable event/state strategy | Yes |

## 11. Proposed Saga Boundary

### 11.1 Recommended ownership

Order is the best Saga coordinator because it already owns the customer-facing purchase request, Order lifecycle, item snapshot, and current orchestration sequence. This is an orchestration Saga, not a distributed transaction. Inventory and Payment are confirmed participants. Product, Cart, and User are synchronous supporting services before or at Saga initiation. Notification is an event consumer outside the core Saga.

### 11.2 Proposed forward flow

```text
Client
  |
  v
Order: validate identity, Cart, Product, price snapshot, create PENDING/SAGA_STARTED
  |
  v
Inventory: reserve each item using orderId + sagaId
  |
  | all reservations ACTIVE and reservation IDs durably recorded
  v
Payment: process/authorize/capture using orderId + sagaId + idempotency key
  |
  | payment success
  v
Inventory: confirm each reservation by reservationId
  |
  | all confirmations complete
  v
Order: mark business transaction complete (PAID/CONFIRMED per agreed state model)
  |
  v
Publish OrderCompleted integration event
  |
  v
Notification: consume and deliver/store notification independently
```

The team must choose the exact meaning of `PAID`, `CONFIRMED`, and `CREATED`; the current enum supports several names but current behavior does not define a coherent transition contract.

### 11.3 Failure paths

```text
Reserve failure
  -> stop new reservations
  -> release every previously ACTIVE reservation by reservationId
  -> mark Saga failed and Order cancelled/failed
```

```text
Payment failure or terminal timeout
  -> release every ACTIVE reservation
  -> mark Order cancelled/failed
  -> no refund unless a payment was actually captured
```

```text
Payment success, Inventory confirm failure
  -> retain durable Saga state
  -> retry exact confirmations
  -> if confirmation cannot complete, execute an agreed recovery policy
     (release plus refund, or operator-assisted reconciliation)
  -> do not silently report Order complete
```

```text
Order cancellation after payment success
  -> determine whether reservations are ACTIVE or CONFIRMED
  -> refund successful payment where allowed
  -> release only ACTIVE reservations
  -> record final cancellation outcome and unresolved compensation
```

### 11.4 Required persisted state

Order-side Saga state must at least retain Saga ID, Order ID, current step, overall state, attempt/timestamps, failure reason, payment ID/reference and status, and one reservation association per OrderItem including reservation ID and state. A durable outbox or equivalent publication record is required if database state and Kafka publication must be reliable. Participant services retain their own local records and deduplication/operation results.

## 12. Kafka Boundary

Kafka should add value for durable workflow commands/results and downstream integration, not for Product/Cart read lookups that are needed immediately to validate the checkout request. The exact topic partitioning and ownership require team agreement.

| Proposed event/command | Producer | Consumer | Purpose | Important payload | Correlation | Saga? |
|---|---|---|---|---|---|---|
| `OrderCheckoutStarted` | Order | Saga handlers/observability | Announces a new workflow after local start | eventId, sagaId, orderId, userId, item product/quantity snapshot, total, currency | sagaId + orderId | Yes |
| `InventoryReserveRequested` | Order coordinator | Inventory | Ask Inventory to reserve one or all item reservations | eventId, sagaId, orderId, productId, quantity, operation/idempotency key | sagaId + orderId + eventId | Yes |
| `InventoryReserved` | Inventory | Order coordinator | Report reservation success | eventId, sagaId, orderId, productId, quantity, reservationId, status | sagaId + orderId + reservationId | Yes |
| `InventoryReservationFailed` | Inventory | Order coordinator | Report business or technical reservation failure | eventId, sagaId, orderId, productId, reason/code, retryable | sagaId + orderId | Yes |
| `InventoryReleaseRequested` | Order coordinator | Inventory | Compensate one exact reservation | eventId, sagaId, orderId, reservationId, reason | sagaId + orderId + reservationId | Yes, compensation |
| `InventoryReleased` | Inventory | Order coordinator | Report compensation success | eventId, sagaId, orderId, reservationId, status | sagaId + orderId + reservationId | Yes, compensation |
| `PaymentRequested` | Order coordinator | Payment | Process one Order payment | eventId, sagaId, orderId, userId, amount, currency, method, idempotency key | sagaId + orderId | Yes |
| `PaymentSucceeded` | Payment | Order coordinator | Report payment success and payment ID/provider reference | eventId, sagaId, orderId, paymentId, amount, status, provider reference | sagaId + orderId + paymentId | Yes |
| `PaymentFailed` | Payment | Order coordinator | Report failure and retryability | eventId, sagaId, orderId, reason/code, retryable | sagaId + orderId | Yes |
| `PaymentRefundRequested` | Order coordinator | Payment | Compensate a captured payment | eventId, sagaId, orderId, paymentId, reason, idempotency key | sagaId + orderId + paymentId | Yes, compensation |
| `PaymentRefunded` | Payment | Order coordinator | Report refund result | eventId, sagaId, orderId, paymentId, status | sagaId + orderId + paymentId | Yes, compensation |
| `InventoryConfirmRequested` | Order coordinator | Inventory | Consume reserved stock by exact reservation ID | eventId, sagaId, orderId, reservationId | sagaId + orderId + reservationId | Yes |
| `InventoryConfirmed` | Inventory | Order coordinator | Report stock confirmation | eventId, sagaId, orderId, reservationId, status | sagaId + orderId + reservationId | Yes |
| `OrderCompleted` | Order | Notification and other integrations | Publish completed business outcome | eventId, sagaId, orderId, userId, items, total, currency, completedAt | sagaId + orderId | No: integration event after Saga |
| `OrderCancelled` | Order | Notification/analytics | Publish terminal failed/cancelled outcome | eventId, sagaId, orderId, userId, reason, compensation summary | sagaId + orderId | No: integration event |

The names are proposals, not approved contracts. The team must decide whether commands and results use separate topics, whether one topic is partitioned by `orderId`, retention, schema versioning, delivery semantics, and dead-letter policy.

## 13. Idempotency, Retry, and Correlation Review

### Existing capabilities

- UUIDs exist for User, Product, Inventory, Reservation, Order, Payment, and Notification records.
- Inventory has exact reservation IDs and prevents a second transition from a non-`ACTIVE` state.
- Payment can query by Order reference and refuses a second refund of the same record.
- Order Payment requests correlate a Payment to an Order through `referenceType=ORDER` and `referenceId`.
- Inventory uses row locking for local quantity/reservation concurrency.

### Missing capabilities

- No request idempotency key for create Order or create Payment.
- No event ID, command ID, Saga ID, trace ID, or standard correlation header.
- No durable record of Order-to-reservation IDs.
- No durable record of Payment ID on Order.
- No duplicate event handling or consumer inbox.
- No retry policy, backoff, timeout policy, circuit breaker, or dead-letter handling.
- No outbox or atomic publication record.
- No reservation expiration/reaper for abandoned active reservations.
- No durable compensation queue or reconciliation state.
- No explicit distinction between retryable transport failure and terminal business failure in all service contracts.
- No provider-level payment idempotency or authorization/capture/refund state.

Production-quality implementation must first define stable operation keys. A reasonable baseline is a client/request id for checkout, a server-generated `sagaId`, `orderId` as business key, an event/command ID per message, and an operation key per participant action. Handlers must return or load the prior outcome for duplicate keys rather than creating new reservations/payments.

## 14. Team Ownership Plan

| Future work | Primary owner | Affected service(s) | Dependency/agreement |
|---|---|---|---|
| Gateway route/auth propagation review | Member 1 | Gateway, User | Agree identity and service-auth propagation with all members |
| JWT/user identity and internal-service contract | Member 1 | User, Gateway, Cart/Order clients | Team agreement on trusted user ID, service principals, and correlation headers |
| Product price snapshot contract | Member 2 | Product, Order/Cart contract | Member 3 must agree checkout price semantics |
| Reservation command/result contract | Member 2 | Inventory, Order | Team agreement on reservation ID, orderId, sagaId, duplicate behavior |
| Reservation expiry/reconciliation design | Member 2 | Inventory | Depends on Saga timeout and compensation policy |
| Cart snapshot/checkout boundary | Member 2 with Member 3 | Cart, Order | Team agreement on active Cart behavior during checkout |
| Order Saga coordinator/state | Member 3 | Order | Depends on all event contracts and participant states |
| Payment process/idempotency/refund contract | Member 3 | Payment, Order | Team agreement on capture versus authorize and refund semantics |
| Notification consumer and delivery retry | Member 3 | Notification | Depends on approved OrderCompleted/OrderCancelled event schema |
| Kafka infrastructure/topics/schema strategy | Shared, implementation coordinated by Member 3 | All participants | Mandatory team agreement before any topic/dependency change |
| Cross-service failure/recovery tests | Shared | All | Test scenarios and expected terminal states require team agreement |

Saga architecture, event names/payloads, topic partition keys, retry semantics, and compensation rules are team decisions even when one member writes the code.

## 15. Pre-Kafka Checklist

| Decision | Status | Current evidence / required decision |
|---|---|---|
| Final Saga flow | NEEDS DECISION | Current flow stops after Payment and never confirms Inventory |
| Participant responsibilities | NEEDS DECISION | Recommended: Order coordinator, Inventory and Payment participants, Notification consumer; approve as team |
| Product role | READY for boundary | Current Product is a synchronous lookup/authority, not a mutating participant |
| Cart role | READY for boundary | Current Cart is active-cart storage and availability validation, not reservation ownership |
| User role | READY for boundary | User owns identity; not a purchase participant |
| Event names | NEEDS DECISION | Names in this document are proposals only |
| Event payloads | NEEDS DECISION | Must include eventId, sagaId, orderId, userId as appropriate, item/payment/reservation data |
| Topic strategy | NEEDS DECISION | Topics, partitions, key, retention, versioning, and DLQ are not present |
| Correlation strategy | NEEDS DECISION | Standardize sagaId, orderId, eventId, and operation/idempotency keys |
| Failure strategy | NEEDS DECISION | Define retryable versus terminal failures and timeout behavior |
| Compensation strategy | NEEDS DECISION | Release exact reservations; refund only captured/successful payment; define confirm failure recovery |
| Idempotency strategy | MISSING | No current request/message deduplication exists |
| Retry strategy | MISSING | No current durable retry/backoff/DLQ exists |
| Persistence requirements | NEEDS DECISION | Persist Saga state, reservation IDs, payment ID/status, attempts, and compensation state |
| Outbox/inbox strategy | MISSING | No durable publication or consumer deduplication mechanism exists |
| Authentication considerations | NEEDS DECISION | Resolve end-user JWT forwarding versus service authentication and correlation propagation |
| Order status transition model | NEEDS DECISION | Current enum has states but create/payment/cancel transitions are not coherent |
| Payment provider state model | NEEDS DECISION | Current mock has immediate SUCCESS; real flow needs provider/idempotency/refund states |
| Reservation expiry | NEEDS DECISION | Current ACTIVE reservations have no expiration or recovery policy |
| Notification role | READY for boundary | Recommend downstream event consumer, not core Saga participant |
| Testing strategy | NEEDS DECISION | Define contract, duplicate, restart, timeout, compensation, and failure tests |
| Kafka implementation | NOT REQUIRED yet | Do not start until the unresolved contract decisions are approved |
| Saga implementation | NOT REQUIRED yet | Do not start until participant and state contracts are approved |

## 16. Final Recommendation

### A. Current architecture status

The project has eight separately configured services with schema-per-service data ownership and a working synchronous checkout implementation centered in Order. It is not currently event-driven and it is not a complete Saga. Inventory reservation primitives are the strongest existing Saga-ready boundary, but the current Order integration does not provide the required correlation or completion/compensation lifecycle.

### B. Confirmed future Saga participants

- **Order:** Saga coordinator and owner of customer-facing Order/Saga state.
- **Inventory:** reserve, release compensation, and confirm participant.
- **Payment:** process/authorize/capture and refund compensation participant.

**Outside the core Saga:** User, Product, and Cart are supporting identity/read/snapshot services. Notification is a downstream event consumer.

### C. Proposed Saga flow

```text
Order validates User + Cart + Product and snapshots items/prices
  -> Inventory reserves all items and returns reservation IDs
  -> Payment succeeds for the Order
  -> Inventory confirms every reservation by reservation ID
  -> Order becomes complete
  -> OrderCompleted event drives Notification
```

### D. Proposed compensation flow

```text
Reservation failure or payment terminal failure
  -> release every successful reservation by exact reservationId
  -> mark Order/Saga failed or cancelled

Payment success followed by confirmation failure
  -> retry confirmation from durable state
  -> apply an approved recovery policy, normally refund payment and release any still-ACTIVE reservations
  -> mark unresolved state for reconciliation rather than claiming success

Cancellation after payment
  -> refund successful payment
  -> release only ACTIVE reservations
  -> persist final or pending compensation state
```

### E. Proposed Kafka events

The minimum event family is `InventoryReserveRequested/Reserved/ReservationFailed`, `InventoryReleaseRequested/Released`, `PaymentRequested/Succeeded/Failed`, `PaymentRefundRequested/Refunded`, `InventoryConfirmRequested/Confirmed`, and post-transaction `OrderCompleted`/`OrderCancelled`. These names and payloads require team approval before implementation. `OrderCompleted` and `OrderCancelled` are integration events; the Inventory and Payment messages are Saga commands/results.

### F. Member-wise implementation ownership

Member 1 owns Gateway/User identity and internal authentication propagation. Member 2 owns Product/Inventory/Cart contracts, exact reservation behavior, and reservation recovery. Member 3 owns Order coordination, Payment state/refund behavior, and Notification event consumption. Kafka infrastructure, event contracts, correlation, and compensation are shared architectural decisions.

### G. Risks and blockers

1. Reservation IDs are not persisted by Order and are currently disconnected from `orderId`.
2. Successful Payment leaves Order `PENDING` and Inventory reservations `ACTIVE`.
3. Local Order transactions cannot roll back remote Inventory/Payment effects.
4. Cancellation has no distributed compensation or ownership enforcement in the service method.
5. Payment is a mock with no real failure, provider state, or idempotency behavior.
6. No event, correlation, retry, timeout, outbox, inbox, or recovery mechanism exists.
7. Existing broad API documentation is stale in places, especially route coverage and current Order orchestration; source code must remain the contract until docs are reconciled.
8. Price authority at checkout is not resolved because Order validates Product but stores Cart price.
9. Gateway and downstream authentication responsibilities are inconsistent for future asynchronous consumers.

### H. Exact recommended implementation order

1. Hold a team decision session and approve the Saga boundary, Order status transitions, price snapshot rule, and cancellation semantics.
2. Approve versioned event/command contracts, including event ID, saga ID, order ID, reservation ID, payment ID, operation key, error model, and topic/partition strategy.
3. Define the participant state machines and idempotency/retry/timeout/DLQ rules on paper, including confirm failure and service restart recovery.
4. Define the persistence model for Order Saga state, per-item reservation associations, payment association, compensation attempts, and publication/consumption records.
5. Add Kafka infrastructure and contract-level serialization/testing only after the above decisions are frozen.
6. Implement one narrow happy-path flow: Order start -> Inventory reserve -> Payment success -> Inventory confirm -> Order complete.
7. Add durable duplicate handling and restart recovery for that happy path.
8. Add reservation-release compensation for reserve/payment failure and test each failure point.
9. Add Payment refund compensation and cancellation behavior, then test partial completion and confirm failures.
10. Add `OrderCompleted`/`OrderCancelled` integration events and Notification consumption with independent retry/DLQ behavior.
11. Reconcile Gateway/auth propagation and update API/event documentation to the approved contracts.
12. Run full failure testing: duplicate requests/events, retries, timeouts, service restarts, unavailable services, partial reservation, payment failure, confirm failure, release failure, refund failure, and reconciliation.

The next implementation step is therefore **team approval of the state machines and versioned event contracts**, not adding Kafka code yet.
