# SmartCommerce — Review API Performance Optimization Report

**Date:** 2026-03-31  
**Module:** `com.smartcommerce.controller.restControllers.ReviewController`  
**Profiler:** JetBrains CPU Profiler  
**Author:** Engineering Review

---

## 1. Executive Summary

A CPU profiling session on the `ReviewController` identified a severe performance bottleneck in the `getReviewsByProductId` endpoint. The endpoint was taking **22,508ms** (22 seconds) to serve a single request, with only **56.5ms** of actual CPU time — meaning **99.7% of the time was spent waiting**, not computing.

Through a systematic investigation of the controller, service, repository, model, and AOP layers, four root causes were identified across three profiling iterations. After all fixes, response time is expected to drop to **under 100ms** — a reduction of over 99%.

---

## 2. Profiling Results — Before & After

| Metric | Baseline | After Fix 1 (DTO) | After Fix 2 (LAZY) | Expected Final |
|---|---|---|---|---|
| Total Time | 22,508 ms | 3,796 ms | 2,824 ms | <100 ms |
| CPU Time | 56.5 ms | 53.6 ms | 39.1 ms | ~35 ms |
| % of Total App Time | 59.2% | 33.1% | 24.7% | <5% |
| Improvement vs Baseline | — | -83% | -87.5% | ~99%+ |

---

## 3. Root Causes Identified

### 3.1  Serialization-Triggered N+1 (Fixed)

**Location:** `ReviewController.getReviewsByProductId`

The controller was returning the raw `Review` JPA entity directly to Jackson for serialization:


```java
// BEFORE
public ResponseEntity<List<Review>> getReviewsByProductId(@PathVariable int productId) {
    List<Review> reviews = reviewService.getReviewsByProductId(productId);
    return ResponseEntity.ok(reviews);
}
```

Because `Review` holds references to `User` and `Product` (which hold further collections), Jackson traversed the full object graph during serialization. Any association not covered by the `JOIN FETCH` in the repository was still marked `LAZY` — so as Jackson accessed each field, Hibernate fired a **new database query per field access**, outside the original transaction.

This is a serialization-triggered N+1: harder to detect than a classic N+1 because it happens after the repository call returns, not inside it.

**Fix:** Map the result to `ReviewResponse` DTO before returning:

```java
// AFTER
public ResponseEntity<List<ReviewResponse>> getReviewsByProductId(@PathVariable int productId) {
    return ResponseEntity.ok(
        reviewService.getReviewsByProductId(productId)
                     .stream()
                     .map(ReviewResponse::from)
                     .toList()
    );
}
```

**Result:** Total time dropped from **22,508ms → 3,796ms** (83% reduction).

---

### 3.2  EAGER Loading on `@OneToMany` Collections (Fixed)

**Location:** `Product.java`, `User.java`

JPA's default fetch type for `@OneToMany` is `EAGER`. All collection mappings in `Product` and `User` were missing an explicit `fetch` attribute, causing Hibernate to automatically load every related collection whenever a `Product` or `User` was fetched — regardless of whether that data was needed.

**Affected fields:**

`Product.java`:
```java
// BEFORE — defaults to EAGER
@OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
private List<OrderItem> orderItems;

@OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
private List<Review> reviews;
```

`User.java`:
```java
// BEFORE — defaults to EAGER
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
private List<Order> orders;

@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
private List<CartItem> cartItems;

@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
private List<Review> reviews;
```

**Fix:** Add `fetch = FetchType.LAZY` explicitly to all `@OneToMany` annotations:

`Product.java`:
```java
// AFTER
@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<OrderItem> orderItems;

@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<Review> reviews;
```

`User.java`:
```java
// AFTER
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<Order> orders;

@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<CartItem> cartItems;

@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<Review> reviews;
```

**Result:** Total time dropped from **3,796ms → 2,824ms**. The remaining ~2,800ms was then traced to a separate AOP issue (see 3.3 below).

---

