# Cart Service -> Inventory Service Postman Guide

This guide tests the implemented synchronous availability check in Cart Service.

## 1. Source-of-truth contracts

### Service base URLs

The current application configuration uses:

| Service | Base URL |
|---|---|
| User Service | `http://localhost:8081` |
| Product Service | `http://localhost:8082` |
| Inventory Service | `http://localhost:8083` |
| Cart Service | `http://localhost:8084` |

These examples call the services directly. The API Gateway is not required for these tests.

### Cart endpoints used by the tests

| Purpose | Method | Exact URL |
|---|---|---|
| Create or get active cart | `POST` | `http://localhost:8084/api/carts/{{userId}}/active` |
| Read active cart | `GET` | `http://localhost:8084/api/carts/{{userId}}/active` |
| Add cart item | `POST` | `http://localhost:8084/api/carts/{{userId}}/active/items` |
| Set an existing item quantity | `PUT` | `http://localhost:8084/api/carts/{{userId}}/active/items/{{productId}}` |

Cart item creation requires this exact JSON shape:

```json
{
  "productId": "{{productId}}",
  "quantity": 2,
  "unitPrice": 25.00
}
```

`productId`, `quantity`, and `unitPrice` are required. `quantity` must be positive and `unitPrice` must not be negative.

Quantity update requires this exact JSON shape:

```json
{
  "quantity": 4
}
```

### Inventory endpoint called by Cart

Cart's `InventoryServiceClient` calls exactly:

```http
GET http://localhost:8083/api/inventory/product/{{productId}}
```

The response shape is:

```json
{
  "id": "inventory-uuid",
  "productId": "product-uuid",
  "quantity": 10,
  "reservedQuantity": 2,
  "status": "AVAILABLE",
  "updatedAt": "2026-09-02T10:00:00"
}
```

Cart calculates:

```text
availableStock = quantity - reservedQuantity
```

For an add operation, Cart validates the final quantity:

- New product in the cart: `finalQuantity = requested quantity`
- Existing product in the cart: `finalQuantity = current cart quantity + requested quantity`

For an update operation:

- `finalQuantity = request.quantity`

Inventory is read before the Cart is saved. Inventory is not reserved or changed by these Cart endpoints.

## 2. Postman setup

Create these Postman environment variables:

| Variable | Value |
|---|---|
| `userId` | UUID returned by User registration |
| `productId` | Existing or newly created Product UUID |
| `productPrice` | Product `price` returned by Product Service |
| `cartId` | UUID returned by active-cart creation |
| `insufficientProductId` | Product UUID used for the insufficient-stock test |
| `missingInventoryProductId` | Product UUID with no Inventory record |
| `updateProductId` | Product UUID used for update tests |

For JSON requests, set this header unless stated otherwise:

```http
Content-Type: application/json
```

Cart endpoints do not declare an Authorization header. The Cart-to-User client uses the configured `X-Internal-Service-Secret` internally when Cart checks whether the user exists; that header is not required on the Postman request to Cart.

## 3. Prepare the test data

Run the User, Product, Inventory, and Cart services before starting. Ensure PostgreSQL is running and the services can connect to `ecommerce_db`.

Use separate product UUIDs for separate scenarios where the inventory state must not overlap.

### 3.1 Create or obtain a user

If a suitable user already exists, use its UUID and skip this request. Otherwise send:

```http
POST http://localhost:8081/api/auth/register
Content-Type: application/json
```

```json
{
  "firstName": "Cart",
  "lastName": "Tester",
  "email": "cart-tester-20260902@example.com",
  "password": "password123",
  "phoneNumber": "5550100001"
}
```

Expected status: `201 Created`.

