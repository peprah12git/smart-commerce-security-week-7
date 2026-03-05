# SmartCommerce — API Reference

Base URL: `http://localhost:8080`

> **Authentication:** All protected endpoints require a `Authorization: Bearer <token>` header.  
> Obtain a token via `POST /api/auth/login`.

---

## Table of Contents

- [Auth](#auth)
- [Users](#users)
- [Products](#products)
- [Categories](#categories)
- [Inventory](#inventory)
- [Orders](#orders)
- [Cart](#cart)
- [Reviews](#reviews)
- [GraphQL](#graphql)

---

## Auth

Base path: `/api/auth`

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| `POST` | `/api/auth/login` | Public | Authenticate with email & password. Returns a signed JWT. |
| `POST` | `/api/auth/logout` | Authenticated | Revokes the current JWT (adds it to the in-memory blacklist). |

### POST /api/auth/login
**Request body**
```json
{
  "email": "john.doe@example.com",
  "password": "securePass123"
}
```
**Response `200`**
```json
{
  "userId": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "role": "CUSTOMER",
  "token": "<jwt>",
  "message": "Login successful"
}
```
**Error responses:** `401 Unauthorized` · `429 Too Many Requests` (brute-force lock)

### POST /api/auth/logout
**Header:** `Authorization: Bearer <token>`  
**Response `200`:** `"Logout successful — token has been revoked"`  
**Error responses:** `400 Bad Request` (missing/malformed header)

---

## Users

Base path: `/api/users`

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| `POST` | `/api/users` | Public | Register a new customer account. |
| `GET` | `/api/users/me` | Authenticated | Get the current user's profile. |
| `PUT` | `/api/users/me` | Authenticated | Update the current user's profile. |
| `GET` | `/api/users` | ADMIN | List all users. |
| `GET` | `/api/users/{id}` | ADMIN | Get a user by ID. |
| `PUT` | `/api/users/{id}` | ADMIN | Update any user. |
| `DELETE` | `/api/users/{id}` | ADMIN | Permanently delete a user. |

### POST /api/users — Register
**Request body**
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "password": "securePass123",
  "phone": "+1 (555) 123-4567",
  "address": "123 Main St, Springfield, IL 62701"
}
```
**Response `201`** — returns `UserResponse` (no password field)

### PUT /api/users/me — Update profile
```json
{
  "name": "Jane Doe",
  "email": "jane.doe@example.com",
  "password": "newPass456",
  "phone": "+1 (555) 987-6543",
  "address": "456 Oak Ave, Chicago, IL 60601",
  "role": "CUSTOMER"
}
```
**Error responses:** `400` · `401` · `409` (email already in use)

### UserResponse shape
```json
{
  "userId": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "phone": "+1 (555) 123-4567",
  "address": "123 Main St",
  "role": "CUSTOMER",
  "createdAt": "2026-03-04T12:00:00.000Z"
}
```

---

## Products

Base path: `/api/products`

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| `GET` | `/api/products` | Public | List all products. |
| `GET` | `/api/products/paged` | Public | List products (paginated). |
| `GET` | `/api/products/{id}` | Public | Get a product by ID. |
| `GET` | `/api/products/category/{categoryName}/paged` | Public | Products filtered by category (paginated). |
| `GET` | `/api/products/search` | Public | Search products by keyword. |
| `GET` | `/api/products/search/paged` | Public | Search products (paginated). |
| `POST` | `/api/products` | ADMIN | Create a new product. |
| `PUT` | `/api/products/{id}` | ADMIN | Update a product. |
| `DELETE` | `/api/products/{id}` | ADMIN | Delete a product. |
| `DELETE` | `/api/products/cache` | ADMIN | Evict the product cache. |

### Query parameters (paged endpoints)
| Param | Default | Description |
|-------|---------|-------------|
| `page` | `0` | Page number (0-based) |
| `size` | `10` | Page size |
| `sort` | `name,asc` | Sort field and direction |

### Query parameters (search endpoints)
| Param | Required | Description |
|-------|----------|-------------|
| `q` | Yes | Search keyword |
| `minPrice` | No | Minimum price filter |
| `maxPrice` | No | Maximum price filter |

### POST /api/products — Create product
```json
{
  "name": "Wireless Headphones",
  "description": "Noise-cancelling over-ear headphones",
  "price": 99.99,
  "categoryId": 3,
  "initialStock": 50
}
```
**Response `201`** — returns `ProductResponse`

---

## Categories

Base path: `/api/categories`

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| `GET` | `/api/categories` | Public | List all categories. |
| `POST` | `/api/categories` | ADMIN | Create a category. |
| `PUT` | `/api/categories/{id}` | ADMIN | Update a category. |
| `DELETE` | `/api/categories/{id}` | ADMIN | Delete a category. |

### POST /api/categories — Create category
```json
{
  "name": "Electronics",
  "description": "Electronic devices and accessories"
}
```
**Response `201`** — returns `CategoryResponse`

---

## Inventory

Base path: `/api/inventory`  
**All inventory endpoints require `ADMIN` or `STAFF` role.**

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/inventory/paged` | All inventory items (paginated). |
| `GET` | `/api/inventory/{productId}` | Inventory record for a specific product. |
| `GET` | `/api/inventory/{productId}/availability` | Returns `true`/`false` — whether product is in stock. |
| `GET` | `/api/inventory/low-stock` | Products with stock below threshold (`?threshold=10`). |
| `GET` | `/api/inventory/low-stock/paged` | Low-stock products (paginated, `?threshold=10`). |
| `GET` | `/api/inventory/out-of-stock` | Products with zero stock. |
| `PUT` | `/api/inventory/{productId}` | Set absolute stock quantity. |
| `POST` | `/api/inventory/{productId}/stock-additions` | Increase stock by an amount. |
| `POST` | `/api/inventory/{productId}/stock-reductions` | Decrease stock by an amount. |

### PUT /api/inventory/{productId} — Set quantity
```json
{ "quantity": 100 }
```

### POST /api/inventory/{productId}/stock-additions — Add stock
```json
{ "quantity": 20 }
```

### InventoryResponse shape
```json
{
  "inventoryId": 1,
  "productId": 5,
  "productName": "Wireless Headphones",
  "quantityAvailable": 48,
  "lastUpdated": "2026-03-04T10:30:00.000Z"
}
```

---

## Orders

Base path: `/api/orders`

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| `POST` | `/api/orders` | CUSTOMER / STAFF | Create an order with explicit items. |
| `POST` | `/api/orders/checkout` | CUSTOMER / STAFF | Create an order from the cart (clears cart on success). |
| `GET` | `/api/orders` | ADMIN | List all orders. Optional `?status=` filter. |
| `GET` | `/api/orders/paged` | ADMIN | All orders (paginated). |
| `GET` | `/api/orders/reports` | ADMIN / STAFF | Orders within a date range. |
| `GET` | `/api/orders/{orderId}` | ADMIN / STAFF | Get a single order by ID. |
| `GET` | `/api/orders/{orderId}/items` | ADMIN / STAFF | Line items for an order. |
| `GET` | `/api/orders/me` | CUSTOMER / STAFF | My orders. Optional `?status=` filter. |
| `GET` | `/api/orders/me/paged` | CUSTOMER / STAFF | My orders (paginated). |
| `PATCH` | `/api/orders/{orderId}/status` | ADMIN / STAFF | Update order status. |
| `POST` | `/api/orders/{orderId}/cancellations` | ADMIN / CUSTOMER | Cancel an order. |
| `DELETE` | `/api/orders/{orderId}` | ADMIN | Hard-delete an order permanently. |

### POST /api/orders — Create order
```json
{
  "items": [
    { "productId": 5, "quantity": 2 },
    { "productId": 12, "quantity": 1 }
  ]
}
```
**Response `201`** — returns `OrderResponse`

### PATCH /api/orders/{orderId}/status — Update status
```json
{ "status": "SHIPPED" }
```
Valid statuses: `PENDING` · `CONFIRMED` · `PROCESSING` · `SHIPPED` · `DELIVERED` · `CANCELLED`

### GET /api/orders/reports — Date-range report
| Param | Required | Format | Example |
|-------|----------|--------|---------|
| `startDate` | Yes | `yyyy-MM-ddTHH:mm:ss` | `2026-01-01T00:00:00` |
| `endDate` | Yes | `yyyy-MM-ddTHH:mm:ss` | `2026-03-31T23:59:59` |

### OrderResponse shape
```json
{
  "orderId": 10,
  "userId": 1,
  "status": "PENDING",
  "totalAmount": 249.97,
  "orderDate": "2026-03-04T14:22:00.000Z",
  "items": [
    { "productId": 5, "productName": "Wireless Headphones", "quantity": 2, "unitPrice": 99.99 }
  ]
}
```

---

## Cart

Base path: `/api/carts`  
**All cart endpoints require authentication.**

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/carts/me` | Get the current user's cart. |
| `GET` | `/api/carts/me/items` | List all items in the cart. |
| `GET` | `/api/carts/me/items/{productId}` | Get a specific cart item by product. |
| `GET` | `/api/carts/me/count` | Number of items in the cart. |
| `GET` | `/api/carts/me/total` | Total price of the cart. |
| `POST` | `/api/carts/items` | Add a product to the cart. |
| `PUT` | `/api/carts/me/items/{productId}` | Update the quantity of a cart item. |
| `DELETE` | `/api/carts/me/items/{productId}` | Remove a specific item from the cart. |
| `DELETE` | `/api/carts/me` | Clear the entire cart. |

### POST /api/carts/items — Add to cart
```json
{
  "productId": 5,
  "quantity": 2
}
```

### PUT /api/carts/me/items/{productId} — Update quantity
```json
{ "quantity": 3 }
```

---

## Reviews

Base path: `/api/reviews`

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| `GET` | `/api/reviews` | Public | List all reviews. |
| `GET` | `/api/reviews/{id}` | Public | Get a review by ID. |
| `GET` | `/api/reviews/product/{productId}` | Public | All reviews for a product. |
| `GET` | `/api/reviews/me` | Authenticated | My reviews. |
| `GET` | `/api/reviews/user/{userId}` | ADMIN | Reviews written by a specific user. |
| `POST` | `/api/reviews` | Authenticated | Create a review. |
| `PUT` | `/api/reviews/{id}` | Authenticated | Update own review. |
| `DELETE` | `/api/reviews/{id}` | Authenticated | Delete own review. |

### POST /api/reviews — Create review
```json
{
  "productId": 5,
  "rating": 5,
  "comment": "Excellent sound quality!"
}
```
**Response `201`** — returns `Review`

---

## GraphQL

Endpoint: `POST /graphql`  
Playground (dev): `http://localhost:8080/graphiql`

### Queries

| Operation | Arguments | Description |
|-----------|-----------|-------------|
| `product(id)` | `id: Int!` | Fetch a single product by ID. |
| `products(...)` | filter, sort args | Fetch the full product list. |
| `productsPaged(...)` | `page`, `size`, sort args | Paginated product list. |
| `searchProducts(searchTerm)` | `searchTerm: String!` | Search products by keyword. |
| `category(id)` | `id: Int!` | Fetch a single category by ID. |
| `categories()` | — | Fetch all categories. |

### Mutations

| Operation | Arguments | Description |
|-----------|-----------|-------------|
| `createProduct(input)` | product fields | Create a new product. |
| `updateProduct(id, input)` | `id` + product fields | Update an existing product. |

### Example — fetch a product
```graphql
query {
  product(id: 5) {
    productName
    categoryName
    inventory {
      quantityAvailable
    }
  }
}
```

### Example — paginated products
```graphql
query {
  productsPaged(page: 0, size: 10) {
    content {
      productName
      categoryName
    }
    totalElements
    totalPages
  }
}
```

---

## HTTP Status Code Reference

| Code | Meaning |
|------|---------|
| `200` | OK |
| `201` | Created |
| `204` | No Content (successful delete) |
| `400` | Bad Request / Validation error |
| `401` | Unauthorized — missing or invalid JWT |
| `403` | Forbidden — insufficient role |
| `404` | Resource not found |
| `409` | Conflict — duplicate resource (e.g. email) |
| `429` | Too Many Requests — brute-force lock active |

---

## Swagger UI

Interactive API documentation is available at:  
`http://localhost:8080/swagger-ui/index.html`

OpenAPI JSON spec:  
`http://localhost:8080/v3/api-docs`
