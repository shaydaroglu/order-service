# Customer Order Service

A Spring Boot service for creating and managing customer orders, built as a compact but production-minded backend exercise. The project focuses on explicit business rules, predictable API behavior, and clear separation between domain logic and infrastructure concerns.

## What this project demonstrates

- Clear separation between web, application, domain, and persistence concerns
- Explicit order lifecycle rules modeled in the domain
- Idempotent creation with replay and conflict handling
- External catalog validation behind an application port
- PostgreSQL persistence with Flyway-managed migrations
- Optimistic locking for concurrent updates
- Domain, service, and API-level test coverage
- Containerized local setup with Docker Compose

## Domain overview

Orders move through a constrained lifecycle:

```text
DRAFT -> PREVIEW -> SUBMITTED -> CONFIRMED
          |
          v
        DRAFT
```

The service enforces several business rules:

- confirmed orders cannot be edited
- submitted orders may only change state
- orders must contain at least one item
- duplicate product-offering IDs are rejected
- direct-debit payments require a valid IBAN

## Architecture

The service follows a ports-and-adapters style structure:

```text
adapter/in/web   REST controllers and request/response DTOs
application      Use cases and orchestration
domain           Business rules and invariants
adapter/out      Persistence and catalog integrations
```

This structure keeps business rules independent from framework code while making infrastructure concerns easier to replace and test.

## Decisions and trade-offs

### State machine

Order lifecycle rules are modeled explicitly in the domain layer instead of being spread across controllers or service methods. This keeps allowed transitions close to the business concept they describe and makes invalid transitions harder to apply accidentally from another entry point.

The current lifecycle is intentionally small:

```text
DRAFT -> PREVIEW -> SUBMITTED -> CONFIRMED
          |
          v
        DRAFT
```

This favors clarity over flexibility. A more dynamic workflow engine would allow transitions to change without code changes, but it would add complexity that is not justified for a service with a small, stable set of business states.

### Validation placement

Validation is split by responsibility:

- request-shape validation is handled at the web boundary with Bean Validation
- business-rule validation lives in the domain and application layers

For example, missing fields are rejected before entering the use case, while rules such as valid state transitions, non-empty orders, duplicate product-offering IDs, and payment-method constraints are enforced by the core model.

This separation keeps transport concerns out of the domain while still protecting invariants beyond the HTTP layer. The trade-off is that validation is not concentrated in one place, but each rule lives where it has the strongest meaning.

### PATCH semantics

`PATCH` is used for partial updates rather than requiring clients to resend the full order representation. Omitted fields remain unchanged, which reduces accidental overwrites and keeps the API convenient for clients.

The service still applies lifecycle restrictions during patching:

- draft orders may change items, payment method, or state
- submitted orders may only change state
- confirmed orders are immutable

This gives clients flexibility without weakening business rules. The trade-off is that patch behavior is more nuanced than a simple update endpoint, so editability rules must be explicit and well tested.

### Idempotency

Order creation supports an `Idempotency-Key` header because create operations are commonly retried after network failures. Repeating the same request with the same key returns the original order instead of creating a duplicate.

Idempotency is enforced in two layers:

- application logic checks whether an existing request with the same key has the same payload
- a unique database constraint protects against concurrent submissions using the same key

If the same key is reused with a different payload, the service returns a conflict instead of silently accepting ambiguous client behavior.

This adds comparison logic and a small amount of coordination with persistence, but it makes retry behavior predictable and safer for clients.

### Persistence

The domain model is kept separate from JPA entities. Persistence adapters map between the two representations so that database concerns do not leak into business logic.

Flyway manages schema evolution explicitly, and optimistic locking is enabled through a version column to protect against lost updates during concurrent modifications.

The trade-off is a small amount of mapping code, but the separation makes the domain easier to test and keeps the persistence model free to evolve independently from the API and business rules.

### Inter-service communication

Product-offering validation is delegated to a catalog service through an output port. The application depends on an abstraction, while the Retrofit-based adapter handles the actual HTTP call.

This keeps the core use case independent from a specific HTTP client and makes the integration easy to replace or mock in tests.

The current implementation is synchronous because order creation should fail fast when referenced product offerings are invalid. In a higher-throughput or less tightly coupled system, this could be revisited with asynchronous messaging, caching, or resilience patterns, but those options introduce consistency and operational trade-offs that are unnecessary for the current scope.

### Status code choices

The API uses status codes to distinguish different classes of outcomes:

- `201 Created` when a new order is created
- `200 OK` when an idempotent request is replayed successfully
- `400 Bad Request` for malformed input or invalid business operations
- `404 Not Found` when an order does not exist
- `409 Conflict` when an idempotency key is reused with a different payload
- `502 Bad Gateway` when the downstream catalog service cannot complete validation

The goal is to expose enough semantic information for clients to react correctly without collapsing all failures into one generic response.

### Error shape

Errors are returned as `application/problem+json` responses with a consistent structure containing status, title, detail, instance, and timestamp fields. Validation failures also include field-level errors.

A consistent error contract makes failures easier for clients to parse and easier to observe during debugging. The trade-off is a little more response-model code, but it avoids ad hoc error payloads that become difficult to maintain as the API grows.

## Tech stack

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway
- Retrofit / OkHttp
- Maven
- Docker Compose
- JUnit 5 / Mockito / MockMvc

## Running locally

### Prerequisites

- Java 21
- Docker and Docker Compose
- The catalog service must be started first so that the shared `services-network` Docker network exists

### Start the service

```bash
docker compose up --build
```

The API will be available at:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Example API usage

### Create an order

```bash
curl -X POST http://localhost:8080/api/v1/customer-orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-order-001" \
  -d '{
    "category": "B2B",
    "customer": { "id": "customer-1" },
    "site": { "id": "site-1" },
    "orderItems": [
      {
        "productOfferingId": "11111111-1111-1111-1111-111111111111",
        "quantity": 2
      }
    ],
    "paymentMethod": {
      "type": "INVOICE"
    }
  }'
```

### Move an order to preview

```bash
curl -X PATCH http://localhost:8080/api/v1/customer-orders/{orderId} \
  -H "Content-Type: application/json" \
  -d '{
    "status": "PREVIEW"
  }'
```

## Testing

Run the full test suite locally with:

```bash
./mvnw verify
```

Or run it inside a Java 21 container:
```bash
docker run --rm \
-v "$PWD":/app \
-w /app \
eclipse-temurin:21-jdk-alpine \
./mvnw verify 
```


The tests cover:

- domain invariants
- state transitions
- payment validation
- service-layer behavior
- REST integration scenarios
- idempotency behavior
- error handling

## Possible next steps

If this service were extended further, I would consider:

- adding authentication and authorization
- introducing structured observability and metrics
- publishing richer OpenAPI examples for all endpoints
- adding Testcontainers-based integration tests against PostgreSQL
- revisiting synchronous catalog validation with caching, retries, or asynchronous workflows where the surrounding system justified the added complexity
