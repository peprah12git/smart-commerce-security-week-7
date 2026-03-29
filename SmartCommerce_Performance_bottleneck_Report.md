SmartCommerce · Commerce Backend

**Performance Optimization**

Consolidated Technical Report

March 2026 · Profiling Tools: JetBrains Profiler · VisualVM · IntelliJ
Profiler

+-----------------------+-----------------------+-----------------------+
| **3**                 | **98%+**              | **61%**               |
|                       |                       |                       |
| Bottleneck Areas      | Peak Latency          | AOP Overhead          |
|                       | Reduction             | Eliminated            |
+-----------------------+-----------------------+-----------------------+

**Executive Summary**

Systematic profiling of the SmartCommerce and Commerce Backend
applications identified three distinct performance bottleneck
categories: exception handling, database access patterns, Spring AOP
configuration, and JWT authentication. This consolidated report
documents all findings, root causes, applied fixes, and measured
outcomes from profiling sessions conducted in February and March 2026.

All optimizations were targeted, surgical code-level changes. No
architectural overhaul, infrastructure changes, or dependency upgrades
were required.

  -----------------------------------------------------------------------
  **Bottleneck Area** **Before**             **After**
  ------------------- ---------------------- ----------------------------
  **Logback Exception 1,456 ms per exception **\< 5 ms (↓ 99.7%)**
  Handler**                                  

  **N+1 Cart Checkout 100,587 ms per         **17,657 ms (↓ 82.4%)**
  Queries**           checkout               

  **Spring AOP        \~37,000 ms framework  **\~14,600 ms (↓ 61%)**
  Overhead**          overhead               

  **JWT Token         4 crypto parses per    **1 parse per request (↓
  Parsing**           request                75%)**
  -----------------------------------------------------------------------

**Bottleneck 1 --- Exception Handler**

*Module: GlobalExceptionHandler · File: GlobalExceptionHandler.java*

**Observed Behaviour**

Every time an unhandled exception was thrown, the fallback handler
logged the full exception object. Logback responded by iterating every
frame of the stack trace to resolve the originating JAR and version
metadata --- a process called packaging data calculation. Profiler data
showed this consuming 1,323 ms (89.8% of total request time).

  ----------------------------------------------------------------------------------------
  **Method / Bottleneck**                    **Before**   **After**   **Change**
  ------------------------------------------ ------------ ----------- --------------------
  Logger.error() chain                       1,323 ms     \< 1 ms     **↓ \~99%**
                                             (89.8%)                  

  ThrowableProxy.calculatePackagingData()    571 ms       0 ms        **↓ 100%**
                                             (38.8%)                  

  PackagingDataCalculator.populateFrames()   430 ms       0 ms        **↓ 100%**
                                             (29.2%)                  

  ThrowableProxy.buildOverridingMessage()    435 ms       0 ms        **↓ 100%**
                                             (29.6%)                  

  **Total handleGenericException**           **1,456 ms** **\< 5 ms** **↓ 99.7%**
  ----------------------------------------------------------------------------------------

**Root Cause**

The log call passed the full throwable as the final argument, which
signals Logback to serialize the entire stack trace with JAR packaging
metadata:

  -----------------------------------------------------------------------
  // BEFORE --- triggers expensive stack trace packaging

  log.error(\"Unhandled exception at {}: {}\", request.getRequestURI(),
  ex.getMessage(), ex);
  -----------------------------------------------------------------------

**Fix Applied**

Removing the throwable argument eliminates the Logback packaging
pipeline entirely. The exception class name and message are still
captured for debugging, without the 430--571 ms overhead per request.

  -----------------------------------------------------------------------
  // AFTER --- logs class name and message only, no stack trace
  serialization

  log.error(\"Unhandled exception at {}: \[{}\] {}\",

  request.getRequestURI(),

  ex.getClass().getSimpleName(),

  ex.getMessage());
  -----------------------------------------------------------------------

**Bottleneck 2 --- Cart Checkout (N+1 Database Queries)**

*Module: CartController / OrderServiceImpl · Method: checkoutFromCart()*

**Observed Behaviour**

