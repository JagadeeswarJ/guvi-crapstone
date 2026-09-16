# LeaseBond Insurance — Developer Handoff Context

## 1. Project Overview

**LeaseBond Insurance** is a Property & Casualty (P&C) insurance microservices platform designed for the rental property domain.

### Business & Insurance Model
- **Core Value Proposition**: Traditional renting requires tenants to deposit substantial upfront cash (security deposits). LeaseBond replaces large tenant security deposits with a landlord-held insurance policy.
- **Policyholder**: The **Property Owner (Landlord)** pays the insurance premium (billed on a monthly frequency).
- **Insured Risks**: The insurer indemnifies the property owner against eligible tenant-related financial losses:
  1. **Rent Default**: Tenant fails to pay agreed rent as stipulated in the lease.
  2. **Covered Property Damage**: Tenant causes covered physical damage to the rental property exceeding normal wear and tear.
  3. **Early Exit / Abrupt Exit**: Tenant abandons or breaks the lease prematurely, causing defined rental income loss.
- **Tenant Role**: The tenant is a participant in the lease agreement and risk assessment (tenant details, credit score, etc.), but currently has **no direct login, portal account, or application functionality** in the platform.
- **Indemnity & Subrogation**: Once a claim is verified and settled with an indemnity payout to the landlord, the insurer may initiate a recovery/subrogation case against the defaulting tenant (subject to lease terms, policy conditions, and legal doctrines such as the Made-Whole Doctrine and Anti-Subrogation Rule).

### Current Platform Actors (RBAC)
1. `PROPERTY_OWNER`: Registers publicly, submits policy applications, accepts quoted premiums, makes monthly premium payments, files claims, uploads loss evidence.
2. `UNDERWRITER`: Internal actor; reviews pending applications, inspects risk/lease/evidence data, requests additional info, sets monthly premiums, approves or rejects applications.
3. `CLAIMS_OFFICER`: Internal actor; reviews submitted claims, verifies policy coverage, assesses losses, requests evidence, approves/rejects claims, finalizes settlement amounts, initiates recovery cases.
4. `ADMIN`: Internal actor; monitors system statistics across policies, claims, and payments, manages internal users and platform access.

---

## 2. Current Architecture

### Implemented vs. Planned System Topology

```text
                                  CLIENT (Browser / Postman / cURL)
                                                 │
                                                 │ HTTP Requests (/api/v1/**)
                                                 ▼
                             ┌──────────────────────────────────────┐
                             │       API GATEWAY (Port 8080)        │
                             │       [IMPLEMENTED - WebFlux]        │
                             │  - JWT Signature & Claims Validation │
                             │  - Strip client X-User-* headers     │
                             │  - Inject trusted X-User-* headers   │
                             │  - Path Rewrite (/api/v1/* -> /*)    │
                             └───────────────────┬──────────────────┘
                                                 │
                     ┌───────────────────────────┴───────────────────────────┐
                     │ Service Discovery (Heartbeats / Registry Lookup)     │
                     ▼                                                       ▼
      ┌─────────────────────────────┐                         ┌─────────────────────────────┐
      │  EUREKA SERVER (Port 8761)  │                         │ IDENTITY SERVICE (Port 8081)│
      │  [IMPLEMENTED - Standalone] │                         │  [IMPLEMENTED - WebFlux]    │
      │  Service Registry           │                         │  - Functional Router/Handler│
      └─────────────────────────────┘                         │  - Auth (Register/Login)    │
                                                              │  - JJWT Generation/Validate │
                                                              │  - R2DBC MySQL (auth_db)    │
                                                              └──────────────┬──────────────┘
                                                                             │ Reactive SQL
                                                                             ▼
                                                              ┌─────────────────────────────┐
                                                              │   MySQL Database (3306)     │
                                                              │   auth_db.users             │
                                                              └─────────────────────────────┘

══════════════════════════════════════════════════════════════════════════════════════════════════════
                                  DOWNSTREAM SERVICES [PLANNED - NOT YET IMPLEMENTED]
══════════════════════════════════════════════════════════════════════════════════════════════════════
      ┌─────────────────────────────┐                         ┌─────────────────────────────┐
      │   POLICY SERVICE [PLANNED]  │                         │   CLAIMS SERVICE [PLANNED]  │
      │   - Applications & Policies │                         │   - Claims & Settlements    │
      │   - DB: policy_db           │                         │   - DB: claims_db           │
      └─────────────────────────────┘                         └─────────────────────────────┘
      ┌─────────────────────────────┐                         ┌─────────────────────────────┐
      │  PAYMENT SERVICE [PLANNED]  │                         │NOTIFICATION SERVICE[PLANNED]│
      │  - Premiums & Payouts       │                         │   - System & Event Alerts   │
      │  - DB: payment_db           │                         │   - DB: notification_db     │
      └─────────────────────────────┘                         └─────────────────────────────┘
```