### 3.3  `QueryPerformanceAspect` — Self-Measuring Double Interception (Pending)

**Location:** `com.smartcommerce.aspect.QueryPerformanceAspect`

After the LAZY fix, the third profiling run revealed a new top offender:

```
com.smartcommerce.aspect.QueryPerformanceAspect    2,647 ms (23.2%)
```

The aspect's pointcut intercepted **both** the service and repository layers simultaneously:

```java
// BEFORE — double interception
@Around("execution(* com.smartcommerce.repositories..*(..))" +
        " || execution(* com.smartcommerce.service..*(..))")
```

Because `ReviewServiceImp.getReviewsByProductId` calls `reviewRepository.findBy...`, the aspect wraps both the outer service call and the inner repository call. The service-level timer therefore includes the repository-level interception overhead — the **aspect was measuring its own cost** and compounding it on every invocation.

Additionally, the slow query threshold of `100ms` was firing on nearly every call in a profiling environment, causing `logger.warn` and `joinPoint.getSignature().toShortString()` to execute on every single request — string allocations that add up at scale.

**Fix:**

```java
// AFTER — repository layer only, raised threshold, lazy string allocation
@Around("execution(* com.smartcommerce.repositories..*(..))")
public Object monitorQueryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
    long startTime = System.currentTimeMillis();

    try {
        return joinPoint.proceed();
    } finally {
        long executionTime = System.currentTimeMillis() - startTime;

        if (executionTime > SLOW_QUERY_THRESHOLD) {
            // String allocated only when actually logging
            logger.warn("SLOW QUERY - {}: {}ms",
                    joinPoint.getSignature().toShortString(),
                    executionTime);
        }
    }
}

private static final long SLOW_QUERY_THRESHOLD = 500; // raised from 100ms
```

| Change | Reason |
|---|---|
| Removed `service` from pointcut | Eliminates double-counting and self-measuring overhead |
| Raised threshold `100ms → 500ms` | Prevents log flooding on every normal request |
| Moved `getSignature()` inside `if` block | String allocation only on genuinely slow queries |

**Expected Result:** Aspect overhead drops from ~2,600ms to effectively 0ms on normal requests.

---


## 4. Files Changed

| File | Change Type | Description |
|---|---|---|
| `ReviewController.java` | Modified | Return `ReviewResponse` DTO instead of raw `Review` entity on all read endpoints |
| `ReviewResponse.java` | Modified | Added static `from(Review r)` mapping method |
| `Product.java` | Modified | Added `fetch = FetchType.LAZY` to `orderItems` and `reviews` |
| `User.java` | Modified | Added `fetch = FetchType.LAZY` to `orders`, `cartItems`, and `reviews` |
| `QueryPerformanceAspect.java` | Modified | Restricted pointcut to repository layer only; raised threshold to 500ms; lazy string allocation |

---

## 5. Recommendations

| Priority | Action | Status | Impact |
|---|---|---|---|
| 🔴 | Use `ReviewResponse` DTO in all read endpoints |  Done | -83% response time |
| 🔴 | Add `FetchType.LAZY` to all `@OneToMany` |  Done | -87.5% vs baseline |
| 🔴 | Fix `QueryPerformanceAspect` pointcut and threshold |  In Progress | Eliminates remaining ~2,600ms |
| 🟡 | Restrict `getAllReviews` to `ADMIN`, add pagination |  Pending | Security + stability |
| 🟡 | Fix detached entity in `createReview` |  Pending | Correctness |
| 🟡 | Remove duplicate `getReviewsByUserId` endpoint |  Pending | Security |


---

## 6. Key Takeaway

The core lesson from this investigation is that **returning JPA entities directly from REST controllers is always unsafe** in applications with bidirectional or nested relationships. Even with `FetchType.LAZY` on the entity itself, Jackson's serializer will walk every accessible getter and trigger Hibernate to load associations on demand — outside any transaction boundary.

The pattern to follow consistently across all controllers is:

```
Repository → Entity → DTO → ResponseEntity<DTO>
```