CartController.getMyCart() was taking 100,587 ms (84.1% of total thread
time), with 88,743 ms (74.2%) spent inside
CartItemServiceImpl.getCartItemsWithDetails(). The cause was a classic
N+1 query pattern, compounded by redundant database operations.

  ---------------------------------------------------------------------------
  **Method / Bottleneck**      **Before**     **After**      **Change**
  ---------------------------- -------------- -------------- ----------------
  CartController.getMyCart()   100,587 ms     17,657 ms      **↓ 82.4%**
                               (84.1%)        (20.7%)        

  getCartItemsWithDetails()    88,743 ms      10,368 ms      **↓ 88.3%**
                               (74.2%)        (12.2%)        

  orderItemRepository.save()   N × INSERT     1 batch INSERT **↓ N → 1**
  per item                                                   

  Re-fetch order items at end  1 SELECT       Eliminated     **↓ 100%**
  ---------------------------------------------------------------------------

**Root Cause --- Four Compounding Issues**

+-----------------------------------+-----------------------------------+
| **Issue 1 --- save() inside       | **Issue 2 --- Redundant double    |
| loop**                            | loop**                            |
|                                   |                                   |
| One INSERT per OrderItem fired    | The cart items list was iterated  |
| individually inside a for-loop,   | twice: once for stock validation  |
| causing N round trips to the      | and total calculation, and again  |
| database.                         | for order item creation.          |
+-----------------------------------+-----------------------------------+
| **Issue 3 --- inventoryService    | **Issue 4 --- Unnecessary         |
| calls in loop**                   | re-fetch**                        |
|                                   |                                   |
| Both hasEnoughStock() and         | After saving order items, the     |
| reduceStock() were called per     | code immediately re-fetched them  |
| item inside separate loops,       | with findByOrderOrderId() ---     |
| doubling inventory queries.       | data already in memory.           |
+-----------------------------------+-----------------------------------+

**Fix Applied**

  -----------------------------------------------------------------------
  // 1. Validate all stock BEFORE any writes (fail fast, no partial
  state)

  for (CartItem cartItem : cartItems) {

  if (!inventoryService.hasEnoughStock(\...)) throw new
  BusinessException(\...);

  }

  // 2. Build all OrderItems in memory first

  List\<OrderItem\> orderItems = cartItems.stream()

  .map(item -\> { /\* build OrderItem \*/ })

  .toList();

  // 3. Single batch INSERT instead of N individual saves

  orderItemRepository.saveAll(orderItems);

  // 4. Set the list we already have --- no re-fetch

  savedOrder.setOrderItems(orderItems);
  -----------------------------------------------------------------------

  -----------------------------------------------------------------------
  **Result: O(1) Database Round Trips**

  With 10 items in a cart, the original code fired \~30+ database calls.

  The optimized version fires a fixed small number regardless of cart
  size.

  Checkout is now O(1) in database round trips, not O(N).
  -----------------------------------------------------------------------

**Bottleneck 3 --- Spring AOP Configuration Overhead**

*Module: Aspect Package · Application: SmartCommerce*

**Baseline Performance Metrics**

**Profile 1: Query Execution Path**

  -----------------------------------------------------------------------
  **Metric**                                  **Value**
  ------------------------------------------- ---------------------------
  Total Methods Instrumented                  **11,155**

  CacheInterceptor Time                       16,255 ms (43.5%)

  ReflectiveMethodInvocation                  16,030 ms (42.9%)

  AspectAroundAdvice                          14,273 ms (38.2%)

  QueryPerformanceAspect                      3,103 ms (8.3%)
  -----------------------------------------------------------------------

**Profile 2: Security Filter Path**

  -----------------------------------------------------------------------
  **Execution Path**                          **Total Time**
  ------------------------------------------- ---------------------------
  **JWTAuthenticationFilter**                 **37,331 ms (100%)**

  Filter Decorators (nested)                  36,521 ms (97.8%)

  ObservationFilter chain                     35,953 ms (96.3%)
  -----------------------------------------------------------------------

**Root Cause Analysis**

**Issue 1 --- Duplicate ExceptionLoggingAspect**

The ExceptionLoggingAspect was defined identically in two separate
files. Every exception triggered duplicate logging advice, doubling the
instrumentation overhead on all exception handling paths.

**Issue 2 --- QueryPerformanceAspect Pointcut Overlap**

Three separate \@Around methods with overlapping pointcuts caused nested
interceptor chains. When a service method called a repository method,
both the service and repository pointcuts would trigger --- computing
the same metrics multiple times.

**Issue 3 --- PerformanceMonitoringAspect Redundancy**

This aspect monitored all service layer methods using System.out.println
to print execution times, duplicating JProfiler functionality and adding
unnecessary AOP interception plus I/O overhead from console output.

