# Smart E-Commerce System

A Spring Boot 3.x e-commerce application with RESTful APIs, GraphQL integration, AOP cross-cutting concerns, and comprehensive validation.

## Tech Stack

- **Framework**: Spring Boot 3.2.1
- **Language**: Java 21
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **API**: REST + GraphQL (coexisting on same port)
- **Docs**: Springdoc OpenAPI (Swagger UI)
- **GraphQL Console**: GraphiQL page 
- **AOP**: Spring AOP (logging + performance monitoring)
- **Caching**: Spring Cache (ConcurrentMapCache)
- **Validation**: Jakarta Bean Validation + custom validators
- **Transactions**: Declarative transaction management (@Transactional)
- **Reporting**: Automated Performance, Validation, and System Documentation reports

---

## Setup Instructions

### Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 14+

### Database Setup
1. Create a PostgreSQL database:
   ```sql
   CREATE DATABASE ecommerce_db;
   ```
2. Run the schema script:
   ```bash
   psql -U postgres -d ecommerce_db -f sql/schema.sql
   ```
3. (Optional) Seed data:
   ```bash
   psql -U postgres -d ecommerce_db -f sql/seed.sql
   ```
### Running with Profiles

```bash
# Development (default)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Test (H2 in-memory database)
mvn spring-boot:run -Dspring-boot.run.profiles=test

# Set production environment variables (URL, USERNAME, PASSWORD) as per your environment
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Running Backend and Frontend Separately

```bash
# Terminal 1: backend API server (REST/GraphQL on port 8080)
mvn spring-boot:run