The response is wrapped and includes the UUID to store as `{{userId}}`:

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "userId": "user-uuid",
    "firstName": "Cart",
    "lastName": "Tester",
    "email": "cart-tester-20260902@example.com",
    "phoneNumber": "5550100001",
    "role": "CUSTOMER",
    "isActive": true
  }
}
```

If the email already exists, use a new email or reuse the existing user's UUID.

### 3.2 Create or obtain products

Cart validates every product through Product Service before asking Inventory Service for stock. Use existing books if available:

```http
GET http://localhost:8082/api/books?page=0&size=100
```

Copy a book `id` into a Postman product variable and copy its `price` into `{{productPrice}}`.

If a new book is required, Product Service exposes:

```http
POST http://localhost:8082/api/books
Content-Type: application/json
```

The exact request body is:

```json
{
  "isbn": "9780134494166",
  "title": "Cart Inventory Test Book",
  "description": "Book used for Cart to Inventory manual testing",
  "authorId": "{{authorId}}",
  "categoryId": "{{categoryId}}",
  "price": 25.00
}
```

`authorId` and `categoryId` must already exist in Product Service. The response status is `201 Created`; store the response `id` as the relevant product variable and `price` as `{{productPrice}}`.

The Cart request's `unitPrice` is required and should match the Product Service price. Cart itself then uses the price returned by Product Service when creating or updating the item.

At minimum, prepare these products:

- `{{productId}}`: normal product for the new-product success test
- `{{insufficientProductId}}`: product for the insufficient-stock add test
- `{{missingInventoryProductId}}`: valid Product Service product with no inventory row
- `{{updateProductId}}`: product for update tests

These may be four separate products or one product reused only after its state is deliberately reset.

### 3.3 Create inventory records

Inventory creation validates that the product exists in Product Service. For each product that needs inventory, send:

```http
POST http://localhost:8083/api/inventory
Content-Type: application/json
```

```json
{
  "productId": "{{productId}}",
  "quantity": 10
}
```

Expected status: `201 Created`. The created record has `reservedQuantity: 0`, and status is `AVAILABLE` when quantity is greater than zero.

Repeat with the relevant product variable for `{{insufficientProductId}}` and `{{updateProductId}}`. Recommended starting values:

| Product | Quantity | Reserved quantity | Available stock |
|---|---:|---:|---:|
| `{{productId}}` | 10 | 0 | 10 |
| `{{insufficientProductId}}` | 3 | 0 | 3 |
| `{{updateProductId}}` | 5 | 0 | 5 |
| `{{missingInventoryProductId}}` | no row | n/a | n/a |

To change an existing inventory quantity before a test, use:

```http
PUT http://localhost:8083/api/inventory/stock/{{productId}}
Content-Type: application/json
```

```json
{
  "quantity": 10
}
```

The `PUT` value is the total quantity, not an increment. It cannot be lower than the current `reservedQuantity`.

Confirm the exact stock values before each test:

```http
GET http://localhost:8083/api/inventory/product/{{productId}}
```

### 3.4 Create the active cart

For a clean cart, send:

```http
POST http://localhost:8084/api/carts/{{userId}}/active
```

No request body is used. No Authorization header is required.

Expected status: `201 Created` and a response such as:

```json
{
  "id": "cart-uuid",
  "userId": "user-uuid",
  "status": "ACTIVE",
  "items": [],
  "total": 0.00
}
```

Store `id` as `{{cartId}}`. If an active cart already exists, this endpoint returns the existing active cart; use `DELETE /api/carts/{{userId}}/active` between scenarios when a clean cart is needed.

## 4. Test cases

## Test 1: Add new product with sufficient inventory

### Purpose
Verify that a valid product can be added when the requested quantity is no greater than available inventory.

### Prerequisites

- User exists and `{{userId}}` is valid.
- Product Service contains `{{productId}}`.
- Inventory exists for that product with `quantity: 10` and `reservedQuantity: 0`.
- Active Cart exists and does not already contain `{{productId}}`.
- `{{productPrice}}` equals the Product Service `price`.

### Request

```http
POST http://localhost:8084/api/carts/{{userId}}/active/items
Content-Type: application/json
```

```json
{
  "productId": "{{productId}}",
  "quantity": 2,
  "unitPrice": {{productPrice}}
}
```

### Expected result

Expected HTTP status: `200 OK`.

Expected response shape:

```json
{
  "id": "cart-uuid",
  "userId": "user-uuid",
  "status": "ACTIVE",
  "items": [
    {
      "id": "cart-item-uuid",
      "productId": "product-uuid",
      "quantity": 2,
      "unitPrice": 25.00
    }
  ],
  "total": 50.00
}
```

The UUIDs and numeric total vary with the data. The important values are the product UUID, quantity `2`, active status, and correct total.

### Verify afterward

1. Send `GET http://localhost:8084/api/carts/{{userId}}/active`.
2. Confirm exactly one item for `{{productId}}` with quantity `2`.
3. Confirm Inventory still reports `quantity: 10` and `reservedQuantity: 0`; Cart availability validation is read-only.
4. In PostgreSQL, verify:

```sql
SELECT c.id AS cart_id, c.user_id, c.status,
       ci.id AS cart_item_id, ci.product_id, ci.quantity, ci.unit_price
FROM cart_schema.carts c
LEFT JOIN cart_schema.cart_items ci ON ci.cart_id = c.id
WHERE c.user_id = '<userId>'
  AND c.status = 'ACTIVE';
```

### What this proves

Cart validates the user and product, reads Inventory through the existing Inventory API, computes available stock, and saves the item only when the requested final quantity is available.

## Test 2: Add new product when requested quantity exceeds available stock

