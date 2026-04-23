# Smart E-Commerce System Documentation

## 1. Repository Usage and Query Logic
- **Spring Data JPA**: All repositories extend `JpaRepository` for standard CRUD.
- **Derived Queries**: Used for simple filters like `findByName`, `findByPriceBetween`.
- **JPQL with JOIN FETCH**: Used in `ProductRepository` and `OrderRepository` to solve the N+1 problem by loading relationships in a single query.
- **Native Queries**: Used in `ProductRepository` for complex reports like `countProductsPerCategory` and `findTop5MostExpensiveProducts`.
- **Specifications**: Used for dynamic filtering in `ProductServiceImpl` (now moved to `searchProducts` for optimization).

## 2. Transaction Handling and Rollback Strategies
- **Declarative Transactions**: Managed via `@Transactional` in the Service layer.
- **Create Order Transaction**: Uses `propagation = REQUIRED` and `isolation = READ_COMMITTED`. It validates stock first and then saves the order items. If stock is insufficient, it throws a `RuntimeException`, triggering a full rollback of the transaction.
- **Update Status Transaction**: Uses `propagation = REQUIRES_NEW` to ensure status changes are committed even if the parent transaction fails (used for logging or status audit).
- **Read-Only Transactions**: All query-only methods use `readOnly = true` to optimize performance and prevent unnecessary dirty checking.

## 3. Caching Strategies
- **Spring Caching**: Integrated using `@Cacheable` and `@CacheEvict`.
- **Cache Names**: `products`, `product`, `categories`, `users`.
- **Eviction Policy**: 
  - Creating/Updating/Deleting a product evicts the `products` list cache.
  - Updating/Deleting a specific product evicts its specific cache entry.
  - Creating an order evicts the `users` cache to refresh user history.

