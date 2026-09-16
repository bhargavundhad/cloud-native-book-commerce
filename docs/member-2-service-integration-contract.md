# Member 2 Service Integration Contract

This document describes the integration contracts currently provided by Product Service, Inventory Service, and Cart Service. The running source code is the source of truth.

## 1. Product Service Contract

### `GET /api/books/{id}`

| Property | Contract |
|---|---|
| Purpose | Retrieve one product and its catalog data. |
| HTTP method | `GET` |
| Path | `/api/books/{id}` |
| Request requirements | `id` must be a valid product UUID in the path. No request body or custom header is required. |

Product Service is the authoritative source for product existence and product price.

#### Response

The successful response is a `BookResponse` object containing:

| Field | Type |
|---|---|
| `id` | UUID |
| `isbn` | String |
| `title` | String |
| `description` | String |
| `authorId` | UUID |
| `categoryId` | UUID |
| `price` | BigDecimal |
| `createdAt` | LocalDateTime |
| `updatedAt` | LocalDateTime |

`price` is the authoritative product price. Services must not rely on a client-supplied product price when this Product response is available.

#### Errors and availability

- If the product does not exist, Product Service returns HTTP `404 Not Found` with an `ApiError` body containing `success: false`, a message, and `errorCode: BOOK_NOT_FOUND`.
- If the path UUID is invalid, Product Service returns HTTP `400 Bad Request` with `errorCode: INVALID_UUID`.
- Product Service does not currently define a dedicated service-unavailable error handler or error code for this endpoint. A caller may observe a connection failure or an HTTP 5xx response if Product Service is unavailable. Downstream clients classify those failures according to their own integration error handling.

## 2. Inventory Service Contract

Inventory tracks total quantity and reserved quantity for each product. Available stock is calculated as:

```text
availableStock = quantity - reservedQuantity
```

### `POST /api/inventory/reserve`

Creates an active reservation for a product.

#### Request

No custom header or authentication header is required by the current controller. The request body is JSON:

```json
{
  "productId": "product-uuid",
  "quantity": 2,
  "orderId": "order-uuid"
}
```

| Field | Requirement |
|---|---|
| `productId` | Required UUID. |
| `quantity` | Required positive integer greater than zero. |
| `orderId` | Optional UUID. Supply it when an Order exists so the reservation can be correlated with that order. |

#### Successful behavior

The endpoint returns HTTP `200 OK` with an `InventoryResponse` containing:

| Field | Type / meaning |
|---|---|
| `id` | Inventory row UUID. |
| `productId` | Reserved product UUID. |
| `quantity` | Total inventory quantity. |
| `reservedQuantity` | Total quantity currently reserved after the operation. |
| `status` | Inventory status, such as `AVAILABLE` or `OUT_OF_STOCK`. |
| `updatedAt` | Inventory update timestamp. |
| `reservationId` | UUID generated for this reservation. |
| `orderId` | The supplied order UUID, or null. |
| `reservationQuantity` | Quantity assigned to this reservation. |
| `reservationStatus` | Reservation lifecycle status; after reserve it is `ACTIVE`. |
| `reservationCreatedAt` | Reservation creation timestamp. |
| `reservationUpdatedAt` | Reservation update timestamp. |

When reservation succeeds:

- `quantity` (total stock) does not decrease.
- `reservedQuantity` increases by the requested quantity.
- Available stock decreases by the requested quantity.
- A new reservation is created with status `ACTIVE`.

### `POST /api/inventory/release/{reservationId}`

Releases one exact reservation. The reservation ID is supplied in the path. The endpoint has no request body.

When release succeeds:

- The reservation changes from `ACTIVE` to `RELEASED`.
- Total `quantity` is unchanged.
- `reservedQuantity` decreases by the reservation quantity.
- Available stock increases by the reservation quantity.

The response is HTTP `200 OK` with the inventory fields and the released reservation fields. Its `reservationStatus` is `RELEASED`.

### `POST /api/inventory/confirm/{reservationId}`

Confirms one exact reservation. The reservation ID is supplied in the path. The endpoint has no request body.

When confirmation succeeds:

- The reservation changes from `ACTIVE` to `CONFIRMED`.
- Total `quantity` decreases by the reservation quantity.
- `reservedQuantity` decreases by the reservation quantity.
- Available stock remains unchanged by the transition because both total and reserved quantities decrease by the same amount.

The response is HTTP `200 OK` with the inventory fields and the confirmed reservation fields. Its `reservationStatus` is `CONFIRMED`.

### Reservation lifecycle

The supported reservation transitions are:

```text
ACTIVE -> RELEASED
ACTIVE -> CONFIRMED
```

A released or confirmed reservation is no longer active and cannot be released or confirmed again.

Reservation-ID-based release and confirmation are the preferred contracts for Order and future Saga orchestration. The quantity-based release and confirmation endpoints also exist, but they do not identify one exact reservation and should not be used for this orchestration.

### `GET /api/inventory/product/{productId}`

Returns the inventory row for one product. No request body or custom header is required.

The response contains:

- `id`
- `productId`
- `quantity`
- `reservedQuantity`
- `status`
- `updatedAt`

Callers can calculate available stock as `quantity - reservedQuantity`. This endpoint does not create a reservation and does not return reservation-specific fields for a normal inventory lookup.