---

## 3. Technology Stack

Verified directly from project build manifests (`pom.xml`) and runtime configurations:

- **Language / JDK**: Java 17
- **Build System**: Maven (with `mvnw` / `mvnw.cmd` wrappers in each service)
- **Spring Boot & Spring Cloud**:
  - `identity-service`: Spring Boot `3.3.5`, Spring Cloud `2023.0.3`
  - `api-gateway`: Spring Boot `3.3.5`, Spring Cloud `2023.0.3`
  - `eureka-server`: Spring Boot `4.1.1` (parent), Spring Cloud `2025.1.3` *(Note: see Inconsistencies)*
- **Reactive Engine**: Spring WebFlux (`spring-boot-starter-webflux`, Project Reactor Netty)
- **Programming Paradigm**: Functional Routing (`RouterFunction` + `HandlerFunction`) — **No Spring MVC `@RestController`**
- **Persistence Layer**: Spring Data R2DBC (`spring-boot-starter-data-r2dbc`)
- **Repository Interface**: `ReactiveCrudRepository<T, ID>` — fully non-blocking Mono/Flux
- **Database Driver**: `io.asyncer:r2dbc-mysql:1.2.0` (asynchronous MySQL R2DBC driver)
- **Runtime Database**: MySQL 8.x
- **Service Discovery**: Netflix Eureka (`spring-cloud-starter-netflix-eureka-server`, `spring-cloud-starter-netflix-eureka-client`)
- **API Gateway**: Spring Cloud Gateway Server WebFlux (`spring-cloud-starter-gateway`)
- **Security & Cryptography**:
  - Spring Security WebFlux (`spring-boot-starter-security`)
  - Password hashing: `BCryptPasswordEncoder(12)`
  - JWT Library: JJWT `0.12.6` (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
  - **No OAuth2 Resource Server dependency** — manual token parsing and verification
- **Monitoring & Metrics**: Spring Boot Actuator (`spring-boot-starter-actuator`)
- **Boilerplate Reduction**: Project Lombok (`org.projectlombok:lombok`)

---

## 4. Eureka Server

- **Directory**: `microservices/eureka-server`
- **Application Name**: `eureka-server`
- **Port**: `8761`
- **Main Class**: `com.thehartford.eurekaserver.EurekaServerApplication` (annotated with `@EnableEurekaServer`, `@SpringBootApplication`)
- **Configuration** (`src/main/resources/application.yaml`):
  ```yaml
  server:
    port: 8761
  spring:
    application:
      name: eureka-server
  eureka:
    client:
      register-with-eureka: false
      fetch-registry: false
  management:
    endpoints:
      web:
        exposure:
          include: health,info
    endpoint:
      health:
        show-details: always
  ```
- **Client Registration Standard**: Downstream microservices must:
  1. Add `spring-cloud-starter-netflix-eureka-client`.
  2. Annotate the main application class with `@EnableDiscoveryClient`.
  3. Configure `eureka.client.service-url.defaultZone: http://localhost:8761/eureka/`.
  4. Configure `eureka.instance.prefer-ip-address: true`.

---

## 5. Identity Service

- **Directory**: `microservices/identity-service`
- **Application Name**: `identity-service`
- **Port**: `8081`
- **Package Root**: `com.thehartford.identityservice`

### Complete Code Structure
```text
com.thehartford.identityservice/
├── IdentityServiceApplication.java      // @SpringBootApplication, @EnableDiscoveryClient
├── config/
│   ├── DataInitializer.java            // @EventListener(ApplicationReadyEvent) default admin seeder
│   └── SecurityConfig.java             // PasswordEncoder (BCrypt-12), SecurityWebFilterChain
├── dto/
│   ├── AuthResponse.java               // token, userId, email, role, expiresAt
│   ├── LoginRequest.java               // email, password
│   ├── RegisterRequest.java            // name, email, password (role/status excluded)
│   ├── UserResponse.java               // userId (Long), name, email, role, status
│   ├── ValidateTokenRequest.java       // token
│   └── ValidateTokenResponse.java      // valid, userId, email, role, message
├── handler/
│   ├── AuthHandler.java                // register, login, validateToken
│   └── UserHandler.java                // getUserById, getCurrentUser
├── model/
│   ├── User.java                       // @Table("users"), Long userId (@Id)
│   ├── UserRole.java                   // PROPERTY_OWNER, UNDERWRITER, CLAIMS_OFFICER, ADMIN
│   └── UserStatus.java                 // ACTIVE, INACTIVE, SUSPENDED
├── repository/
│   └── UserRepository.java             // ReactiveCrudRepository<User, Long>
├── router/
│   ├── AuthRouter.java                 // /auth/** routes (NO /api/v1 prefix)
│   └── UserRouter.java                 // /users/** routes (NO /api/v1 prefix)
├── security/
│   ├── HeaderLoggingWebFilter.java     // Temporary filter logging X-User-* headers
│   ├── JwtAuthWebFilter.java           // Service-level JWT extraction & validation
│   └── JwtService.java                 // JJWT 0.12.6 generator & validator
└── service/
    └── UserService.java                // Reactive business logic for users and auth
```

### Database Implementation
- **Database**: `auth_db`
- **R2DBC URL**: `r2dbc:mysql://localhost:3306/auth_db?useSSL=false&serverTimezone=UTC`
- **Table Definition** (`src/main/resources/schema.sql`):
  ```sql
  CREATE TABLE IF NOT EXISTS users (
      user_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
      name     VARCHAR(100) NOT NULL,
      email    VARCHAR(150) NOT NULL UNIQUE,
      password VARCHAR(255) NOT NULL,
      role     VARCHAR(50)  NOT NULL,
      status   VARCHAR(20)  NOT NULL
  );
  ```
  *(Important: `CREATE DATABASE IF NOT EXISTS auth_db;` must be executed prior to boot as R2DBC connects to the database pool before running initialization scripts).*

### Registration & Login Logic
- **Registration (`POST /auth/register`)**:
  - Accepts `RegisterRequest` (`name`, `email`, `password`).
  - `role` is hardcoded to `UserRole.PROPERTY_OWNER`.
  - `status` is hardcoded to `UserStatus.ACTIVE`.
  - `userId` is left `null` so MySQL `AUTO_INCREMENT` assigns the primary key upon `userRepository.save(user)`.
  - Validates duplicate email via `userRepository.existsByEmail()`.
- **Login (`POST /auth/login`)**:
  - Finds user by email; returns 401 if missing.
  - Verifies account status: rejects `SUSPENDED` or `INACTIVE` accounts.
  - Verifies password using `passwordEncoder.matches(rawPassword, encodedPassword)`.
  - Issues signed JWT token.
- **Default Admin Initialization**:
  - Executed by `DataInitializer` upon `ApplicationReadyEvent`.
  - Checks if `admin@leasebond.com` exists.
  - If missing, creates `System Admin` with `role = ADMIN`, `status = ACTIVE`, and password `Admin@123` (hashed with BCrypt).

### JWT Specification
- **Library**: JJWT `0.12.6`
- **Algorithm**: HMAC-SHA (HS256/HS512 using `Keys.hmacShaKeyFor`)
- **Signing Key**: Configured via `app.jwt.secret` in `application.yaml`
- **Issuer**: `identity-service` (configured via `app.jwt.issuer`)
- **Expiration**: 3600000 ms (1 hour, configured via `app.jwt.expiration-ms`)
- **Claims Payload**:
  - `sub`: Numeric user ID serialized as a String (`String.valueOf(user.getUserId())`)
  - `email`: User email address
  - `role`: Role enum name string (`PROPERTY_OWNER`, `UNDERWRITER`, `CLAIMS_OFFICER`, `ADMIN`)
  - `iss`: `identity-service`
  - `iat`: Timestamp of issue
  - `exp`: Timestamp of expiration

---

## 6. API Gateway

- **Directory**: `microservices/api-gateway`
- **Application Name**: `api-gateway` (or `API-GATEWAY`)
- **Port**: `8080`
- **Package Root**: `com.thehartford.apigateway`

### Architecture & Filtering Pipeline
```text
Inbound Request
      │
      ▼
JwtAuthenticationWebFilter
      │
      ├── [Is Public Endpoint?] (/api/v1/auth/register, /api/v1/auth/login, /api/v1/auth/validate, /actuator/**)
      │         │
      │         ├── YES ──► Sanitize: Remove client-supplied X-User-* headers
      │         │           Forward to downstream service via RouteLocator
      │         │
      │         └── NO (Protected Endpoint)
      │                 │
      │                 ├── Check "Authorization: Bearer <token>"
      │                 │         │
      │                 │         ├── Missing / Malformed ──► Return HTTP 401 Unauthorized
      │                 │         ▼
      │                 │   Validate JWT via JwtService (Signature, Exp, Issuer, Claims)
      │                 │         │
      │                 │         ├── Invalid / Expired ──► Return HTTP 401 Unauthorized
      │                 │         ▼
      │                 └── Valid ──► Decorate Request (ServerHttpRequestDecorator):
      │                                 1. Remove client-supplied X-User-* headers
      │                                 2. Inject trusted X-User-Id, X-User-Email, X-User-Role
      │                                 3. Preserve Authorization header intact
      ▼                                 Forward to downstream microservice
Spring Cloud Gateway Routing
      │
      ▼
RewritePath Filter (/api/v1/(?<segment>.*) -> /${segment})
      │
      ▼
LoadBalancer (lb://<SERVICE-ID>)
```

### Configured Gateway Routes (`application.yaml`)

| Route ID | Inbound Path Predicate | Target URI | RewritePath Filter |
| :--- | :--- | :--- | :--- |
| `identity-service-auth` | `/api/v1/auth/**` | `lb://IDENTITY-SERVICE` | `/api/v1/(?<segment>.*)` $\rightarrow$ `/${segment}` |
| `identity-service-users` | `/api/v1/users/**` | `lb://IDENTITY-SERVICE` | `/api/v1/(?<segment>.*)` $\rightarrow$ `/${segment}` |
| `policy-service-applications`| `/api/v1/applications/**`| `lb://POLICY-SERVICE` | `/api/v1/(?<segment>.*)` $\rightarrow$ `/${segment}` |
| `policy-service-policies` | `/api/v1/policies/**` | `lb://POLICY-SERVICE` | `/api/v1/(?<segment>.*)` $\rightarrow$ `/${segment}` |
| `claims-service` | `/api/v1/claims/**` | `lb://CLAIMS-SERVICE` | `/api/v1/(?<segment>.*)` $\rightarrow$ `/${segment}` |
| `payment-service` | `/api/v1/payments/**` | `lb://PAYMENT-SERVICE` | `/api/v1/(?<segment>.*)` $\rightarrow$ `/${segment}` |
| `notification-service` | `/api/v1/notifications/**`| `lb://NOTIFICATION-SERVICE`| `/api/v1/(?<segment>.*)` $\rightarrow$ `/${segment}` |

---

## 7. Current API Inventory

### Implemented Endpoints Across Implemented Services

| Method | Public Gateway Path | Internal Service Path | Owning Service | Handler / Component | Auth Required | Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | `/auth/register` | `identity-service` | `AuthHandler::register` | **No** (Public) | Register new Property Owner |
| `POST` | `/api/v1/auth/login` | `/auth/login` | `identity-service` | `AuthHandler::login` | **No** (Public) | Authenticate & obtain JWT |
| `POST` | `/api/v1/auth/validate` | `/auth/validate` | `identity-service` | `AuthHandler::validateToken` | **No** (Public) | Verify token validity & claims |
| `GET` | `/api/v1/users/me` | `/users/me` | `identity-service` | `UserHandler::getCurrentUser`| **Yes** (Bearer JWT) | Retrieve current user's profile |
| `GET` | `/api/v1/users/{userId}`| `/users/{userId}`| `identity-service` | `UserHandler::getUserById` | **Yes** (Bearer JWT) | Retrieve user by ID |
| `GET` | `/actuator/health` | `/actuator/health` | All services | Actuator Health Endpoint | **No** (Public) | Liveness & health check |
| `GET` | `/actuator/info` | `/actuator/info` | All services | Actuator Info Endpoint | **No** (Public) | Application metadata |

---

## 8. Authentication & Security Model

1. **Authentication Ownership**:
   - Authentication is validated at the **API Gateway** for all incoming external traffic.
   - `identity-service` is the token authority (generates and signs JWTs).
2. **Authorization Ownership (RBAC)**:
   - The Gateway does **NOT** enforce role authorization (e.g. it does not block a `PROPERTY_OWNER` calling an `/applications/pending` route).
   - **Downstream microservices are strictly responsible for their own role authorization** using the propagated trusted headers.
3. **Identity Propagation**:
   - The Gateway extracts claims from the validated JWT and sets HTTP headers:
     - `X-User-Id`: Numeric ID (String representation of user PK)
     - `X-User-Email`: User email
     - `X-User-Role`: `PROPERTY_OWNER`, `UNDERWRITER`, `CLAIMS_OFFICER`, `ADMIN`
   - Client-supplied `X-User-*` headers are stripped via `ServerHttpRequestDecorator` to avoid spoofing.
   - The original `Authorization: Bearer <token>` header is **forwarded intact** so downstream services can also validate the JWT directly if desired.
4. **Secret Sharing**:
   - The Gateway and `identity-service` share the same HMAC secret key (`app.jwt.secret`) and issuer name (`app.jwt.issuer`).

---

## 9. Database Configuration

### Implemented Database (`auth_db`)
- **Database Engine**: MySQL 8.x
- **Access Protocol**: Reactive R2DBC (`r2dbc:mysql://localhost:3306/auth_db`)
- **Table**: `users`
  - `user_id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `name`: `VARCHAR(100) NOT NULL`
  - `email`: `VARCHAR(150) NOT NULL UNIQUE`
  - `password`: `VARCHAR(255) NOT NULL` (BCrypt encoded)
  - `role`: `VARCHAR(50) NOT NULL` (`PROPERTY_OWNER`, `UNDERWRITER`, `CLAIMS_OFFICER`, `ADMIN`)
  - `status`: `VARCHAR(20) NOT NULL` (`ACTIVE`, `INACTIVE`, `SUSPENDED`)

### Planned Databases (From Documentation)

| Microservice | Target Database | Intended Tables |
| :--- | :--- | :--- |
| `policy-service` | `policy_db` | `Applications`, `Policies` |
| `claims-service` | `claims_db` | `Claims` |
| `payment-service` | `payment_db` | `Payments` |
| `notification-service` | `notification_db` | `Notifications` |

---

## 10. Important Implementation Decisions

1. **Reactive Throughout**: Spring WebFlux with Project Reactor (`Mono` / `Flux`). No blocking calls (`.block()` is strictly avoided).
2. **Functional Endpoints**: No `@RestController` or Spring MVC mappings. WebFlux `RouterFunction` + `HandlerFunction` is used for all service endpoints.
3. **Database IDs**: User ID is numeric (`BIGINT AUTO_INCREMENT` / `Long`), replacing earlier conceptual UUID designs so that R2DBC's `ReactiveCrudRepository.save()` detects a null ID and executes an `INSERT` rather than an erroneous `UPDATE`.
4. **No Database in Gateway**: The Gateway has zero database dependencies, JPA, or R2DBC drivers.
5. **No OAuth2 Resource Server**: Token validation is performed manually using the JJWT parser to maintain a lightweight reactive footprint.
6. **Path Prefix Ownership**: The `/api/v1` prefix is exclusively owned and stripped by the API Gateway; downstream microservices expose clean root paths (`/auth/**`, `/users/**`, etc.).

---

## 11. Current Limitations & Not Yet Implemented

1. **Downstream Domain Microservices Are Unimplemented**:
   - `policy-service`, `claims-service`, `payment-service`, and `notification-service` directories are currently empty placeholders.
2. **Admin User Management APIs**:
   - Planned endpoints `POST /users`, `PUT /users/{id}/status`, `PUT /users/{id}/roles`, and `GET /users` (defined in RTM REQ-036/037) are not yet implemented in `identity-service`.
3. **Inter-Service Event Broker**:
   - Documented event communication (`03_microservice_comm.md`) between Policy, Claims, Payment, and Notification is not yet implemented (no Kafka/RabbitMQ broker configuration exists).
4. **Google Cloud Storage (GCS)**:
   - GCS bucket configuration and client SDKs for property evidence (`property_evidence_key`) and claim proof (`evidence_key`) are not yet integrated.
5. **Frontend Application**:
   - The Angular frontend referenced in `01_main-plan.xlsx` and `02_LeaseBond_RTM.xlsx` has not been constructed.
6. **Temporary Header Logging**:
   - `HeaderLoggingWebFilter.java` in `identity-service` is currently logging `X-User-*` headers for debugging/verification purposes.

---

## 12. Planned Policy Service

- **Folder**: `microservices/policy-service`
- **Planned Port**: `8082`
- **Database**: `policy_db`
- **Planned Tables & Schemas** (`01_main-plan.xlsx` / schemas sheet):
  - **`Applications`**:
    - `application_id (PK)`
    - `property_owner_id` (foreign ref to `users.user_id`)
    - `property_address`
    - `property_type`
    - `monthly_rent`
    - `tenant_name`
    - `tenant_credit_score`
    - `lease_start_date`
    - `lease_end_date`
    - `property_evidence_key` (GCS object key)
    - `premium_amount` (set manually by Underwriter)
    - `underwriter_id` (foreign ref to `users.user_id`)
    - `underwriter_remarks`
    - `status` (`DRAFT`, `SUBMITTED`, `INFO_REQUESTED`, `APPROVED`, `REJECTED`, `ACCEPTED`)
    - `submitted_at`
  - **`Policies`**:
    - `policy_id (PK)`
    - `application_id`
    - `policy_number` (unique string identifier)
    - `coverage_amount`
    - `premium_amount`
    - `coverage_start_date`
    - `coverage_end_date`
    - `status` (`ACTIVE`, `EXPIRED`, `CANCELLED`)
    - `issued_date`
- **Core Endpoints Planned**:
  - `POST /applications` (Property Owner creates draft application)
  - `PUT /applications/{id}/property`
  - `PUT /applications/{id}/tenant`
  - `PUT /applications/{id}/lease`
  - `POST /applications/{id}/evidence` (or document upload)
  - `POST /applications/{id}/submit`
  - `GET /applications/pending` (Underwriter view)
  - `GET /applications/{id}`
  - `POST /applications/{id}/request-info`
  - `POST /applications/{id}/additional-info`
  - `PUT /applications/{id}/approve`
  - `PUT /applications/{id}/reject`
  - `PUT /applications/{id}/premium` (Underwriter quotes monthly premium)
  - `POST /applications/{id}/accept` (Property Owner accepts premium)
  - `POST /policies/activate` (System/service policy activation post-payment)
  - `GET /policies/{policyId}`

---

## 13. Planned Claims Service

- **Folder**: `microservices/claims-service`
- **Planned Port**: `8083`
- **Database**: `claims_db`
- **Planned Table & Schema**:
  - **`Claims`**:
    - `claim_id (PK)`
    - `policy_id` (foreign ref to `policies.policy_id`)
    - `claim_type` (`RENT_DEFAULT`, `PROPERTY_DAMAGE`, `EARLY_EXIT`)
    - `description`
    - `claimed_amount`
    - `approved_amount`
    - `evidence_key` (GCS object key)
    - `claims_officer_id` (foreign ref to `users.user_id`)
    - `remarks`
    - `status` (`FILED`, `UNDER_REVIEW`, `INFO_REQUESTED`, `APPROVED`, `REJECTED`, `SETTLED`)
    - `settlement_date`
- **Core Endpoints Planned**:
  - `POST /claims` (Property Owner creates claim against active policy)
  - `PUT /claims/{claimId}/type`
  - `PUT /claims/{claimId}/details`
  - `POST /claims/{claimId}/evidence` (GCS upload)
  - `POST /claims/{claimId}/submit`
  - `GET /claims/pending` (Claims Officer queue)
  - `GET /claims/{claimId}`
  - `POST /claims/{claimId}/request-info`
  - `PUT /claims/{claimId}/approve`
  - `PUT /claims/{claimId}/reject`
  - `PUT /claims/{claimId}/settlement` (Finalize indemnity amount)
  - `PUT /claims/{claimId}/settle` (Mark claim settled)
  - `POST /recoveries` (Initiate tenant subrogation/recovery case)

---

## 14. Planned Payment Service

- **Folder**: `microservices/payment-service`
- **Planned Port**: `8084`
- **Database**: `payment_db`
- **Planned Table & Schema**:
  - **`Payments`**:
    - `payment_id (PK)`
    - `policy_id`
    - `property_owner_id`
    - `payment_type` (`PREMIUM`, `INDEMNITY_PAYOUT`)
    - `amount`
    - `payment_period` (tracks monthly billing cycle e.g. `2026-10`)
    - `transaction_reference` (mock payment gateway transaction ID)
    - `status` (`PENDING`, `COMPLETED`, `FAILED`)
    - `payment_date`
- **Core Endpoints Planned**:
  - `POST /payments` (Property Owner premium payment)
  - `GET /payments/{paymentId}`
  - `GET /payments/policy/{policyId}` (Payment history by policy)
  - `POST /payouts` (Disburse approved indemnity payout to Property Owner)
  - `GET /admin/payments/stats`

---

## 15. Planned Notification Service

- **Folder**: `microservices/notification-service`
- **Planned Port**: `8085`
- **Database**: `notification_db`
- **Planned Table & Schema**:
  - **`Notifications`**:
    - `notification_id (PK)`
    - `user_id`
    - `title`
    - `message`
    - `notification_type` (`POLICY_UPDATE`, `PAYMENT_REMINDER`, `CLAIM_UPDATE`, `RECOVERY_NOTICE`)
- **Core Endpoints Planned**:
  - `POST /notifications` (Internal event listener or service call)
  - `GET /notifications` (User views inbox)

---

## 16. Rules the Next Developer Must Preserve

1. **Preserve WebFlux Functional Routing**: Continue using `RouterFunction<ServerResponse>` and `HandlerFunction` for all future microservices. Do **NOT** use Spring MVC `@RestController`.
2. **Preserve Reactive R2DBC**: Use `ReactiveCrudRepository` with non-blocking R2DBC MySQL drivers. Never introduce JDBC, Hibernate, or Spring Data JPA.
3. **No Blocking Operations**: Avoid `.block()` or synchronous file/network I/O across all services.
4. **Honor Path Rewriting**: Microservices must define routes **without** the `/api/v1` prefix (e.g. `/applications/**`, `/claims/**`). The API Gateway handles the public `/api/v1` prefix and rewrites it downstream.
5. **Enforce Role Authorization in Downstream Services**:
   - Inspect the trusted Gateway headers: `X-User-Id`, `X-User-Email`, `X-User-Role`.
   - Protect sensitive endpoints:
     - Underwriting endpoints $\rightarrow$ require `X-User-Role == "UNDERWRITER"`.
     - Claims endpoints $\rightarrow$ require `X-User-Role == "CLAIMS_OFFICER"`.
     - Property Owner endpoints $\rightarrow$ verify `X-User-Role == "PROPERTY_OWNER"` and `X-User-Id == resource.property_owner_id`.
6. **Preserve Evidence Reference Pattern**: Never store raw file bytes in MySQL. Store string object keys pointing to Google Cloud Storage (e.g. `property_evidence_key`, `evidence_key`).
7. **Database Isolation**: Keep databases strictly per microservice (`auth_db`, `policy_db`, `claims_db`, `payment_db`, `notification_db`). Do not create cross-database foreign key constraints.

---

## 17. Recommended Next Implementation Target

The next logical service to implement is **`policy-service`**:
1. It is the immediate next step in the business lifecycle (`Property Owner registers -> applies for policy -> underwriter reviews -> underwriter approves and sets premium`).
2. Both `claims-service` and `payment-service` depend directly on active `Policies` and `Applications` created by `policy-service`.
3. Setting up `policy_db` with `Applications` and `Policies` tables will unlock end-to-end testing from registration to policy generation through the existing API Gateway.

---

## Current Ground Truth

- **Implemented Services**:
  1. `eureka-server` (Port 8761) — Functional Service Registry
  2. `identity-service` (Port 8081) — Functional WebFlux Auth & JWT Provider
  3. `api-gateway` (Port 8080) — Functional Reactive Gateway with JWT Validation & Header Sanitization
- **Planned Services**: `policy-service`, `claims-service`, `payment-service`, `notification-service`
- **Implemented APIs**:
  - `POST /api/v1/auth/register` (Public)
  - `POST /api/v1/auth/login` (Public)
  - `POST /api/v1/auth/validate` (Public)
  - `GET /api/v1/users/me` (Protected)
  - `GET /api/v1/users/{userId}` (Protected)
  - `/actuator/health`, `/actuator/info`
- **Implemented Databases/Tables**:
  - MySQL `auth_db` $\rightarrow$ `users` table (`user_id`, `name`, `email`, `password`, `role`, `status`)
- **Authentication Model**: Stateless JWT (HS256/512), validated at the API Gateway.
- **Authorization Model**: Gateway authenticates token; downstream microservices authorize roles via trusted `X-User-*` headers.
- **Eureka Setup**: Standalone registry on port `8761`. `identity-service` and `api-gateway` actively register as Eureka clients.
- **Gateway Setup**: Port `8080`, routes `/api/v1/**` with path rewrite to Eureka `lb://` service instances.
- **Next Implementation Target**: `policy-service` (`policy_db`, `Applications`, `Policies`).
