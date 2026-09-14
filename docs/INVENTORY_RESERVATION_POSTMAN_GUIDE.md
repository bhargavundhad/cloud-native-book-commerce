# Inventory Reservation Postman Guide

This guide covers only the reservation-aware Inventory Service functionality. It uses the current direct service port and implemented endpoints.

No Kafka, Saga, Order Service, Cart Service, or Product Service call is required by these requests.

## 1. Source-of-truth contract

### Service URL

Inventory Service is configured on:

```text
http://localhost:8083
```

Base URL used below:

```text
{{inventoryBaseUrl}} = http://localhost:8083
```

### Reservation-aware endpoints

| Purpose | Method | Exact URL |
|---|---|---|
| Create a reservation | `POST` | `{{inventoryBaseUrl}}/api/inventory/reserve` |
| Release an active reservation | `POST` | `{{inventoryBaseUrl}}/api/inventory/release/{{reservationId}}` |
| Confirm an active reservation | `POST` | `{{inventoryBaseUrl}}/api/inventory/confirm/{{reservationId}}` |

The existing quantity-based endpoints also remain implemented:

```text
POST /api/inventory/release?productId={productId}&quantity={quantity}
POST /api/inventory/confirm?productId={productId}&quantity={quantity}
```

This guide intentionally tests the reservation-ID endpoints because they identify one reservation and prevent the same reservation from being released or confirmed twice.

### Headers

The current controllers do not require authentication or custom headers.

For `POST /api/inventory/reserve`, send:

```http
Content-Type: application/json
```

The release and confirm requests have no request body and do not require `Content-Type`. Postman may still send `Content-Type: application/json`; it is ignored because these endpoints have no body.

### Reservation request body

`productId` and `quantity` are required. `orderId` is optional.

```json
{
  "productId": "{{productId}}",
  "quantity": 2,
  "orderId": "{{orderId}}"
}
```

For an independent Inventory test, `orderId` may be omitted:

```json
{
  "productId": "{{productId}}",
  "quantity": 2
}
```

`quantity` must be greater than zero. UUID fields must contain valid UUID values.

### Successful response shape

All three reservation-aware endpoints return HTTP `200 OK` with this JSON shape:

```json
{
  "id": "inventory-uuid",
  "productId": "product-uuid",
  "quantity": 10,
  "reservedQuantity": 2,
  "status": "AVAILABLE",
  "updatedAt": "2026-09-12T10:00:00",
  "reservationId": "reservation-uuid",
  "orderId": "order-uuid-or-null",
  "reservationQuantity": 2,
  "reservationStatus": "ACTIVE",
  "reservationCreatedAt": "2026-09-12T10:00:00",
  "reservationUpdatedAt": "2026-09-12T10:00:00"
}
```

The UUIDs and timestamps are generated at runtime. A reservation response from the reserve endpoint has `reservationStatus: ACTIVE`; a release response has `reservationStatus: RELEASED`; a confirm response has `reservationStatus: CONFIRMED`.

The inventory `quantity` is the total stock. Available stock is calculated as:

```text
availableStock = quantity - reservedQuantity
```

## 2. Postman environment and prerequisites

Create these environment variables:

| Variable | Value |
|---|---|
| `inventoryBaseUrl` | `http://localhost:8083` |
| `productId` | Existing product UUID with an Inventory row |
| `orderId` | Optional valid UUID used for correlation |
| `reservationId` | Set from the successful reserve response |
| `missingReservationId` | Valid UUID that does not exist in `inventory_schema.reservations` |

Before running the scenarios:

1. Start Inventory Service on port `8083`.
2. Ensure the `inventory_schema.reservations` table exists. The SQL is in `inventory-service/src/main/resources/reservation-schema.sql`.
3. Ensure `{{productId}}` has an Inventory row.
4. For predictable examples, use an inventory row with:
   - `quantity = 10`
   - `reservedQuantity = 0`
   - available stock `10`
5. Do not reuse the same reservation for both release and confirm. An ACTIVE reservation can transition once, to either RELEASED or CONFIRMED.

An Inventory row can be read with:

```http
GET {{inventoryBaseUrl}}/api/inventory/product/{{productId}}
```

That endpoint returns the current `quantity`, `reservedQuantity`, and inventory `status`.

## 3. Test 1: Successful stock reservation

### Purpose