# Terminal 2: JavaFX frontend only
mvn javafx:run
```

**Note:** `javafx:run` starts a non-web Spring context for UI wiring and does not start the embedded web server.

---

## 🔒 Security Configuration

The Smart E-Commerce System implements multi-layered security using Spring Security 6.x.

### 🔑 Authentication Flow
1. **JWT (Stateless)**:
   - **Login**: POST to `/api/auth/login` with email and password. Returns a signed JWT.
   - **Usage**: Include the token in the `Authorization` header as `Bearer <token>` for all protected REST/GraphQL requests.
   - **Claims**: Includes `sub` (email) and `roles`.
   - **Blacklisting**: Logged-out tokens are added to an in-memory blacklist using `ConcurrentHashMap` and checked by the `JwtAuthenticationFilter`.

2. **OAuth2 (Google)**:
   - **Provider**: Google OAuth2.
   - **Flow**: Redirect to `/oauth2/authorization/google`. Upon success, `OAuth2LoginSuccessHandler` syncs user data with the database and assigns the `CUSTOMER` role.

### 🛡️ Access Control (RBAC)
Endpoints are secured using `@PreAuthorize` annotations based on user roles:
- **ADMIN**: Product management (Create/Update/Delete), User management, Order management.
- **STAFF**: Order management, Product viewing.
- **CUSTOMER**: Profile viewing/updating, Order history, Cart management.

| Endpoint | Role Required |
|----------|---------------|
| `/api/auth/**` | Public |
| `/api/products` (GET) | Public |
| `/api/products` (POST/PUT/DELETE) | `ADMIN` |
| `/api/orders` | `ADMIN`, `STAFF` |
| `/api/users/**` | `ADMIN` or Self |
| `/graphql` | Public (Queries) / Secured (Mutations) |

### 🌐 CORS & CSRF
- **CORS**: Configured to allow `localhost:4200` (Angular) and `localhost:3000` (React).
- **CSRF**: Disabled for `/api/**` and GraphQL as they use stateless JWT tokens which are not susceptible to traditional CSRF attacks.

### 🛠️ Testing Instructions
1. **Postman**:
   - Import the provided Postman collection.
   - Use the login request to obtain a token.
   - Set the `token` environment variable for subsequent requests.
2. **Swagger UI**:
   - Access `/swagger-ui/index.html`.
   - Use the "Authorize" button to enter your Bearer token (without the prefix).
3. **Logs**:
   - Check application logs for "SecurityLoggingAspect" entries to monitor auth events.

---

## API Endpoints

### Base URL
```
http://localhost:8080
```
### Swagger UI (OpenAPI Documentation)
```
http://localhost:8080/swagger-ui.html
```
### GraphQL Endpoint
```
http://localhost:8080/graphql
```
### GraphiQL Interactive Explorer
```
http://localhost:8080/graphiql
```
### h2 on test profile
http://localhost:8081/h2-console
```
## REST API Reference

### Users (`/api/users`)

| Method | Endpoint           | Description              |
|--------|--------------------|--------------------------|
| GET    | `/api/users`       | List users (paginated)   |
| GET    | `/api/users/{id}`  | Get user by ID           |
| POST   | `/api/users`       | Create a new user        |
| PUT    | `/api/users/{id}`  | Update a user            |
| DELETE | `/api/users/{id}`  | Delete a user            |

### Products (`/api/products`)

| Method | Endpoint              | Description                 |
|--------|-----------------------|-----------------------------|
| GET    | `/api/products`       | List products (paginated, filterable) |
| GET    | `/api/products/{id}`  | Get product by ID           |
| POST   | `/api/products`       | Create a new product        |
| PUT    | `/api/products/{id}`  | Update a product            |
| DELETE | `/api/products/{id}`  | Delete a product            |

**Query Parameters for GET `/api/products`**:
- `page` (default: 0) — Page number
- `size` (default: 10) — Page size
- `sortBy` (default: name) — Sort field: `name`, `price`, `productId`
- `sortDir` (default: asc) — Sort direction: `asc`, `desc`
- `name` — Partial, case-insensitive name filter
- `categoryId` — Filter by category ID
- `minPrice` / `maxPrice` — Price range filter

### Categories (`/api/categories`)

| Method | Endpoint                | Description               |
|--------|-------------------------|---------------------------|
| GET    | `/api/categories`       | List categories (paginated) |
| GET    | `/api/categories/all`   | List all categories       |
| GET    | `/api/categories/{id}`  | Get category by ID        |
| POST   | `/api/categories`       | Create a new category     |
| PUT    | `/api/categories/{id}`  | Update a category         |
| DELETE | `/api/categories/{id}`  | Delete a category         |

### Orders (`/api/orders`)

| Method | Endpoint              | Description                 |
|--------|-----------------------|-----------------------------|
| GET    | `/api/orders`         | List all orders             |
| GET    | `/api/orders/user/{id}`| List orders for a user      |
| GET    | `/api/orders/{id}`    | Get order details           |
| POST   | `/api/orders`         | Create a new order (with stock check) |
| PUT    | `/api/orders/{id}/status`| Update order status (New Tx) |

---

## AOP Aspects

### LoggingAspect
- **Target**: All methods in `com.ecommerce.service.*`
- **Advice**: `@Before` (logs method entry + args), `@AfterReturning` (logs result type), `@AfterThrowing` (logs exceptions)
- **Log prefix**: `[AOP-LOG]`

### PerformanceAspect
- **Target**: All methods in `com.ecommerce.repository.*`
- **Advice**: `@Around` (measures execution time)
- **Threshold**: 500ms — logs `[PERF-WARNING]` for slow queries
- **Log prefix**: `[PERF-MONITOR]` or `[PERF-WARNING]`

---

## Validation

### Bean Validation (on DTOs)
- `@NotBlank`, `@NotNull` — Required fields
- `@Email` — Valid email format
- `@Size(min=6)` — Password minimum length
- `@DecimalMin("0.0")` — Non-negative price
- `@Min(0)` — Non-negative stock

### Custom Validator
- `@UniqueProductName` — Checks database for duplicate product names before creation

### Error Handling
- `GlobalExceptionHandler` via `@ControllerAdvice`
- Consistent error response format: `{ status, message, data }`

---

## Repository Architecture & Query Logic

### Persistence Layer
The system uses Spring Data JPA with PostgreSQL. Repositories are designed for both efficiency and developer productivity.

### Query Strategies
- **Optimized Loading**: We utilize `LEFT JOIN FETCH` in JPQL queries to solve the N+1 problem, specifically for loading Products with Categories and Orders with Items.
- **Dynamic Filtering**: Custom search queries in `ProductRepository` handle optional parameters (name, category, price range) gracefully using JPQL `IS NULL` checks.
- **Native SQL Reporting**: For database-intensive aggregations (e.g., monthly revenue), we use `@Query(nativeQuery = true)` to leverage PostgreSQL's performance directly.
- **Specifications**: Used where highly dynamic criteria are needed, ensuring the data access layer remains flexible.

---

## Transaction Management

### Configuration
The application uses declarative transaction management via `@Transactional` in the service layer.

### Key Policies
- **Atomic Order Creation**: `createOrder` uses `REQUIRED` propagation and `READ_COMMITTED` isolation. It performs a "check-then-act" sequence for stock validation. If any part of the process fails (e.g., insufficient stock), a `RuntimeException` is thrown, triggering a full rollback to maintain inventory integrity.
- **Independent Status Updates**: `updateOrderStatus` uses `REQUIRES_NEW` to ensure the status change is recorded regardless of the outcome of any surrounding business transaction.
- **Read-Only Optimization**: Query-heavy methods use `readOnly = true` to inform Hibernate that no dirty checking is required, reducing memory overhead and improving latency.

---

## Caching Implementation

### Configuration
Enabled via `@EnableCaching` and configured in `CacheConfig.java`.

### Cache Strategy
- **Manager**: `ConcurrentMapCacheManager`
- **Cache Names**:
    - `products`: Paginated product lists.
    - `product`: Individual product details (by ID).
    - `categories`: All categories list.
    - `users`: User-related data.

### Eviction Rules
- `createProduct` -> Evicts `products` cache.
- `updateProduct` / `deleteProduct` -> Evicts specific `product` entry and all `products` collection caches.
- `createOrder` -> Evicts `users` cache to refresh order history.
- `createCategory` / `updateCategory` / `deleteCategory` -> Evicts `categories` cache.

---

## Testing Guide

### Using Swagger UI
1. Start the application: `mvn spring-boot:run`
2. Open `http://localhost:8080/swagger-ui.html`
3. Explore and test all endpoints interactively

### Using GraphiQL
1. Start the application: `mvn spring-boot:run`
2. Open `http://localhost:8080/graphiql?path=/graphql`
3. Write and execute GraphQL queries in the interactive editor

### Using cURL

```bash
# Create a category
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"Electronics","description":"Electronic devices"}'

# Create a product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","description":"Gaming laptop","price":999.99,"categoryId":1,"stockQuantity":50}'

# List products with filters
curl "http://localhost:8080/api/products?name=laptop&sortBy=price&sortDir=desc"

# GraphQL query (POST only - GET returns 405)
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ product(id:1) { productId name price category { name } } }"}'

# Test GraphQL endpoint health
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ __typename }"}'
# Expected response: {"data":{"__typename":"Query"}}
```

### Test Cases

| # | Test Case                     | Method | Endpoint                          | Expected |
|---|-------------------------------|--------|-----------------------------------|----------|
| 1 | List all users                | GET    | `/api/users`                      | 200 OK   |
| 2 | Create user with valid data   | POST   | `/api/users`                      | 201 Created |
| 3 | Create user with invalid email| POST   | `/api/users`                      | 400 Bad Request |
| 4 | Get product by ID             | GET    | `/api/products/1`                 | 200 OK   |
| 5 | Filter products by name       | GET    | `/api/products?name=laptop`       | 200 OK   |
| 6 | Filter by price range         | GET    | `/api/products?minPrice=10&maxPrice=100` | 200 OK |
| 7 | Create duplicate product name | POST   | `/api/products`                   | 400 Bad Request |
| 8 | Delete non-existent product   | DELETE | `/api/products/999`               | 404 Not Found |
| 9 | GraphQL fetch product         | POST   | `/graphql`                        | 200 OK   |
| 10| GraphQL create product        | POST   | `/graphql`                        | 200 OK   |
| 11| GraphQL GET request           | GET    | `/graphql`                        | 405 Method Not Allowed |
| 12| Create order (Insufficient Stock)| POST   | `/api/orders`                     | 400 Bad Request / 500 Error |
| 13| Verify Transaction Rollback    | -      | -                                 | Stock remains unchanged |
| 14| Verify Cache Hit (Logs)        | GET    | `/api/products/1`                 | "Cache hit" in logs |

---

## Project Structure

```
src/main/java/com/ecommerce/
├── SmartECommerceApplication.java      # Spring Boot entry point
├── controller/
│   ├── UserRestController.java         # REST: User CRUD
│   ├── ProductRestController.java      # REST: Product CRUD + filters
│   └── CategoryRestController.java     # REST: Category CRUD
├── service/
│   ├── UserService.java                # User business logic
│   ├── SpringProductService.java       # Product business logic
│   └── CategoryService.java            # Category business logic
├── repository/
│   ├── UserRepository.java             # JPA repository
│   ├── ProductRepository.java          # JPA repository + specs
│   └── CategoryRepository.java         # JPA repository
├── dto/
│   ├── ApiResponse.java                # Standard response wrapper
│   ├── UserDTO.java                    # User validation DTO
│   ├── ProductDTO.java                 # Product validation DTO
│   └── CategoryDTO.java                # Category validation DTO
├── model/
│   ├── User.java                       # JPA entity
│   ├── Product.java                    # JPA entity
│   └── Category.java                   # JPA entity
├── validator/
│   ├── UniqueProductName.java          # Custom annotation
│   └── UniqueProductNameValidator.java # Custom validator logic
├── exception/
│   └── GlobalExceptionHandler.java     # @ControllerAdvice
├── aspect/
│   ├── LoggingAspect.java              # @Before/@After logging
│   └── PerformanceAspect.java          # @Around performance monitor
├── graphql/
│   ├── ProductResolver.java            # GraphQL product resolver
│   └── CategoryResolver.java           # GraphQL category resolver
├── config/
│   ├── GraphQLConfig.java              # GraphQL configuration
│   └── WebConfig.java                  # Web configuration (maps /graphiql)
└── resources/
    └── static/
        └── graphiql/
            └── index.html              # Local GraphiQL page

src/main/resources/
├── application.properties              # Main config (profile activation)
├── application-dev.yml                 # Dev profile
├── application-test.yml                # Test profile
├── application-prod.yml                # Prod profile
└── graphql/
    └── schema.graphqls                 # GraphQL schema
```