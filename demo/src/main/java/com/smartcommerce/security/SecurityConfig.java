package com.smartcommerce.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.smartcommerce.security.audit.AccessDeniedHandlerImpl;

/**
 * Epic 5 — DSA & Security Optimization: Security Configuration
 * ─────────────────────────────────────────────────────────────────────────────
 * DSA Principles Applied
 *
 * 1. BCrypt — Adaptive Hashing for Password Storage
 *    • BCryptPasswordEncoder wraps the bcrypt KDF which applies a
 *      configurable cost factor (default 10 rounds) to the input password.
 *    • The output is a deterministic 60-character string that includes the
 *      salt, cost factor, and digest — making rainbow-table attacks infeasible.
 *    • Verification is constant-time to prevent timing attacks.
 *    • Defined as a shared @Bean and injected into:
 *        – DaoAuthenticationProvider  (login credential check)
 *        – UserServiceImp.registration() (password encoding on sign-up)
 *
 * 2. JWT Validation — HMAC-SHA256 Hashing
 *    • Every inbound JWT is verified by re-computing HMAC-SHA256 over
 *      header.payload with the shared secret key.  If the digests differ,
 *      the token is rejected immediately — O(1) constant-time check.
 *    • See JwtTokenService for full validation pipeline details.
 *
 * 3. Token Blacklist — ConcurrentHashMap (O(1) Lookup)
 *    • Revoked (logged-out) tokens are stored in an in-memory
 *      ConcurrentHashMap<SHA256(token), expiryMillis> managed by
 *      TokenBlacklistService.
 *    • Each request goes through JWTAuthenticationFilter which calls
 *      JwtTokenService.validateToken() — this includes an O(1) blacklist
 *      lookup before the SecurityContext is populated.
 *    • A @Scheduled task evicts expired entries every 30 minutes,
 *      keeping memory bounded without impacting per-request latency.
 *
 * Security pipeline per request:
 *   Request → JWTAuthenticationFilter
 *               ├─ extract Bearer token
 *               ├─ JwtTokenService.validateToken()
 *               │     ├─ 1. HMAC-SHA256 signature check     O(1)
 *               │     ├─ 2. expiry check                    O(1)
 *               │     └─ 3. HashMap blacklist check          O(1)
 *               └─ SecurityContext.setAuthentication()
 *                     → proceed to controller
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final OAuthLoginSuccesshandler oAuthLoginSuccesshandler;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    public SecurityConfig(JWTAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          OAuthLoginSuccesshandler oAuthLoginSuccesshandler,
                          AccessDeniedHandlerImpl accessDeniedHandler) {
        this.jwtAuthenticationFilter  = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.oAuthLoginSuccesshandler = oAuthLoginSuccesshandler;
        this.accessDeniedHandler      = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                // IF_REQUIRED: JWT endpoints are stateless; OAuth2 code flow needs a brief session for the state param
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth

                        // ── Public: auth & registration ──────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/users", "/api/users/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // ── Authenticated: logout — requires a valid (non-revoked) Bearer token
                        //    TokenBlacklistService will O(1)-insert the token on this call
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()

                        .requestMatchers("/api/auth/**").permitAll()

                        // ── Public: browsing ─────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()

                        // ── Admin only: inventory ─────────────────────────────────────────
                        .requestMatchers("/api/inventory/**").hasAuthority("ROLE_ADMIN")

                        // ── Admin only: product write operations ──────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAuthority("ROLE_ADMIN")

                        // ── Admin only: category management ───────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAuthority("ROLE_ADMIN")

                        // ── Admin only: order reporting & all-orders views ─────────────────
                        .requestMatchers(HttpMethod.GET, "/api/orders", "/api/orders/paged").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/status/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/report/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasAuthority("ROLE_ADMIN")

                        // ── Swagger / Actuator ────────────────────────────────────────────
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()

                        // ── Everything else requires a valid JWT ──────────────────────────
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuthLoginSuccesshandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)   // 401
                        .accessDeniedHandler(accessDeniedHandler))                // 403
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * DaoAuthenticationProvider uses:
     *   1. UserDetailsService — loads user record from DB by username/email
     *   2. BCryptPasswordEncoder — verifies submitted password against stored hash
     *      using constant-time bcrypt comparison (prevents timing side-channels)
     */
    @Bean
    AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCryptPasswordEncoder — Secure Password Hashing
     *
     * DSA Principle: Cryptographic hash function with adaptive cost.
     *   • Internally runs the Blowfish cipher in a key-setup phase iterated
     *     2^strength (default strength=10, i.e. 1024) times.
     *   • Produces a 60-character encoded string: $2a$10$<22-char-salt><31-char-hash>
     *   • encode()  — O(1), cost ~100 ms at strength 10 (intentionally slow)
     *   • matches() — O(1), constant-time byte comparison after re-hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