Create an identifiable ACTIVE reservation for two units without reducing total inventory quantity.

### Prerequisites

- Inventory Service is running.
- `{{productId}}` exists in Inventory Service.
- Current inventory is `quantity = 10`, `reservedQuantity = 0`.
- `{{orderId}}` is a valid UUID, or omit `orderId` from the body.

### Request

```http
POST {{inventoryBaseUrl}}/api/inventory/reserve
Content-Type: application/json
```

Body:

```json
{
  "productId": "{{productId}}",
  "quantity": 2,
  "orderId": "{{orderId}}"
}
```

### Expected HTTP status

```text
200 OK
```

### Expected response

```json
{
  "id": "inventory-uuid",
  "productId": "{{productId}}",
  "quantity": 10,
  "reservedQuantity": 2,
  "status": "AVAILABLE",
  "updatedAt": "runtime timestamp",
  "reservationId": "new UUID",
  "orderId": "{{orderId}}",
  "reservationQuantity": 2,
  "reservationStatus": "ACTIVE",
  "reservationCreatedAt": "runtime timestamp",
  "reservationUpdatedAt": "runtime timestamp"
}
```

The exact response field names are those of `InventoryResponse`.

### Save `reservationId`

In Postman, add a **Tests** script to this request to save the returned reservation identity:

```javascript
const response = pm.response.json();
pm.environment.set("reservationId", response.reservationId);
```

The saved `{{reservationId}}` is then used by the release and confirm URLs:

```text
POST {{inventoryBaseUrl}}/api/inventory/release/{{reservationId}}
POST {{inventoryBaseUrl}}/api/inventory/confirm/{{reservationId}}
```

Save a second reservation if you want to test both release and confirm independently. A reservation that has been released cannot later be confirmed, and a reservation that has been confirmed cannot later be released.

### Database verification

```sql
SELECT id, product_id, quantity, reserved_quantity, status, updated_at
FROM inventory_schema.inventory
WHERE product_id = '<product UUID>';

SELECT reservation_id, product_id, quantity, order_id, status, created_at, updated_at
FROM inventory_schema.reservations
WHERE reservation_id = '<reservation UUID>';
```

Expected values:

- `inventory.quantity = 10`; total stock is unchanged.
- `inventory.reserved_quantity = 2`.
- Available stock is `10 - 2 = 8`.
- `reservations.quantity = 2`.
- `reservations.status = 'ACTIVE'`.
- `reservations.order_id` equals the supplied `orderId`, or is `NULL` when omitted.

### What the test proves

The service validates available stock under the existing pessimistic inventory lock, increases only `reservedQuantity`, creates a unique reservation record, and returns the reservation ID needed for compensation or confirmation.

## 4. Test 2: Insufficient stock reservation

### Purpose

Verify that a reservation larger than available stock is rejected and does not create a reservation or change inventory.

### Prerequisites

Use an inventory row with:

- `quantity = 10`
- `reservedQuantity = 8`
- available stock `2`

Alternatively, first create reservations totalling eight units.

### Request

```http
POST {{inventoryBaseUrl}}/api/inventory/reserve
Content-Type: application/json
```

```json
{
  "productId": "{{productId}}",
  "quantity": 3
}
```

### Expected HTTP status

```text
400 Bad Request
```

### Expected response

```json
{
  "success": false,
  "message": "Insufficient stock for reservation. Requested: 3, available: 2",
  "errorCode": "INSUFFICIENT_STOCK"
}
```

### Database verification

Before and after the request, verify:

```sql
SELECT quantity, reserved_quantity
FROM inventory_schema.inventory
WHERE product_id = '<product UUID>';

SELECT COUNT(*)
FROM inventory_schema.reservations
WHERE product_id = '<product UUID>';
```

`quantity`, `reserved_quantity`, and the reservation count must not increase because validation occurs before the mutation and reservation insert.

### What the test proves

The service calculates available stock as `quantity - reservedQuantity`, rejects an over-sized request, and does not create a partial reservation.

## 5. Test 3: Release an ACTIVE reservation

### Purpose

Release one specific ACTIVE reservation and return its reserved units to available stock.

### Prerequisites

Run Test 1 and save its `reservationId` as `{{reservationId}}`. The reservation must still have `reservationStatus = ACTIVE`.

### Request

```http
POST {{inventoryBaseUrl}}/api/inventory/release/{{reservationId}}
```

