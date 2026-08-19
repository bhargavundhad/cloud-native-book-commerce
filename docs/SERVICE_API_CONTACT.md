# Service API Contract

## Project

Cloud-Native Book E-Commerce Platform

## Architecture

Microservices Architecture

## Database

PostgreSQL

## Current Database Strategy

Single PostgreSQL database with schema-per-service.

Cross-service foreign keys are NOT used.

## Services

| Service | Port | Owner | Database Schema |
|---|---:|---|---|
| API Gateway | 8080 | Member 1 | None |
| User Service | 8081 | Member 1 | user_schema |
| Product Service | 8082 | Member 2 | product_schema |
| Inventory Service | 8083 | Member 2 | inventory_schema |
| Cart Service | 8084 | Member 2 | cart_schema |
| Order Service | 8085 | Member 3 | order_schema |
| Payment Service | 8086 | Member 3 | payment_schema |
| Notification Service | 8087 | Member 3 | notification_schema |

---

# 1. Common API Standards

## 1.1 Base URL

Local development:

    http://localhost:{PORT}

Example:

    http://localhost:8082

---

## 1.2 Content Type

Requests and responses use:

    application/json

---

## 1.3 Authentication

Protected APIs use:

    Authorization: Bearer <JWT>

Public APIs:

- Register
- Login
- Public book browsing
- Public categories
- Public authors

All other APIs require authentication unless explicitly specified.

---

## 1.4 User ID

All services use UUID for user identification.

The JWT `sub` claim contains the authenticated user's UUID.

Example:

    {
      "sub": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user@example.com",
      "role": "CUSTOMER"
    }

---

## 1.5 Common Success Response

    {
      "success": true,
      "message": "Operation successful",
      "data": {}
    }

---

## 1.6 Common Error Response

    {
      "success": false,
      "message": "Resource not found",
      "errorCode": "RESOURCE_NOT_FOUND"
    }

---

## 1.7 Common HTTP Status Codes

| Status | Meaning |
|---|---|
| 200 | Successful request |
| 201 | Resource created |
| 204 | Successful request with no response body |
| 400 | Bad request |
| 401 | Authentication required/invalid |
| 403 | Insufficient permission |
| 404 | Resource not found |
| 409 | Conflict |
| 422 | Validation/business rule failure |
| 500 | Internal server error |
| 503 | Service temporarily unavailable |

---

# 2. API Gateway

## Port

    8080

## Responsibility

- Single external entry point
- Request routing
- JWT authentication enforcement
- CORS
- Rate limiting
- Forward requests to appropriate services

## Routing

| External Route | Target Service |
|---|---|
| `/api/auth/**` | User Service |
| `/api/users/**` | User Service |
| `/api/books/**` | Product Service |
| `/api/categories/**` | Product Service |
| `/api/authors/**` | Product Service |
| `/api/book-listings/**` | Product Service |
| `/api/loans/**` | Product Service |
| `/api/inventory/**` | Inventory Service |
| `/api/cart/**` | Cart Service |
| `/api/orders/**` | Order Service |
| `/api/payments/**` | Payment Service |
| `/api/notifications/**` | Notification Service |

The API Gateway does not own business data.

---

# 3. User Service

## Port

    8081

## Database

    user_schema

## Tables

- roles
- users

## Responsibility

- User registration
- Authentication
- JWT generation
- User profile
- Role management
- User account status

---

## 3.1 Register

### Endpoint

    POST /api/auth/register

### Request

    {
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@example.com",
      "password": "Password@123",
      "phoneNumber": "9876543210"
    }

### Response

    {
      "success": true,
      "message": "User registered successfully",
      "data": {
        "id": "uuid",
        "firstName": "John",
        "lastName": "Doe",
        "email": "john@example.com"
      }
    }

---

## 3.2 Login

### Endpoint

    POST /api/auth/login

### Request

    {
      "email": "john@example.com",
      "password": "Password@123"
    }

### Response

    {
      "success": true,
      "message": "Login successful",
      "data": {
        "accessToken": "JWT_TOKEN",
        "tokenType": "Bearer",
        "expiresIn": 3600
      }
    }

---

## 3.3 Get Current User

### Endpoint

    GET /api/users/me

### Authentication

Required.

### Response

    {
      "success": true,
      "message": "User fetched successfully",
      "data": {
        "id": "uuid",
        "firstName": "John",
        "lastName": "Doe",
        "email": "john@example.com",
        "role": "CUSTOMER"
      }
    }

---

## 3.4 Update Current User

### Endpoint

    PUT /api/users/me

### Authentication

Required.

### Request

    {
      "firstName": "John",
      "lastName": "Smith",
      "phoneNumber": "9876543210"
    }

---

## 3.5 Get User

### Endpoint

    GET /api/users/{id}

### Authentication

Internal/service/admin usage.

### Response

    {
      "success": true,
      "message": "User fetched successfully",
      "data": {
        "id": "uuid",
        "firstName": "John",
        "lastName": "Doe",
        "email": "john@example.com",
        "role": "CUSTOMER",
        "isActive": true
      }
    }

---