### Purpose
Verify that Cart rejects an add when requested quantity is greater than available stock and does not create a cart item.

### Prerequisites

- User exists.
- Product Service contains `{{insufficientProductId}}`.
- Inventory for that product reports `quantity: 3`, `reservedQuantity: 0`, so available stock is `3`.
- Active Cart exists and does not contain this product.

### Request

```http
POST http://localhost:8084/api/carts/{{userId}}/active/items
Content-Type: application/json
```

```json
{
  "productId": "{{insufficientProductId}}",
  "quantity": 4,
  "unitPrice": {{productPrice}}
}
```

### Expected result

Expected HTTP status: `400 Bad Request`.

Expected response:

```json
{
  "success": false,
  "message": "Insufficient stock available for product: {{insufficientProductId}}",
  "errorCode": "INSUFFICIENT_STOCK",
  "errors": {}
}
```

### Verify afterward

1. Send `GET http://localhost:8084/api/carts/{{userId}}/active`.
2. Confirm the product is absent and all pre-existing cart items are unchanged.
3. Confirm Inventory quantity and reserved quantity are unchanged.
4. Verify no row was inserted for this product:

```sql
SELECT ci.*
FROM cart_schema.cart_items ci
JOIN cart_schema.carts c ON c.id = ci.cart_id
WHERE c.user_id = '<userId>'
  AND c.status = 'ACTIVE'
  AND ci.product_id = '<insufficientProductId>';
```

Expected result: zero rows.

### What this proves

Cart compares the requested final quantity with `quantity - reservedQuantity` and rejects before any Cart mutation or save.

## Test 3: Add product with no Inventory record

### Purpose
Verify that a valid Product Service product without an Inventory row is rejected and the cart remains unchanged.

### Prerequisites

- User exists.
- Product Service contains `{{missingInventoryProductId}}`.
- Confirm no Inventory record exists for that product. Do not call Inventory creation for it.
- Active Cart exists.

### Request

```http
POST http://localhost:8084/api/carts/{{userId}}/active/items
Content-Type: application/json
```

```json
{
  "productId": "{{missingInventoryProductId}}",
  "quantity": 1,
  "unitPrice": {{productPrice}}
}
```

### Expected result

Expected HTTP status: `404 Not Found`.

Expected response:

```json
{
  "success": false,
  "message": "Inventory not found for product: {{missingInventoryProductId}}",
  "errorCode": "INVENTORY_NOT_FOUND",
  "errors": {}
}
```

### Verify afterward

1. Send `GET http://localhost:8084/api/carts/{{userId}}/active`.
2. Confirm the cart has no new item for this product.
3. Send `GET http://localhost:8083/api/inventory/product/{{missingInventoryProductId}}` and confirm Inventory itself returns `404 Not Found`.
4. Verify no Cart row exists for the product using the query from Test 2 with this product UUID.

### What this proves

Cart uses the existing Inventory read API and maps Inventory's missing-record response to a structured Cart error without changing the cart.

## Test 4: Add product while Inventory Service is unavailable

### Purpose
Verify that Cart returns a structured upstream failure when it cannot reach Inventory and does not mutate the cart.

### Prerequisites

- User exists.
- Product Service contains `{{productId}}` and remains running. Cart calls Product before Inventory.
- Active Cart exists.
- Record the cart response before the test.
- Stop the Inventory Service on port `8083`, or otherwise make `http://localhost:8083` unreachable. Do not stop Product or Cart.

### Request

```http
POST http://localhost:8084/api/carts/{{userId}}/active/items
Content-Type: application/json
```

```json
{
  "productId": "{{productId}}",
  "quantity": 1,
  "unitPrice": {{productPrice}}
}
```

### Expected result

Expected HTTP status: `502 Bad Gateway`.

Expected response:

```json
{
  "success": false,
  "message": "Inventory service unavailable for product: {{productId}}",
  "errorCode": "INVENTORY_SERVICE_UNAVAILABLE",
  "errors": {}
}
```

### Verify afterward

1. Send `GET http://localhost:8084/api/carts/{{userId}}/active`.
2. Confirm the response is identical in item membership and quantities to the pre-test cart.
3. Verify no Cart item row was inserted or changed.
4. Restart Inventory Service and confirm its health before continuing.

### What this proves

Cart does not bypass the Inventory boundary or assume stock when the Inventory dependency is unreachable. The integration failure is converted into a structured `502` response before Cart persistence.

## Test 5: Update existing item to a sufficient final quantity

### Purpose
Verify that updating an existing cart item succeeds when the requested final quantity is within available stock.

### Prerequisites