No request body and no query parameters.

### Expected HTTP status

```text
200 OK
```

### Expected response

```json
{
  "id": "inventory-uuid",
  "productId": "{{productId}}",
  "quantity": 10,
  "reservedQuantity": 0,
  "status": "AVAILABLE",
  "updatedAt": "runtime timestamp",
  "reservationId": "{{reservationId}}",
  "orderId": "order-uuid-or-null",
  "reservationQuantity": 2,
  "reservationStatus": "RELEASED",
  "reservationCreatedAt": "original timestamp",
  "reservationUpdatedAt": "runtime timestamp"
}
```

### Database verification

```sql
SELECT quantity, reserved_quantity, status
FROM inventory_schema.inventory
WHERE product_id = '<product UUID>';

SELECT reservation_id, quantity, status
FROM inventory_schema.reservations
WHERE reservation_id = '<reservation UUID>';
```

Expected values after releasing a two-unit reservation from the Test 1 starting state:

- `quantity = 10`; total stock is unchanged.
- `reserved_quantity = 0`.
- Available stock is `10 - 0 = 10`.
- Reservation `status = 'RELEASED'`.

### What the test proves

The service locks the reservation and matching inventory transactionally, decreases `reservedQuantity` by that reservation's exact quantity, and marks the reservation RELEASED.

## 6. Test 4: Confirm an ACTIVE reservation

### Purpose

Confirm one specific ACTIVE reservation and consume its stock.

### Prerequisites

Create a fresh reservation using Test 1 and save its new `reservationId`. Do not use a reservation already released or confirmed.

For a starting inventory of `quantity = 10`, `reservedQuantity = 0`, reserve two units first.

### Request

```http
POST {{inventoryBaseUrl}}/api/inventory/confirm/{{reservationId}}
```

No request body and no query parameters.

### Expected HTTP status

```text
200 OK
```

### Expected response

```json
{
  "id": "inventory-uuid",
  "productId": "{{productId}}",
  "quantity": 8,
  "reservedQuantity": 0,
  "status": "AVAILABLE",
  "updatedAt": "runtime timestamp",
  "reservationId": "{{reservationId}}",
  "orderId": "order-uuid-or-null",
  "reservationQuantity": 2,
  "reservationStatus": "CONFIRMED",
  "reservationCreatedAt": "original timestamp",
  "reservationUpdatedAt": "runtime timestamp"
}
```

### Database verification

```sql
SELECT quantity, reserved_quantity, status
FROM inventory_schema.inventory
WHERE product_id = '<product UUID>';

SELECT reservation_id, quantity, status
FROM inventory_schema.reservations
WHERE reservation_id = '<reservation UUID>';
```

Expected values after confirming a two-unit reservation:

- `quantity = 8`; total stock decreases by two.
- `reserved_quantity = 0`; the reservation is no longer held.
- Available stock is `8 - 0 = 8`.
- Reservation `status = 'CONFIRMED'`.
- `quantity >= reserved_quantity` remains true.

### What the test proves

The service consumes both total quantity and reserved quantity for the identified ACTIVE reservation, exactly once, within a transaction.

## 7. Test 5: Release an already RELEASED reservation

### Purpose

Verify that repeating release for the same reservation is rejected and does not reduce reserved stock again.

### Prerequisites

Run Test 3 successfully. Keep the same `{{reservationId}}`.

### Request

```http
POST {{inventoryBaseUrl}}/api/inventory/release/{{reservationId}}
```

No request body or query parameters.

### Expected HTTP status

```text
400 Bad Request
```

### Expected response

```json
{
  "success": false,
  "message": "Reservation is already released and cannot be release",
  "errorCode": "RESERVATION_ALREADY_RELEASED"
}
```

The message is produced by the current implementation; the stable machine-readable value is `errorCode`.

### Database verification

```sql
SELECT quantity, reserved_quantity
FROM inventory_schema.inventory
WHERE product_id = '<product UUID>';

SELECT status
FROM inventory_schema.reservations
WHERE reservation_id = '<reservation UUID>';
```

Inventory values must be identical before and after the repeated request. Reservation status remains `RELEASED`.

### What the test proves

Only ACTIVE reservations can be released. A repeated release cannot decrement `reservedQuantity` a second time.

## 8. Test 6: Confirm an already CONFIRMED reservation

### Purpose

