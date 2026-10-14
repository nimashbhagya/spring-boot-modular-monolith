# Modular Store - Spring Boot Modular Monolith



## Package Layout

```text
src/main/java/com/example/modularstore/
│
├── ModularStoreApplication.java              # Main Application Class (@EnableAsync)
│
├── config/
│   └── MultiSchemaFlywayConfig.java          # Custom multi-schema Flyway runner
│
├── customer/
│   ├── CustomerApi.java                      # Public interface contract
│   ├── CustomerResponse.java                 # Public response record (DTO)
│   └── internal/                             # Hidden module internals
│       ├── Customer.java                     # JPA Entity mapped to customer_schema
│       ├── CustomerRepository.java
│       ├── CustomerController.java
│       └── CustomerApiImpl.java              # Implementation of CustomerApi
│
├── order/
│   ├── OrderPlacedEvent.java                 # Public Domain Event record
│   └── internal/
│       ├── Order.java                        # JPA Entity mapped to order_schema
│       ├── OrderRepository.java
│       └── OrderController.java              # Validates customer & fires event
│
└── notification/
    └── internal/
        └── OrderNotificationListener.java    # Listens to OrderPlacedEvent asynchronously
```

---

## Database Architecture & Flyway Setup

### Schema Layout
```text
modular_store_db
├── customer_schema
│   ├── flyway_schema_history                 # Dedicated Customer migration history
│   └── customers                             # ID, Name, Email, Created At
│
└── order_schema
    ├── flyway_schema_history                 # Dedicated Order migration history
    └── orders                                # ID, Customer ID (Scalar), Total, Status
```

### Migration Locations
- `src/main/resources/db/migration/customer/V1__create_customers_table.sql`
- `src/main/resources/db/migration/order/V1__create_orders_table.sql`

Flyway auto-creates these schemas and runs versioned migrations independently at application startup.

---

## Prerequisites & Setup

### Prerequisites
- **Java:** JDK 17 or 21 installed (`java -version`)
- **Database:** PostgreSQL 16+ running locally or in Docker
- **Build Tool:** Maven Wrapper (bundled inside repository)

### 1. Database Creation
In `psql` or pgAdmin, create the empty database:
```sql
CREATE DATABASE modular_store_db;
```
*(No need to manually create schemas or tables; Flyway handles schema generation automatically on startup).*

### 2. Configure Credentials
Update your `src/main/resources/application.yml` with your local PostgreSQL password:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/modular_store_db
    username: postgres
    password: your_postgres_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: false # Handled via custom MultiSchemaFlywayConfig

server:
  error:
    include-message: always
    include-binding-errors: always
```

### 3. Build & Run
Run the application using the Maven wrapper:

**Windows (PowerShell / Command Prompt):**
```cmd
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**
```bash
./mvnw spring-boot:run
```

---

## API Specification & Verification

### 1. Register a Customer
- **Endpoint:** `POST /api/customers`
- **Request Body:**
  ```json
  {
    "name": "Kasun Perera",
    "email": "kasun@example.com"
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "id": 1,
    "name": "Kasun Perera",
    "email": "kasun@example.com"
  }
  ```

---

### 2. Create an Order (Valid Customer)
- **Endpoint:** `POST /api/orders`
- **Request Body:**
  ```json
  {
    "customerId": 1,
    "amount": 3500.00
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "id": 1,
    "customerId": 1,
    "totalAmount": 3500.00,
    "status": "COMPLETED"
  }
  ```
- **Console Log (Asynchronous Event Consumer):**
  ```text
  [NOTIFICATION THREAD: task-1] Event received for Order #1
  --> Notification: Email sent to kasun@example.com for order payment of LKR 3500.00
  ```

---

### 3. Create an Order (Invalid Customer ID - Validation Test)
- **Endpoint:** `POST /api/orders`
- **Request Body:**
  ```json
  {
    "customerId": 9999,
    "amount": 1500.00
  }
  ```
- **Response (`400 Bad Request`):**
  ```json
  {
    "timestamp": "2026-10-14T16:50:00.000Z",
    "status": 400,
    "error": "Bad Request",
    "message": "Customer not found with ID: 9999",
    "path": "/api/orders"
  }
  ```


