# Order Service — Backend Technical Assessment

A production-quality backend service for managing e-commerce orders through their full lifecycle. Built with clean architecture principles, emphasising engineering quality, correctness, and extensibility over feature volume.

## Technology Stack

| Component | Version |
|---|---|
| **Java** | 21 (LTS) |
| **Spring Boot** | 4.1.0 |
| **Hibernate ORM** | 7.4.1.Final |
| **Build System** | Maven |
| **Database** | H2 (in-memory) |
| **Validation** | Hibernate Validator (Jakarta Bean Validation 3.1) |

---

## Getting Started

### Prerequisites

- **Java 21** or later — verify with `java -version`
- **Maven** is bundled via the Maven Wrapper (`mvnw` / `mvnw.cmd`) — no separate installation needed

### Build

```bash
# Unix/macOS
./mvnw clean package

# Windows
.\mvnw.cmd clean package
```

### Run

```bash
# Unix/macOS
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

The service starts at **http://localhost:8080**.

### Run Tests

All tests can be run with a single command:

```bash
# Unix/macOS
./mvnw test

# Windows
.\mvnw.cmd test
```

### Accessing the Database (H2 Console)

You can view the in-memory database directly via the H2 Console while the application is running:

1. Open a browser and navigate to: **http://localhost:8080/h2-console**
2. Use the following credentials to log in:
   - **JDBC URL:** `jdbc:h2:mem:orderdb`
   - **User Name:** `sa`
   - **Password:** *(leave blank)*

---

## API Reference (Testing with Postman)

Base URL: `http://localhost:8080/api/orders`

Below are the step-by-step instructions to test all CRUD endpoints using Postman. For all requests that require a JSON body, ensure you select **Body > raw > JSON** in Postman.

### 1. Create an Order
- **Method:** `POST`
- **URL:** `http://localhost:8080/api/orders`
- **Body:**
```json
{
  "customerName": "Budi Santoso",
  "items": [
    { "productName": "Laptop Stand", "quantity": 1, "unitPrice": 25.50 },
    { "productName": "Wireless Mouse", "quantity": 2, "unitPrice": 15.00 }
  ]
}
```
**Response:** `201 Created` — note the `orderId` in the response, you will need it for the next steps!

### 2. Get a Single Order
- **Method:** `GET`
- **URL:** `http://localhost:8080/api/orders/{orderId}` (replace `{orderId}` with the ID from step 1)

**Response:** `200 OK` with the order details.

### 3. List Orders (Paginated & Sorted)
- **Method:** `GET`
- **URL:** `http://localhost:8080/api/orders?page=0&size=10&sort=highest_total`
- **Available Sort Options:** `newest`, `highest_total`, `oldest_unpaid`

**Response:** `200 OK` with a paginated array of orders.

### 4. Update an Order
- **Method:** `PUT`
- **URL:** `http://localhost:8080/api/orders/{orderId}`
- **Body:**
```json
{
  "customerName": "Budi Santoso (Updated)",
  "items": [
    { "productName": "Mechanical Keyboard", "quantity": 1, "unitPrice": 120.00 }
  ]
}
```
**Response:** `200 OK` with the updated order. *(Will fail with 409 Conflict if order is already PAID)*.

### 5. Transition Order Status (e.g., Pay Order)
- **Method:** `PATCH`
- **URL:** `http://localhost:8080/api/orders/{orderId}/status`
- **Body:**
```json
{
  "status": "PAID"
}
```
**Response:** `200 OK`.

### 6. Cancel an Order (Requires Reason)
- **Method:** `PATCH`
- **URL:** `http://localhost:8080/api/orders/{orderId}/status`
- **Body:**
```json
{
  "status": "CANCELLED",
  "reason": "Customer changed their mind"
}
```
**Response:** `200 OK` if valid, or `409 Conflict` if trying to cancel an already PAID/SHIPPED order.

### 7. Delete an Order
- **Method:** `DELETE`
- **URL:** `http://localhost:8080/api/orders/{orderId}`

**Response:** `204 No Content`.

---

## Error Response Format

All errors follow a consistent structure:

```json
{
  "timestamp": "2026-07-08T11:45:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": [
    "customerName: Customer name is required",
    "items: An order must contain at least one item"
  ]
}
```

| HTTP Status | When |
|---|---|
| `400 Bad Request` | Validation failure, malformed input |
| `404 Not Found` | Order ID does not exist |
| `409 Conflict` | Illegal status transition, items locked after payment |