Verify that repeating confirmation for the same reservation is rejected and does not consume inventory again.

### Prerequisites

Run Test 4 successfully. Keep the same `{{reservationId}}`.

### Request

```http
POST {{inventoryBaseUrl}}/api/inventory/confirm/{{reservationId}}
```

No request body or query parameters.

### Expected HTTP status

```text
400 Bad Request
```

### Expected response

```json
{
  "success": false,
  "message": "Reservation is already confirmed and cannot be confirm",
  "errorCode": "RESERVATION_ALREADY_CONFIRMED"
}
```

### Database verification

```sql
SELECT quantity, reserved_quantity
FROM inventory_schema.inventory
WHERE product_id = '<product UUID>';

SELECT status
FROM inventory_schema.reservations
WHERE reservation_id = '<reservation UUID>';
```

Inventory values must be identical before and after the repeated request. Reservation status remains `CONFIRMED`.

### What the test proves

Only ACTIVE reservations can be confirmed. A repeated confirmation cannot decrement `quantity` or `reservedQuantity` again.

## 9. Test 7: Release a nonexistent reservation

### Purpose

Verify that release rejects a valid UUID that is not present in the reservation table.

### Prerequisites

Set `{{missingReservationId}}` to a valid UUID that does not exist in `inventory_schema.reservations`.

### Request

```http
POST {{inventoryBaseUrl}}/api/inventory/release/{{missingReservationId}}
```

No request body or query parameters.

### Expected HTTP status

```text
404 Not Found
```

### Expected response

```json
{
  "success": false,
  "message": "Reservation not found for reservationId: {{missingReservationId}}",
  "errorCode": "RESERVATION_NOT_FOUND"
}
```

### Database verification

```sql
SELECT COUNT(*)
FROM inventory_schema.reservations
WHERE reservation_id = '<missing reservation UUID>';
```

The count remains zero. Inventory rows must not change.

### What the test proves

Release requires an existing reservation and cannot mutate inventory when the reservation identity is unknown.

## 10. Test 8: Confirm a nonexistent reservation

### Purpose

Verify that confirm rejects a valid UUID that is not present in the reservation table.

### Prerequisites

Use a valid missing UUID in `{{missingReservationId}}`. It must not be an existing reservation ID.

### Request

```http
POST {{inventoryBaseUrl}}/api/inventory/confirm/{{missingReservationId}}
```

No request body or query parameters.

### Expected HTTP status

```text
404 Not Found
```

### Expected response

```json
{
  "success": false,
  "message": "Reservation not found for reservationId: {{missingReservationId}}",
  "errorCode": "RESERVATION_NOT_FOUND"
}
```

### Database verification

```sql
SELECT COUNT(*)
FROM inventory_schema.reservations
WHERE reservation_id = '<missing reservation UUID>';
```

The count remains zero. Inventory `quantity` and `reserved_quantity` must not change.

### What the test proves

Confirm requires an existing reservation and cannot consume inventory for an unknown reservation identity.

## 11. State transition summary

| Operation | `quantity` | `reservedQuantity` | Available stock | Reservation status |
|---|---:|---:|---:|---|
| Reserve `n` | unchanged | `+n` | decreases by `n` | new `ACTIVE` reservation |
| Release reservation `n` | unchanged | `-n` | increases by `n` | `ACTIVE` -> `RELEASED` |
| Confirm reservation `n` | `-n` | `-n` | decreases by `n` from the post-reservation state | `ACTIVE` -> `CONFIRMED` |
| Repeat release | unchanged | unchanged | unchanged | remains `RELEASED`; rejected |
| Repeat confirm | unchanged | unchanged | unchanged | remains `CONFIRMED`; rejected |

The implementation preserves the invariant:

```text
quantity >= reservedQuantity
```

## 12. Common setup and routing notes

- A reservation request for an unknown product inventory row returns `404 Not Found` with `errorCode: INVENTORY_NOT_FOUND`.
- A zero or negative reservation quantity returns `400 Bad Request` with `errorCode: INVALID_RESERVATION`.
- A malformed UUID in a UUID path or parameter is handled as `400 Bad Request` with `errorCode: INVALID_UUID`.
- The reservation-aware endpoints are synchronous REST endpoints. This guide does not add or assume Kafka or Saga behavior.
- The database table is `inventory_schema.reservations`; there is no cross-schema foreign key to `order_schema`.