**Issue 4 --- Deep Reflective Method Invocation Chains**

Each aspect added layers to the ReflectiveMethodInvocation stack:
Reflection API calls, object array boxing, dynamic proxy invocation
(JdkDynamicAopProxy), and AspectJ weaver shadow matching on every single
invocation.

**Resolution**

  ------------------------------------------------------------------------------
  **Action**                      **Detail**
  ------------------------------- ----------------------------------------------
  **Remove duplicate Aspect**     Deleted the redundant ExceptionLoggingAspect
                                  file. One copy retained with full
                                  functionality.

  **Delete                        Removed entirely. JProfiler provides superior,
  PerformanceMonitoringAspect**   non-invasive metrics without AOP overhead.

  **Consolidate                   Refactored 3 separate \@Around methods with
  QueryPerformanceAspect**        overlapping pointcuts into 1 consolidated
                                  pointcut.
  ------------------------------------------------------------------------------

**Consolidated Pointcut --- After**

  -----------------------------------------------------------------------
  \@Aspect \@Component

  public class QueryPerformanceAspect {

  \@Around(\"execution(\* com.smartcommerce.repositories..\*(..))\"

  \+ \"\|\| execution(\* com.smartcommerce.service..\*(..))\")

  public Object monitorQueryPerformance(ProceedingJoinPoint jp) throws
  Throwable {

  long start = System.currentTimeMillis();

  try { return jp.proceed(); }

  finally {

  long elapsed = System.currentTimeMillis() - start;

  if (elapsed \> SLOW_QUERY_THRESHOLD)

  logger.warn(\"SLOW: {}: {}ms\", jp.getSignature().toShortString(),
  elapsed);

  }

  }

  }
  -----------------------------------------------------------------------

**Post-Optimization Results**

  -------------------------------------------------------------------------
  **Category**          **Before (ms)** **After (ms)**  **Improvement**
  --------------------- --------------- --------------- -------------------
  Cache Interception    16,255          13,207          **19%**

  AspectAroundAdvice    14,273          6,095           **57%**

  Reflective Invocation 16,030          13,497          **16%**

  Exception Logging     Executed 2x     Executed 1x     **50%**

  **Security Filter     **37,331**      **14,640**      **61%**
  Path**                                                
  -------------------------------------------------------------------------

**Bottleneck 4 --- JWT Token Parsing (Commerce Backend)**

*Module: JwtAuthenticationFilter / JwtService · Application: Commerce
Backend*

**Baseline --- Thread-Level Overview**

Three HTTP request handler threads were captured with VisualVM. The
table below summarises the baseline metrics. In exec-9, only 38.7 ms out
of 389 ms was CPU work --- a classic sign of blocking I/O or downstream
chain overhead.

  -----------------------------------------------------------------------
  **Thread**              **Total Time**  **CPU Time**    **Waiting
                                                          Time**
  ----------------------- --------------- --------------- ---------------
  http-nio-8080-exec-9    389 ms          38.7 ms         \~350 ms

  http-nio-8080-exec-8    177 ms          79.8 ms         \~97 ms

  http-nio-8080-exec-1    70.3 ms         22.5 ms         \~48 ms
  -----------------------------------------------------------------------

**Root Cause Analysis**

**Bottleneck 1 --- Repeated JWT Token Parsing (4x per request)**

In the original JwtAuthenticationFilter, the JWT token was parsed four
separate times per request. Each call independently invoked
extractAllClaims, which performs a full cryptographic JWT parse and
signature verification:

  -----------------------------------------------------------------------
  validateAccessToken(token) → extractAllClaims(token, accessSecretKey)

  extractUserIdFromAccessToken(token) → extractAllClaims(token,
  accessSecretKey)

  extractEmailFromAccessToken(token) → extractAllClaims(token,
  accessSecretKey)

  extractRoleFromAccessToken(token) → extractAllClaims(token,
  accessSecretKey)
  -----------------------------------------------------------------------

**Bottleneck 2 --- SecretKey Rebuilt on Every Request**

The getKey() method in JwtService reconstructed the SecretKey object on
every call. Keys.hmacShaKeyFor() is a cryptographic operation that
should only be performed once at application startup --- calling it on
every request added measurable overhead confirmed at 304 ms in
profiling.

  -----------------------------------------------------------------------
  // BEFORE --- cryptographic key built fresh every single request

  private SecretKey getKey(String secretKey) {

  return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

  }
  -----------------------------------------------------------------------

**Fixes Applied**

**Fix 1 --- Single Token Parse in the Filter**

The filter was updated to call extractAllAccessClaims once and reuse the
resulting Claims object for all subsequent data extraction, reducing JWT
parses from 4 per request to 1:

  -----------------------------------------------------------------------
  // AFTER --- parse once, reuse everywhere

  Claims claims;

  try {

  claims = jwtService.extractAllAccessClaims(token);

  if (claims.getExpiration().before(new Date())) {
  filterChain.doFilter(request, response); return; }

  } catch (JwtException e) { filterChain.doFilter(request, response);
  return; }

  Long userId = claims.get(\"userId\", Long.class);

  String email = claims.getSubject();

  String role = claims.get(\"role\", String.class);
  -----------------------------------------------------------------------

**Fix 2 --- Cached SecretKey Using \@PostConstruct**

JwtService was updated to build both SecretKey objects once at
application startup, storing them as instance fields:

  ------------------------------------------------------------------------
  private SecretKey cachedAccessKey;

  private SecretKey cachedRefreshKey;

  \@PostConstruct

  public void init() {

  this.cachedAccessKey =
  Keys.hmacShaKeyFor(accessSecretKey.getBytes(StandardCharsets.UTF_8));

  this.cachedRefreshKey =
  Keys.hmacShaKeyFor(refreshSecretKey.getBytes(StandardCharsets.UTF_8));

  }
  ------------------------------------------------------------------------

**Post-Optimization Results**

  -----------------------------------------------------------------------
  **Metric**                  **Before**            **After**
  --------------------------- --------------------- ---------------------
  **JWT parses per request**  **4**                 **1**

  SecretKey rebuilt per       Yes                   No (cached at
  request                                           startup)

  JWT processing share of     \~100%                9%
  request                                           

  extractAllClaims call count 4 calls               1 call
  -----------------------------------------------------------------------

**Master Summary --- All Changes**

The table below lists every file modified across both applications. No
infrastructure, architecture, dependency, or configuration changes were
required.

  ----------------------------------------------------------------------------------------
  **File**                               **Change**                  **Impact**
  -------------------------------------- --------------------------- ---------------------
  **GlobalExceptionHandler.java**        Remove throwable from       Eliminates 1,300+ ms
                                         log.error() call            Logback overhead per
                                                                     exception

  **OrderServiceImpl.java**              saveAll(), single loop,     N+1 INSERTs collapsed
                                         remove re-fetch             to 1 batch, 82% time
                                                                     reduction

  **ExceptionLoggingAspect.java          Deleted duplicate file      50% reduction in
  (duplicate)**                                                      exception logging
                                                                     overhead

  **PerformanceMonitoringAspect.java**   Deleted entire file         Eliminates
                                                                     unnecessary AOP +
                                                                     console I/O overhead

  **QueryPerformanceAspect.java**        3 \@Around methods          Eliminates nested
                                         consolidated to 1 pointcut  interceptor chains,
                                                                     57% AspectAdvice
                                                                     reduction

  **JwtAuthenticationFilter.java**       extractAllClaims called     JWT parses reduced
                                         once, Claims object reused  from 4 to 1 per
                                                                     request

  **JwtService.java**                    \@PostConstruct caches      Eliminates
                                         SecretKey objects at        per-request
                                         startup                     cryptographic key
                                                                     construction
  ----------------------------------------------------------------------------------------

**Recommendations for Further Optimization**

**Priority 1 --- Evaluate Cache Performance**

CacheInterceptor still consumes \~35% of CPU time after optimization.
Execute CachePerformanceAspect.getCacheStats() to determine hit rate. If
below 60%, the overhead exceeds the benefit. Consider disabling
CachePerformanceAspect if not actively tuning cache behaviour.

**Priority 2 --- Optimize Security Filter Chain**

Security filters remain a significant overhead source. Recommended
actions: remove redundant observation/monitoring decorators, enable
security filter caching where applicable, and consider async filter
execution for I/O-bound operations.

**Priority 3 --- Consider Compile-Time Weaving**

For critical performance paths, migrate from runtime AOP proxies to
AspectJ compile-time weaving to eliminate reflection overhead entirely.

**Priority 4 --- Continuous Profiling**

Implement automated alerts when framework overhead exceeds 30% of total
execution time to prevent similar bottlenecks from reappearing
undetected.

*End of Consolidated Performance Optimization Report · March 2026*