## 3.6 Update User Status

### Endpoint

    PUT /api/users/{id}/status

### Authentication

Admin only.

### Request

    {
      "isActive": false
    }

---

# 4. Product Service

## Port

    8082

## Database

    product_schema

## Tables

- authors
- categories
- books
- book_listings
- loans

## Responsibility

- Book catalog
- Authors
- Categories
- Fresh books
- Second-hand book listings
- Borrowing

---

# 4.1 Get Books

### Endpoint

    GET /api/books

### Authentication

Not required.

### Query Parameters

    page
    size
    categoryId
    authorId
    search

Example:

    GET /api/books?page=0&size=10&search=java

---

# 4.2 Get Book

### Endpoint

    GET /api/books/{id}

---

# 4.3 Create Book

### Endpoint

    POST /api/books

### Authentication

Admin only.

### Request

    {
      "isbn": "9781234567890",
      "title": "Clean Architecture",
      "description": "Software architecture book",
      "authorId": "uuid",
      "categoryId": "uuid",
      "price": 599.00
    }

---

# 4.4 Update Book

### Endpoint

    PUT /api/books/{id}

### Authentication

Admin only.

---

# 4.5 Delete Book

### Endpoint

    DELETE /api/books/{id}

### Authentication

Admin only.

---

# 4.6 Categories

### Get Categories

    GET /api/categories

### Get Category

    GET /api/categories/{id}

### Create Category

    POST /api/categories

### Update Category

    PUT /api/categories/{id}

### Delete Category

    DELETE /api/categories/{id}

Admin permissions are required for create/update/delete.

---

# 4.7 Authors

### Get Authors

    GET /api/authors

### Get Author

    GET /api/authors/{id}

### Create Author

    POST /api/authors

### Update Author

    PUT /api/authors/{id}

Admin permissions are required for create/update.

---

# 4.8 Second-Hand Book Listings

A normal registered customer can create a listing for a book they want to make available for borrowing.

### Get Listings

    GET /api/book-listings

### Get Listing

    GET /api/book-listings/{id}

### Create Listing

    POST /api/book-listings

### Request

    {
      "bookId": "uuid",
      "condition": "GOOD",
      "borrowFee": 100.00,
      "borrowDurationDays": 15,
      "description": "Good condition"
    }

The owner is taken from the authenticated JWT.

The client should NOT send `ownerId`.

### Update Listing

    PUT /api/book-listings/{id}

Only the listing owner or authorized administrator can update it.

### Delete Listing

    DELETE /api/book-listings/{id}

---

# 4.9 Borrowing

### Create Loan

    POST /api/loans

### Request

    {
      "listingId": "uuid"
    }

The borrower ID is taken from the authenticated JWT.

### Get Loan

    GET /api/loans/{id}

### Get My Loans

    GET /api/loans/my

### Return Book

    POST /api/loans/{id}/return

---

# 5. Inventory Service

## Port

    8083

## Database

    inventory_schema

## Table

- inventory

## Responsibility

- Stock management
- Stock reservation
- Stock release
- Availability checking

---

# 5.1 Get Inventory

    GET /api/inventory/{productId}

---

# 5.2 Create Inventory

    POST /api/inventory

### Request

    {
      "productId": "uuid",
      "quantity": 100
    }

Admin only.

---

# 5.3 Update Inventory

    PUT /api/inventory/{productId}

### Request

    {
      "quantity": 120
    }

Admin only.

---

# 5.4 Reserve Inventory

    POST /api/inventory/{productId}/reserve

### Request

    {
      "quantity": 2
    }

### Response

    {
      "success": true,
      "message": "Inventory reserved successfully",
      "data": {
        "productId": "uuid",
        "quantity": 2,
        "status": "RESERVED"
      }
    }

---

# 5.5 Release Inventory

    POST /api/inventory/{productId}/release

### Request

    {
      "quantity": 2
    }

Used mainly for failed/cancelled transactions.

---

# 6. Cart Service

## Port

    8084

## Database

    cart_schema

## Tables

- carts
- cart_items

## Responsibility

- User cart
- Add items
- Remove items
- Update quantity
- Clear cart

---

# 6.1 Get Current Cart

    GET /api/cart

The user is identified using the JWT.

---

# 6.2 Add Cart Item

    POST /api/cart/items

### Request

    {
      "productId": "uuid",
      "quantity": 2
    }

---

# 6.3 Update Cart Item

    PUT /api/cart/items/{itemId}

### Request

    {
      "quantity": 3
    }

---

# 6.4 Remove Cart Item

    DELETE /api/cart/items/{itemId}

---

# 6.5 Clear Cart

    DELETE /api/cart

---

# 7. Order Service

## Port

    8085

## Database

    order_schema

## Tables

- orders
- order_items

## Responsibility

- Order creation
- Order history
- Order status
- Order cancellation
- Checkout orchestration

---

# 7.1 Create Order

### Endpoint

    POST /api/orders

### Authentication

Required.

### Request

    {
      "items": [
        {
          "productId": "uuid",
          "quantity": 2
        }
      ],
      "shippingAddress": "Surat, Gujarat, India"
    }