---

## Design Decisions

### 1. `totalAmount` is Server-Computed

The `totalAmount` is calculated exclusively on the server by iterating `quantity × unitPrice` across all line items using `BigDecimal` arithmetic. This prevents price-manipulation attacks where a malicious client submits a lower total. The field has no public setter; it is recalculated before every persist operation.

### 2. BigDecimal for All Monetary Values

All prices and amounts use `java.math.BigDecimal` with `DECIMAL(10,2)` column precision. This avoids the rounding errors inherent in `float`/`double` and is standard practice for financial calculations.

### 3. Encapsulation Over Convenience — No Lombok

Getter/setter generation was done manually to enforce strict access control:
- **No public setter** for `orderId`, `totalAmount`, `createdAt`, `updatedAt`
- **Package-private setter** for `OrderItem.setOrder()` to enforce bidirectional sync
- **Unmodifiable list** returned by `Order.getItems()` to prevent bypassing aggregate rules

This means even if Jackson deserialisation is attempted, there is no setter pathway to override server-controlled fields.

### 4. OrderStatus Enum as a State Machine

Each `OrderStatus` constant defines its own `allowedTransitions()`, implementing the Strategy Pattern within the enum. This design choice was driven by Part 2's requirement for constrained transitions. Benefits:
- Adding a new status requires adding one enum constant — existing transition rules remain untouched (Open/Closed Principle)
- `canTransitionTo()` centralises validation, reducing scattered `if-else` logic
- `areItemsLocked()` encodes the Part 2 item-immutability rule at the domain level

### 5. Aggregate Root Pattern (DDD)

`Order` is the aggregate root; `OrderItem` has no dedicated repository. All item lifecycle management flows through `Order.addOrderItem()` / `removeOrderItem()` with `CascadeType.ALL` and `orphanRemoval = true`. This ensures:
- Referential integrity is maintained by the domain model, not just the database
- Bidirectional sync is never broken

### 6. Part 2 Requirements Influenced Part 1 Design

Before writing any code, the Part 2 requirements were analysed and baked into the Part 1 data model:

| Part 2 Requirement | Part 1 Preparation |
|---|---|
| Constrained status transitions | `OrderStatus.allowedTransitions()` + `canTransitionTo()` |
| Cancel requires a reason | Extensible DTO design — `StatusTransitionRequest` accepts optional metadata |
| Items immutable after PAID | `areItemsLocked()` + `getItems()` returns unmodifiable view |
| Extensible sort strategies | `OrderRepository` extends `JpaRepository` (paging/sorting built in) |

### 7. Assumptions Made

- **Currency:** Single-currency system (no currency field). All amounts are in the same unit.
- **Rounding:** `BigDecimal` multiplication preserves precision; no explicit rounding mode applied at the entity level. The column constrains to 2 decimal places.
- **Authentication:** No auth is implemented. In production, Spring Security with JWT would be added.
- **Concurrency:** Optimistic locking (`@Version`) is not implemented in the initial version but would be added for production use to prevent lost updates.
- **Soft Delete:** Orders are hard-deleted per specification. A `deletedAt` soft-delete pattern would be preferred in production.

### 8. What I Would Improve Given More Time

- **Optimistic Locking** — Add `@Version` to prevent concurrent update conflicts
- **Audit Log** — Track all status transitions with timestamp and actor, and persist cancellation reasons for compliance and debugging.
- **Pagination Metadata** — Include `totalElements`, `totalPages`, `hasNext` in list responses
- **API Versioning** — Use URI versioning (`/api/v1/orders`) for forward compatibility
- **OpenAPI/Swagger** — Auto-generated API documentation
- **Integration Tests with Testcontainers** — Test against a real database for higher confidence

---

## Project Structure

```
com.assessment.orderservice
├── entity/                  # JPA entities and enums
│   ├── Order.java           # Aggregate root
│   ├── OrderItem.java       # Line item (child entity)
│   └── OrderStatus.java     # Lifecycle state machine
├── repository/              # Data access layer
│   └── OrderRepository.java
├── dto/                     # Request/response DTOs
├── exception/               # Custom exceptions
├── service/                 # Business logic
├── controller/              # REST endpoints + error handling
└── sort/                    # Extensible sort strategies (Part 2)
```

---

## License

This project was created as part of a technical assessment and is not licensed for distribution.
