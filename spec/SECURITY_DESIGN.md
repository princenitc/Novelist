# Security Design Specification

## Table of Contents
- [Overview](#overview)
- [Authentication Strategy](#authentication-strategy)
- [Authorization Model](#authorization-model)
- [Endpoint Security Matrix](#endpoint-security-matrix)
- [Message Queue Security](#message-queue-security)
- [API Key Management](#api-key-management)
- [CORS Configuration](#cors-configuration)
- [Security Headers](#security-headers)
- [Data Protection](#data-protection)
- [Security Best Practices](#security-best-practices)

## Overview

This document defines the security architecture for the Novelist application, covering authentication, authorization, data protection, and secure communication between services.

### Security Principles

1. **Defense in Depth**: Multiple layers of security
2. **Least Privilege**: Minimum necessary permissions
3. **Secure by Default**: Security enabled out of the box
4. **Zero Trust**: Verify every request
5. **Audit Everything**: Comprehensive logging

### Threat Model

**Assets to Protect**:
- User credentials and personal data
- Book content and metadata
- Rating and review data
- API keys and secrets
- Service-to-service communication

**Potential Threats**:
- Unauthorized access to user data
- API abuse and DDoS attacks
- SQL/Cypher injection
- Cross-site scripting (XSS)
- Man-in-the-middle attacks
- Data breaches

## Authentication Strategy

### Option 1: JWT Authentication (Recommended)

**Implementation**: Stateless JWT tokens with Spring Security

#### Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

#### JWT Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-123",
    "name": "John Doe",
    "email": "john.doe@example.com",
    "roles": ["USER", "ADMIN"],
    "iat": 1718712000,
    "exp": 1718798400
  },
  "signature": "..."
}
```

#### JWT Service Implementation

```java
package com.prince.novelist.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration:86400000}") // 24 hours
    private Long expiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
```

#### JWT Authentication Filter

```java
package com.prince.novelist.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        jwt = authHeader.substring(7);
        username = jwtService.extractUsername(jwt);
        
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (jwtService.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

#### Authentication Endpoints

```java
package com.prince.novelist.resource;

import com.prince.novelist.dto.request.LoginRequest;
import com.prince.novelist.dto.request.RegisterRequest;
import com.prince.novelist.dto.response.AuthResponse;
import com.prince.novelist.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthResource {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtService jwtService;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateToken(userDetails);
        
        return ResponseEntity.ok(AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .expiresIn(86400L) // 24 hours
            .build());
    }
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // User registration logic
        // ...
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateToken(userDetails);
        
        return ResponseEntity.ok(AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .expiresIn(86400L)
            .build());
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        String username = jwtService.extractUsername(jwt);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String newToken = jwtService.generateToken(userDetails);
        
        return ResponseEntity.ok(AuthResponse.builder()
            .token(newToken)
            .type("Bearer")
            .expiresIn(86400L)
            .build());
    }
}
```

---

### Option 2: OAuth2/OIDC Integration

**Implementation**: Integration with external identity providers (Google, GitHub, Keycloak)

#### Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

#### Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://accounts.google.com
          jwk-set-uri: https://www.googleapis.com/oauth2/v3/certs
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid,profile,email
        provider:
          google:
            issuer-uri: https://accounts.google.com
```

---

## Authorization Model

### Role-Based Access Control (RBAC)

#### Roles

```java
public enum Role {
    USER,           // Regular user
    PREMIUM_USER,   // Premium subscription
    MODERATOR,      // Content moderator
    ADMIN,          // System administrator
    SERVICE         // Service-to-service communication
}
```

#### Permissions

```java
public enum Permission {
    // Book permissions
    BOOK_READ,
    BOOK_CREATE,
    BOOK_UPDATE,
    BOOK_DELETE,
    
    // User permissions
    USER_READ,
    USER_UPDATE,
    USER_DELETE,
    
    // Rating permissions
    RATING_CREATE,
    RATING_UPDATE,
    RATING_DELETE,
    
    // Admin permissions
    ADMIN_ACCESS,
    MODERATE_CONTENT,
    
    // Service permissions
    SERVICE_ACCESS
}
```

#### Role-Permission Mapping

```java
@Configuration
public class RolePermissionConfig {
    
    public static final Map<Role, Set<Permission>> ROLE_PERMISSIONS = Map.of(
        Role.USER, Set.of(
            Permission.BOOK_READ,
            Permission.USER_READ,
            Permission.USER_UPDATE,
            Permission.RATING_CREATE,
            Permission.RATING_UPDATE
        ),
        Role.PREMIUM_USER, Set.of(
            Permission.BOOK_READ,
            Permission.USER_READ,
            Permission.USER_UPDATE,
            Permission.RATING_CREATE,
            Permission.RATING_UPDATE
            // Additional premium features
        ),
        Role.MODERATOR, Set.of(
            Permission.BOOK_READ,
            Permission.BOOK_UPDATE,
            Permission.USER_READ,
            Permission.MODERATE_CONTENT
        ),
        Role.ADMIN, Set.of(
            Permission.BOOK_READ,
            Permission.BOOK_CREATE,
            Permission.BOOK_UPDATE,
            Permission.BOOK_DELETE,
            Permission.USER_READ,
            Permission.USER_UPDATE,
            Permission.USER_DELETE,
            Permission.ADMIN_ACCESS,
            Permission.MODERATE_CONTENT
        ),
        Role.SERVICE, Set.of(
            Permission.SERVICE_ACCESS
        )
    );
}
```

#### Security Configuration

```java
package com.prince.novelist.config;

import com.prince.novelist.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // Book endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/books/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/books").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/books/**").hasAnyRole("ADMIN", "MODERATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/books/**").hasRole("ADMIN")
                
                // User endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/users/{userId}").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/users/{userId}").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN")
                
                // Rating endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/users/*/reviews").authenticated()
                
                // Search endpoints
                .requestMatchers("/api/v1/search/**").authenticated()
                
                // Recommendation endpoints
                .requestMatchers("/api/v1/recommendations/**").authenticated()
                
                // Admin endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) 
            throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

#### Method-Level Security

```java
@Service
public class BookService {
    
    @PreAuthorize("hasRole('ADMIN')")
    public Book createBook(CreateBookRequest request) {
        // Only admins can create books
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public Book updateBook(String bookId, UpdateBookRequest request) {
        // Admins and moderators can update books
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBook(String bookId) {
        // Only admins can delete books
    }
    
    @PreAuthorize("@securityService.isOwner(#userId)")
    public User updateUser(String userId, UpdateUserRequest request) {
        // Users can only update their own profile
    }
}
```

---

## Endpoint Security Matrix

| Endpoint | Method | Authentication | Roles | Rate Limit |
|----------|--------|----------------|-------|------------|
| `/api/v1/auth/login` | POST | No | - | 5/min |
| `/api/v1/auth/register` | POST | No | - | 3/min |
| `/api/v1/books` | GET | No | - | 100/hour |
| `/api/v1/books` | POST | Yes | ADMIN | 10/hour |
| `/api/v1/books/{id}` | GET | No | - | 100/hour |
| `/api/v1/books/{id}` | PUT | Yes | ADMIN, MODERATOR | 20/hour |
| `/api/v1/books/{id}` | DELETE | Yes | ADMIN | 10/hour |
| `/api/v1/users/{id}` | GET | Yes | USER (own), ADMIN | 100/hour |
| `/api/v1/users/{id}` | PUT | Yes | USER (own), ADMIN | 20/hour |
| `/api/v1/users/{id}/reviews` | POST | Yes | USER | 50/hour |
| `/api/v1/search/**` | GET | Yes | USER | 100/hour |
| `/api/v1/recommendations/**` | GET | Yes | USER | 50/hour |
| `/api/v1/admin/**` | ALL | Yes | ADMIN | 1000/hour |

---

## Message Queue Security

### Kafka Security

#### SASL/SCRAM Authentication

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: SCRAM-SHA-512
      sasl.jaas.config: |
        org.apache.kafka.common.security.scram.ScramLoginModule required
        username="${KAFKA_USERNAME}"
        password="${KAFKA_PASSWORD}";
      ssl.endpoint.identification.algorithm: https
      ssl.truststore.location: /path/to/truststore.jks
      ssl.truststore.password: ${TRUSTSTORE_PASSWORD}
```

#### ACLs (Access Control Lists)

```bash
# Producer ACLs
kafka-acls --bootstrap-server localhost:9092 \
  --add --allow-principal User:novelist-service \
  --operation Write --topic book-events

# Consumer ACLs
kafka-acls --bootstrap-server localhost:9092 \
  --add --allow-principal User:rag-service \
  --operation Read --topic book-events \
  --group rag-consumer-group
```

---

### RabbitMQ Security

#### User Management

```bash
# Create users
rabbitmqctl add_user novelist-service ${PASSWORD}
rabbitmqctl add_user rag-service ${PASSWORD}

# Set permissions
rabbitmqctl set_permissions -p / novelist-service ".*" ".*" ".*"
rabbitmqctl set_permissions -p / rag-service ".*" ".*" ".*"

# Set user tags
rabbitmqctl set_user_tags novelist-service management
```

#### TLS Configuration

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: 5671
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
    ssl:
      enabled: true
      verify-hostname: true
      key-store: classpath:keystore.p12
      key-store-password: ${KEYSTORE_PASSWORD}
      trust-store: classpath:truststore.jks
      trust-store-password: ${TRUSTSTORE_PASSWORD}
```

---

## API Key Management

### Service-to-Service Authentication

#### API Key Generation

```java
@Service
public class ApiKeyService {
    
    public String generateApiKey() {
        return UUID.randomUUID().toString() + "-" + 
               System.currentTimeMillis();
    }
    
    public String hashApiKey(String apiKey) {
        return BCrypt.hashpw(apiKey, BCrypt.gensalt(12));
    }
    
    public boolean validateApiKey(String apiKey, String hashedKey) {
        return BCrypt.checkpw(apiKey, hashedKey);
    }
}
```

#### API Key Filter

```java
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private ApiKeyService apiKeyService;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String apiKey = request.getHeader("X-API-Key");
        
        if (apiKey != null && apiKeyService.isValidApiKey(apiKey)) {
            // Set authentication for service account
            Authentication auth = new ApiKeyAuthentication(apiKey);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

#### API Key Storage

```java
@Entity
@Table(name = "api_keys")
public class ApiKey {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String hashedKey;
    
    @Column(nullable = false)
    private String serviceName;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    @Column
    private Instant expiresAt;
    
    @Column(nullable = false)
    private Boolean active;
    
    @ElementCollection
    private Set<String> permissions;
}
```

---

## CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // React dev server
            "http://localhost:4200",  // Angular dev server
            "https://novelist.com",   // Production
            "https://app.novelist.com"
        ));
        
        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-API-Key",
            "X-Correlation-ID"
        ));
        
        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
            "X-Total-Count",
            "X-Page-Number",
            "X-Page-Size"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Max age
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
```

---

## Security Headers

```java
@Configuration
public class SecurityHeadersConfig {
    
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean = 
            new FilterRegistrationBean<>();
        
        registrationBean.setFilter(new SecurityHeadersFilter());
        registrationBean.addUrlPatterns("/*");
        
        return registrationBean;
    }
}

public class SecurityHeadersFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Prevent clickjacking
        httpResponse.setHeader("X-Frame-Options", "DENY");
        
        // Prevent MIME sniffing
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        
        // Enable XSS protection
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Content Security Policy
        httpResponse.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self'");
        
        // Strict Transport Security (HTTPS only)
        httpResponse.setHeader("Strict-Transport-Security", 
            "max-age=31536000; includeSubDomains");
        
        // Referrer Policy
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions Policy
        httpResponse.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=()");
        
        chain.doFilter(request, response);
    }
}
```

---

## Data Protection

### Password Hashing

```java
@Service
public class PasswordService {
    
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    
    public String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }
    
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        return passwordEncoder.matches(plainPassword, hashedPassword);
    }
}
```

### Sensitive Data Encryption

```java
@Service
public class EncryptionService {
    
    @Value("${encryption.key}")
    private String encryptionKey;
    
    public String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(
            encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
        
        byte[] iv = new byte[12];
        SecureRandom.getInstanceStrong().nextBytes(iv);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        
        // Combine IV and encrypted data
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    public String decrypt(String encryptedText) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedText);
        
        // Extract IV and encrypted data
        byte[] iv = new byte[12];
        byte[] encrypted = new byte[combined.length - 12];
        System.arraycopy(combined, 0, iv, 0, 12);
        System.arraycopy(combined, 12, encrypted, 0, encrypted.length);
        
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(
            encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
        
        cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
```

### PII Data Masking

```java
@Service
public class DataMaskingService {
    
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];
        
        if (username.length() <= 2) {
            return "**@" + domain;
        }
        
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + "@" + domain;
    }
    
    public String maskCreditCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return cardNumber;
        }
        
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}
```

---

## Security Best Practices

### 1. Input Validation

```java
@Service
public class InputValidationService {
    
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove potentially dangerous characters
        return input.replaceAll("[<>\"']", "");
    }
    
    public boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
```

### 2. Rate Limiting

```java
@Component
public class RateLimitingFilter implements Filter {
    
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientId = getClientIdentifier(httpRequest);
        
        RateLimiter limiter = limiters.computeIfAbsent(clientId, 
            k -> RateLimiter.create(100.0)); // 100 requests per second
        
        if (!limiter.tryAcquire()) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(429);
            httpResponse.getWriter().write("Rate limit exceeded");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private String getClientIdentifier(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            return apiKey;
        }
        
        return request.getRemoteAddr();
    }
}
```

### 3. Audit Logging

```java
@Aspect
@Component
public class SecurityAuditAspect {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    @Around("@annotation(org.springframework.security.access.prepost.PreAuthorize)")
    public Object auditSecuredMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "anonymous";
        
        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setAction(joinPoint.getSignature().getName());
        log.setTimestamp(Instant.now());
        
        try {
            Object result = joinPoint.proceed();
            log.setStatus("SUCCESS");
            return result;
        } catch (Exception e) {
            log.setStatus("FAILURE");
            log.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            auditLogRepository.save(log);
        }
    }
}
```

### 4. Secrets Management

```yaml
# application.yml
spring:
  config:
    import: optional:secrets:/run/secrets/

# Use environment variables for sensitive data
jwt:
  secret: ${JWT_SECRET}
  
neo4j:
  authentication:
    password: ${NEO4J_PASSWORD}
    
kafka:
  password: ${KAFKA_PASSWORD}
```

### 5. Security Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/123"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    @WithMockUser(roles = "USER")
    public void shouldReturn403WhenInsufficientPermissions() throws Exception {
        mockMvc.perform(post("/api/v1/books"))
            .andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    public void shouldAllowAdminToCreateBook() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test\",\"author\":\"Test\"}"))
            .andExpect(status().isCreated());
    }
}
```

---

## References

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [OAuth 2.0 Security Best Practices](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics)

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-18  
**Author**: Bob (AI Assistant)  
**Status**: Draft