- User exists.
- Product Service contains `{{updateProductId}}`.
- Inventory reports `quantity: 5` and `reservedQuantity: 0`, so available stock is `5`.
- Active Cart contains `{{updateProductId}}` at quantity `2`.

To establish the cart item, if necessary, use Test 1 setup with this product and quantity `2`. Alternatively, send:

```http
POST http://localhost:8084/api/carts/{{userId}}/active/items
Content-Type: application/json
```

```json
{
  "productId": "{{updateProductId}}",
  "quantity": 2,
  "unitPrice": {{productPrice}}
}
```

### Request

```http
PUT http://localhost:8084/api/carts/{{userId}}/active/items/{{updateProductId}}
Content-Type: application/json
```

```json
{
  "quantity": 4
}
```

### Expected result

Expected HTTP status: `200 OK`.

Expected response shape:

```json
{
  "id": "cart-uuid",
  "userId": "user-uuid",
  "status": "ACTIVE",
  "items": [
    {
      "id": "cart-item-uuid",
      "productId": "update-product-uuid",
      "quantity": 4,
      "unitPrice": 25.00
    }
  ],
  "total": 100.00
}
```

### Verify afterward

1. Send `GET http://localhost:8084/api/carts/{{userId}}/active`.
2. Confirm the existing item's quantity is exactly `4`, not `2 + 4`.
3. Confirm Inventory remains unchanged.
4. Verify `cart_schema.cart_items.quantity = 4` for that cart item.

### What this proves

The update endpoint treats the request quantity as the final desired quantity and validates that final value before saving.

## Test 6: Update existing item to an insufficient final quantity

### Purpose
Verify that an update is rejected when the requested final quantity exceeds available stock and the existing quantity is preserved.

### Prerequisites

- Use the same state as Test 5: cart item quantity `2`.
- Inventory reports `quantity: 5` and `reservedQuantity: 0`, so available stock is `5`.
- Inventory Service and Product Service are running.

### Request

```http
PUT http://localhost:8084/api/carts/{{userId}}/active/items/{{updateProductId}}
Content-Type: application/json
```

```json
{
  "quantity": 6
}
```

### Expected result

Expected HTTP status: `400 Bad Request`.

Expected response:

```json
{
  "success": false,
  "message": "Insufficient stock available for product: {{updateProductId}}",
  "errorCode": "INSUFFICIENT_STOCK",
  "errors": {}
}
```

### Verify afterward

1. Send `GET http://localhost:8084/api/carts/{{userId}}/active`.
2. Confirm the existing item's quantity is still `2`.
3. Confirm no save changed the cart item and Inventory is unchanged.
4. Verify the database value:

```sql
SELECT ci.product_id, ci.quantity, ci.unit_price
FROM cart_schema.cart_items ci
JOIN cart_schema.carts c ON c.id = ci.cart_id
WHERE c.user_id = '<userId>'
  AND c.status = 'ACTIVE'
  AND ci.product_id = '<updateProductId>';
```

Expected result: one row with `quantity = 2`.

### What this proves

Cart validates the requested final quantity before mutating the entity or saving it, so an insufficient update cannot overwrite the existing valid quantity.

## 5. Reset procedure between scenarios

For an isolated scenario:

1. Ensure User and Product records remain available.
2. Set the Inventory quantity with `PUT /api/inventory/stock/{productId}` and confirm `reservedQuantity` is suitable for the intended available-stock calculation.
3. Delete the active cart if the scenario requires an empty cart:

```http
DELETE http://localhost:8084/api/carts/{{userId}}/active
```

4. Recreate it with `POST http://localhost:8084/api/carts/{{userId}}/active`.
5. Record the cart response before the test and compare it with the response after a rejection.
6. Never use Inventory reservation, release, or confirmation endpoints as part of these Cart availability tests; those are separate Inventory operations and are not called by Cart's `addItem` or `updateItemQuantity` implementation.

## 6. Database verification reference

Cart connects to PostgreSQL database `ecommerce_db` using schema `cart_schema`. Inventory connects to the same database using schema `inventory_schema`.

Inventory verification query:

```sql
SELECT id, product_id, quantity, reserved_quantity, status, updated_at
FROM inventory_schema.inventory
WHERE product_id = '<productId>';
```

Cart verification query:

```sql
SELECT c.id AS cart_id, c.user_id, c.status,
       ci.id AS cart_item_id, ci.product_id, ci.quantity, ci.unit_price
FROM cart_schema.carts c
LEFT JOIN cart_schema.cart_items ci ON ci.cart_id = c.id
WHERE c.user_id = '<userId>'
  AND c.status = 'ACTIVE';
```

For every rejected availability test, the Cart query must show no new row or the original item quantity. The Inventory query must show no change because Cart performs a read-only stock check.