### Inventory errors

The current Inventory implementation uses these relevant error codes:

| Error code | Meaning | HTTP status |
|---|---|---|
| `INSUFFICIENT_STOCK` | The requested reservation exceeds available stock. | `400 Bad Request` via the inventory state error handler. |
| `RESERVATION_NOT_FOUND` | The supplied reservation ID does not exist. | `404 Not Found`. |
| `INVALID_RESERVATION_STATE` | The reservation or inventory state does not permit release or confirmation. | `400 Bad Request`. |
| `INVENTORY_NOT_FOUND` | No inventory row exists for the product. | `404 Not Found`. |
| `PRODUCT_SERVICE_UNAVAILABLE` | Inventory could not reach Product Service while validating a product. | `503 Service Unavailable`. |

Invalid request fields are reported as validation errors with HTTP `400 Bad Request`. The Inventory controller does not define a separate error code for Inventory Service itself being unavailable; a caller may observe a connection failure or HTTP 5xx response and must classify that transport/service failure in its own integration layer.

## 3. Cart Service Contract

### `GET /api/carts/{userId}/active`

Returns the active cart for a user.

| Property | Contract |
|---|---|
| Purpose | Read the user's current active cart for display or future checkout orchestration. |
| HTTP method | `GET` |
| Path | `/api/carts/{userId}/active` |
| Request requirements | `userId` must be a valid UUID in the path. No request body is used. |

#### Response

The response is a `CartResponse` containing:

| Field | Type / meaning |
|---|---|
| `id` | Cart UUID. |
| `userId` | Cart owner UUID. |
| `status` | Cart status. |
| `items` | List of cart items. |
| `total` | Calculated cart total. |

Each cart item contains:

| Field | Type / meaning |
|---|---|
| `id` | Cart item UUID. |
| `productId` | Product UUID. |
| `quantity` | Requested cart quantity. |
| `unitPrice` | Unit price stored in the cart. |

Cart currently obtains product data from Product Service and stores the Product Service price when items are added or updated. Cart also checks currently available inventory when quantities are changed.

Cart currently does not reserve inventory. Cart currently does not provide a checkout or cart-finalization endpoint.

## 4. Service-to-Service Integration Rules

- No service may directly access another service's database or schema.
- Product information must be obtained through Product Service.
- Inventory information must be obtained through Inventory Service.
- Cart information must be obtained through Cart Service.
- Cross-schema foreign keys must not be used.
- The `price` returned by Product Service is authoritative.
- Do not rely on a client-supplied product price when authoritative Product data is available.
- Inventory orchestration must use `reservationId` rather than quantity-based release or confirmation.
- Supply `orderId` when creating an Inventory reservation if an Order exists.
- Product Service and Inventory Service currently do not require internal headers for these contracts.
- Cart's User Service integration currently uses the `X-Internal-Service-Secret` header. This is an existing Cart-to-User-Service integration detail, not a requirement for Product or Inventory calls.

## 5. Order Integration Notes

### Current capability

Order Service currently does **not** call Product Service, Inventory Service, or Cart Service. Current Order creation accepts item data directly and persists the supplied item price, defaulting a missing price to zero. It does not currently create or store Inventory reservations.

### Future integration flow

The expected future integration sequence is:

```text
Product validation/price lookup
        |
        v
Inventory reservation with orderId
        |
        v
Persist reservationId per order item
        |
        v
Payment/order processing
        |
        v
Confirm or release the exact reservation
```

This section documents a target integration contract only. It does not implement the flow.

## 6. Out of Scope

This document does not implement or define changes for:

- Kafka
- Saga
- Event publishing
- Retries
- Idempotency
- Payment integration
- Authentication changes
- Order Service changes

## 7. Contract Compatibility Summary

| Integration | Current endpoint available | Compatible now | Owner |
|---|---|---|---|
| Order -> Product | `GET /api/books/{id}` | Yes for future lookup; Order does not currently call it. | Product Service / Member 2 |
| Order -> Inventory | `POST /api/inventory/reserve`, `POST /api/inventory/release/{reservationId}`, `POST /api/inventory/confirm/{reservationId}` | Yes for reservation-ID orchestration; Order does not currently call or persist reservations. | Inventory Service / Member 2 |
| Order -> Cart | `GET /api/carts/{userId}/active` | Yes for read-only cart access; no checkout/finalization operation exists. Order does not currently call it. | Cart Service / Member 2 |

## Existing Contract Ambiguities

- Product Service has a structured not-found response, but no Product-side `SERVICE_UNAVAILABLE` error contract. Callers must handle connection failures and upstream HTTP 5xx responses themselves.
- Cart translates a Product Service 404 into its own `PRODUCT_NOT_FOUND` integration error; that is not the Product Service error code.
- Inventory Service has a structured `PRODUCT_SERVICE_UNAVAILABLE` error for failures while Inventory validates a product, but it does not define a separate Inventory-self-unavailable response contract.
- Cart requires a `unitPrice` in its add-item request, but Cart obtains and stores the authoritative Product price internally. The request field is therefore accepted at the API boundary but is not the final source of price truth.
- The checked-in Order documentation describes authentication and JWT-derived user identity, while the current Order controller and request DTO accept `userId` directly. This document records the current Member 2 contracts and does not resolve that Order-side discrepancy.