The user ID is taken from JWT.

The client should NOT send userId.

---

# 7.2 Get Order

    GET /api/orders/{id}

---

# 7.3 Get My Orders

    GET /api/orders/my

---

# 7.4 Cancel Order

    PUT /api/orders/{id}/cancel

---

# 7.5 Admin Get Orders

    GET /api/orders

---

# 7.6 Update Order Status

    PUT /api/orders/{id}/status

### Request

    {
      "status": "CONFIRMED"
    }

Admin/internal usage.

---

# 8. Payment Service

## Port

    8086

## Database

    payment_schema

## Table

- payments

## Responsibility

- Payment processing
- Payment status
- Refunds
- Payment gateway integration

---

# 8.1 Create Payment

    POST /api/payments

### Request

    {
      "referenceType": "ORDER",
      "referenceId": "uuid",
      "amount": 1198.00,
      "currency": "INR",
      "paymentMethod": "MOCK"
    }

---

# 8.2 Get Payment

    GET /api/payments/{id}

---

# 8.3 Get Order Payment

    GET /api/payments/order/{orderId}

---

# 8.4 Refund Payment

    POST /api/payments/{id}/refund

---

# 9. Notification Service

## Port

    8087

## Database

    notification_schema

## Table

- notifications

## Responsibility

- Email notification
- SMS notification
- Push notification
- Notification history

---

# 9.1 Create Notification

    POST /api/notifications

### Request

    {
      "userId": "uuid",
      "type": "EMAIL",
      "eventType": "ORDER_CREATED",
      "message": "Your order has been created."
    }

---

# 9.2 Get Notification

    GET /api/notifications/{id}

---

# 9.3 Get User Notifications

    GET /api/notifications/user/{userId}

---

# 10. Service-to-Service Communication

## Current Phase

Initially, services can communicate using REST.

Example:

    Order Service
          |
          | REST
          v
    Inventory Service

    Order Service
          |
          | REST
          v
    Payment Service

---

# 11. Authentication Flow

    Client
      |
      | Login
      v
    API Gateway
      |
      v
    User Service
      |
      | JWT
      v
    Client

For protected requests:

    Client
      |
      | Bearer JWT
      v
    API Gateway
      |
      | Valid JWT
      v
    Target Service

---

# 12. User Validation

Services must not access the User Service database directly.

Example:

    Order Service
          |
          | GET /api/users/{userId}
          v
    User Service
          |
          v
    User information

No cross-service database access is allowed.

---

# 13. Important Data Ownership Rules

Each service owns its own data.

| Service | Owns |
|---|---|
| User | Users and roles |
| Product | Books, authors, categories, listings, loans |
| Inventory | Inventory |
| Cart | Carts and cart items |
| Order | Orders and order items |
| Payment | Payments |
| Notification | Notifications |

No service directly accesses another service's tables.

---

# 14. Checkout Flow

Initial synchronous implementation:

    Client
      |
      v
    API Gateway
      |
      v
    Order Service
      |
      +----> User Service
      |
      +----> Product Service
      |
      +----> Inventory Service
      |
      +----> Payment Service
      |
      +----> Notification Service

Later this flow will be improved using:

- Kafka
- Saga Pattern
- Asynchronous events

---

# 15. Future Kafka Events

The following events are planned for the asynchronous architecture:

    OrderCreated
    InventoryReserved
    InventoryReleased
    PaymentCompleted
    PaymentFailed
    OrderConfirmed
    OrderCancelled
    NotificationRequested

The exact event schema will be finalized before Kafka implementation.

---

# 16. Future Saga Flow

Planned order Saga:

    OrderCreated
          |
          v
    Reserve Inventory
          |
          v
    Process Payment
          |
          +---- Payment Success
          |          |
          |          v
          |     Confirm Order
          |
          +---- Payment Failure
                     |
                     v
               Release Inventory
                     |
                     v
               Cancel Order

---

# 17. API Contract Rules

1. Do not change an existing endpoint without informing the team.
2. Do not change request/response JSON silently.
3. Use UUID consistently.
4. Do not expose database entities directly as API responses.
5. Use DTOs for API requests/responses.
6. Do not access another service's database directly.
7. Do not send passwords between services.
8. Do not send userId from the client when it can be obtained from JWT.
9. Use consistent HTTP status codes.
10. Document new APIs before integrating them.
11. Any breaking API change requires team agreement.
12. Kafka event contracts will be documented separately.

---

# 18. Service Ownership

### Member 1

- User Service
- API Gateway

### Member 2

- Product Service
- Inventory Service
- Cart Service

### Member 3

- Order Service
- Payment Service
- Notification Service

---

# 19. Implementation Priority

## Phase 1

- User Service
- Product Service
- Inventory Service
- Cart Service
- Order Service
- Payment Service
- Notification Service

## Phase 2

- API Gateway integration

## Phase 3

- Docker Compose

## Phase 4

- Kafka

## Phase 5

- Saga Pattern

## Phase 6

- Kubernetes

## Phase 7

- CI/CD

## Phase 8

- Monitoring, logging and security