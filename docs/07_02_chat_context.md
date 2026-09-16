# LeaseBond Insurance — Project Context & Chat History Summary

## Purpose

This document captures the important business, architecture, database, security, implementation, and development decisions established during the LeaseBond Insurance project discussion. It is intended to be portable context for continuing development in another environment or with another coding agent.

**Status convention:** `IMPLEMENTED` means already developed in the current project; `PLANNED` means agreed design for future work.

---

# 1. Project Overview

## Project name

**LeaseBond Insurance**

## Core business idea

LeaseBond is a proposed rental-risk insurance product where:

- The **Property Owner** purchases the insurance.
- The **Property Owner pays the premium**.
- The tenant does not pay a large traditional security deposit.
- The product protects the Property Owner against specified tenant-related losses.
- After an eligible claim is indemnified, a recovery/subrogation process may pursue the tenant according to the policy, lease, and applicable law.

## Current claim scenarios

1. **Rent default** — tenant fails to pay contractual rent.
2. **Property damage** — tenant causes covered damage to the property.
3. **Eligible early exit / covered rental loss** — tenant leaves early and creates a defined covered rental loss; current discussion uses next-month rent as the example, but exact conditions are not yet fully finalized.

Do not assume every tenant departure or every damage event is automatically covered; policy conditions, exclusions, limits, evidence requirements, and eligibility still belong to the future domain-design phase.

---

# 2. Domain Positioning

The system is broadly positioned as a **Property & Casualty (P&C)** insurance-oriented project.

The rent-default portion is not identical to ordinary landlord property insurance and may overlap conceptually with financial/credit risk or surety/bond concepts. This is a domain-research point, not a finalized legal/product classification.

---

# 3. Current Business Flow

The original flow was simplified.

There is **no separate Underwriting Engine** and **no separate Subrogation Engine**.

Underwriting is a manual process performed by the Underwriter.

```text
PROPERTY OWNER
    |
    | Submit insurance application
    v
UNDERWRITER
    |
    | Review application
    | Assess risk
    | Approve / Reject
    | Set premium manually
    v
PROPERTY OWNER
    |
    | Pay monthly premium
    v
POLICY ACTIVE
    |
    | Lease in progress
    |
    | Loss occurs
    | - Rent default
    | - Property damage
    | - Eligible early exit
    v
PROPERTY OWNER
    |
    | Submit claim + evidence
    v
CLAIMS OFFICER
    |
    | Review coverage
    | Review evidence
    | Assess loss
    | Approve / Reject
    | Determine indemnity
    v
PAYMENT / INDEMNITY
    |
    v
CLAIM SETTLED
    |
    v
RECOVERY / SUBROGATION PROCESS
    |
    v
TENANT
```

Recovery/subrogation is currently part of the Claims domain rather than a separate microservice.

---

# 4. Platform Actors

## Property Owner

Login-based platform user.

Main capabilities:

- Register
- Login
- Submit insurance application
- Enter property information
- Enter tenant information
- Enter lease information
- Upload pre-rental/property-condition evidence
- View application status
- Accept premium
- Pay monthly premiums
- View policy
- Submit claim
- Upload claim evidence
- Track claim
- View settlement
- View notifications

## Underwriter

Internal platform user.

Main capabilities:

- Login
- View pending applications
- Review property/tenant/lease information
- Review risk information
- Review property evidence
- Request additional information
- Approve application
- Reject application
- Set premium manually

## Claims Officer

Internal platform user.

Main capabilities:

- Login
- View pending claims
- Review claim information
- Verify policy coverage
- Review evidence
- Request additional evidence
- Assess loss
- Approve/reject claim
- Determine indemnity
- Manage recovery/subrogation workflow

## Admin

Internal platform administrator.

Main capabilities:

- Login
- Create internal users
- Activate/deactivate users
- Assign roles
- View users
- View operational statistics

---

# 5. Tenant Role

The Tenant is a **domain participant, not currently a platform actor**.

Current decision:

- Tenant does not log in.
- Tenant does not have a tenant portal.
- Tenant does not submit applications or claims.
- Tenant data is collected through the Property Owner's application.
- Tenant may be involved in recovery/subrogation after an eligible claim settlement.

Therefore:

```text
TENANT != APPLICATION USER
```

---

# 6. Public Registration and Role Assignment

Public registration must not allow the client to provide `role` or `status`.

Public registration automatically assigns:

```text
role   = PROPERTY_OWNER
status = ACTIVE
```

A public registration request should look like:

```json
{
  "name": "Test Owner",
  "email": "owner@test.com",
  "password": "Password123"
}
```

Internal roles such as `UNDERWRITER`, `CLAIMS_OFFICER`, and `ADMIN` are intended to be created/managed by the Admin through controlled user management.

A client must not be able to self-register as ADMIN.

---

# 7. Fixed Technical Stack

These technical choices were explicitly fixed:

- Java 17
- Maven
- Spring Boot
- Spring Cloud
- Spring WebFlux
- Functional routing using `RouterFunction` / Handler
- Reactor `Mono` / `Flux`
- Spring Data R2DBC
- `ReactiveCrudRepository`
- MySQL
- R2DBC MySQL driver (`io.asyncer:r2dbc-mysql`)
- Eureka Server
- Spring Cloud Gateway Reactive Gateway
- Spring Security where required by the Gateway/identity layer
- JJWT
- Actuator

The business services should **not** use Spring MVC, JPA, Hibernate, or blocking JDBC repositories.

The API Gateway has no business database and does not need R2DBC or MySQL dependencies.

---

# 8. Planned Microservices

The full planned backend contains seven applications:

```text
eureka-server
api-gateway
identity-service
policy-service
claims-service
payment-service
notification-service
```

Current implementation status:

```text
eureka-server        IMPLEMENTED
identity-service     IMPLEMENTED
api-gateway          IMPLEMENTED / CURRENTLY BEING VERIFIED
policy-service       PLANNED
claims-service       PLANNED
payment-service      PLANNED
notification-service PLANNED
```

---

# 9. Microservice Responsibilities

## Identity Service — IMPLEMENTED

Owns:

- user accounts
- authentication
- JWT issuance
- user roles
- user status

Database:

```text
auth_db
```

Table:

```text
Users
```

## Policy Service — PLANNED

Owns:

- Applications
- Policies
- underwriting workflow
- premium assignment
- policy activation

Database:

```text
policy_db
```

Tables:

```text
Applications
Policies
```

## Claims Service — PLANNED

Owns:

- Claims
- claim evidence references
- coverage verification
- claim assessment
- approval/rejection
- settlement information
- recovery/subrogation process

Database:

```text
claims_db
```

Table:

```text
Claims
```

There is intentionally no separate Settlement, Recovery, or Subrogation microservice for the current capstone scope.

## Payment Service — PLANNED

Owns:

- monthly premium payments
- payment status
- payment transactions
- claim indemnity payouts

Database:

```text
payment_db
```

Table:

```text
Payments
```

The same `Payments` table supports both premium payments and payouts using `payment_type`.

## Notification Service — PLANNED

Owns:

- notifications
- notification status
- policy/payment/claim notification records

Database:

```text
notification_db
```

Table:

```text
Notifications
```

---

# 10. Final Microservice → Database Mapping

| Microservice | Database | Tables |
|---|---|---|
| Identity Service | auth_db | Users |
| Policy Service | policy_db | Applications, Policies |
| Claims Service | claims_db | Claims |
| Payment Service | payment_db | Payments |
| Notification Service | notification_db | Notifications |

The schema was intentionally simplified because this is a capstone rather than a production insurer core system.

---

# 11. Final Planned Database Schemas

## Users

```text
user_id (PK)
name
email
password
role
status
```

Notes:

- `email` should be UNIQUE.
- `password` stores a BCrypt hash, not plaintext.
- `role` and `status` are stored as strings (`VARCHAR`) rather than MySQL ENUM values.
- Java uses `UserRole` and `UserStatus` enums.

Roles:

```text
PROPERTY_OWNER
UNDERWRITER
CLAIMS_OFFICER
ADMIN
```

Status:

```text
ACTIVE
INACTIVE
SUSPENDED
```

## Applications

```text
application_id (PK)
property_owner_id
property_address
property_type
monthly_rent
tenant_name
tenant_credit_score
lease_start_date
lease_end_date
property_evidence_key
premium_amount
underwriter_id
underwriter_remarks
status
submitted_at
```

Simplification decisions:

- no separate Property table
- no separate Tenant table
- no separate Lease table
- no separate Underwriting table

All relevant application-stage information is kept in `Applications`.

`property_evidence_key` points to pre-rental/property-condition evidence in GCS.

## Policies

```text
policy_id (PK)
application_id
policy_number
coverage_amount
premium_amount
coverage_start_date
coverage_end_date
status
issued_date
```

Conceptual relationship:

```text
Application
    |
    | 0..1
    v
Policy
```

An application does not literally become the same database record as a policy. A policy is created after approval and required payment/activation conditions are satisfied.

## Claims

```text
claim_id (PK)
policy_id
claim_type
description
claimed_amount
approved_amount
evidence_key
claims_officer_id
remarks
status
settlement_date
```

The claim table intentionally includes settlement information, so a separate `Settlements` table is not required for the capstone.

## Payments

```text
payment_id (PK)
policy_id
property_owner_id
payment_type
amount
payment_period
transaction_reference
status
payment_date
```

Premium frequency is fixed as **monthly**, so a separate `premium_frequency` field is not needed.

`payment_period` tracks monthly premium periods:

```text
2026-09
2026-10
2026-11
```

`payment_type` can represent:

```text
PREMIUM
PAYOUT
```

## Notifications

```text
notification_id (PK)
user_id
title
message
notification_type
created_at
status
```

---

# 12. Evidence / GCS Design

Actual evidence files are stored in **Google Cloud Storage**.

The database stores a GCS object key/reference.

## Pre-rental evidence

`Applications.property_evidence_key`

Example:

```text
applications/A1001/property-condition-before-lease.mp4
```

Purpose:

- establish the property's initial/pre-rental condition
- provide comparison evidence for future property-damage claims

## Claim evidence

`Claims.evidence_key`

Example:

```text
claims/C5001/damage-photo.jpg
claims/C5001/damage-video.mp4
```

Purpose:

- establish damage/incident evidence after the loss
- support claim review

The simplified schema intentionally uses a single evidence reference per record. A future multi-file design could introduce an Evidence table if required.

---

# 13. User Journey Mapping

The project has a User Journey Mapping artifact with these columns:

```text
Stage
Persona
Action
Goal
Angular/Frontend Page
API/Endpoint
Microservice
Database
```

The journey covers:

- Property Owner registration/login
- application submission
- property/tenant/lease information
- evidence upload
- underwriting
- manual premium setting
- payment
- policy activation
- policy viewing
- claim creation
- claim evidence
- claims review
- settlement
- recovery
- notifications
- Admin user management and statistics

The user journey also provided the high-level API inventory.

---

# 14. RTM

An RTM (Requirements Traceability Matrix) was created as an Excel artifact.

It traces requirements through implementation concepts such as:

```text
Requirement
→ Actor
→ UI
→ API
→ Microservice
→ Database/Table
```

Major requirements represented include:

- registration
- login
- application creation
- property/tenant/lease details
- evidence upload
- application submission
- underwriting review
- approve/reject
- manual premium setting
- monthly premium payment
- policy activation
- claims
- claim evidence
- coverage verification
- claim assessment
- settlement
- recovery
- notifications
- admin user/role management
- dashboard/statistics functionality

---

# 15. Microservice Communication Mapping

The simplified communication model was established as:

| Service | Owns | Calls | Called By | Communication |
|---|---|---|---|---|
| Identity Service | Users, authentication, RBAC | — | Gateway | REST |
| Policy Service | Applications, Policies | Identity, Payment | Gateway, Payment | REST + event |
| Claims Service | Claims, settlement | Policy, Payment | Gateway | REST + event |
| Payment Service | Payments | — | Gateway, Policy, Claims | REST + event |
| Notification Service | Notifications | — | Policy, Claims, Payment | Event |

The exact event broker has not been finalized.

Do not introduce Kafka/RabbitMQ unless a later project decision explicitly requires it.

For a capstone, simple REST plus a limited event mechanism is sufficient.

---

# 16. API Gateway Architecture — IMPLEMENTED

The API Gateway is reactive.

It uses:

- Reactive Gateway
- Eureka Discovery Client
- Spring Security
- JJWT
- Actuator

It does not use:

- Spring MVC
- JPA
- JDBC repositories
- R2DBC/MySQL
- business database

## Why there is no RouterFunction in Gateway

Business services use:

```text
RouterFunction
→ Handler
→ Service
→ Repository
```

The Gateway uses Spring Cloud Gateway's own reactive route abstraction, usually configured through `application.yml`.

Therefore:

```text
Gateway
→ Spring Cloud Gateway routing
→ filters
→ Eureka / load balancing
→ downstream service
```

No business `RouterFunction` or business `Handler` is needed in Gateway routing.

---

# 17. Gateway External vs Internal URLs

The external/public API owns the `/api/v1` prefix.

Example:

```text
External:
POST /api/v1/auth/login
```

Gateway rewrites it to:

```text
Internal Identity Service:
POST /auth/login
```

The `/api/v1` prefix should not be embedded in the individual service's internal route definitions.

---

# 18. Gateway Route Model

Core routes:

```text
/api/v1/auth/**          → IDENTITY-SERVICE
/api/v1/applications/**  → POLICY-SERVICE
/api/v1/policies/**      → POLICY-SERVICE
/api/v1/claims/**        → CLAIMS-SERVICE
/api/v1/payments/**      → PAYMENT-SERVICE
/api/v1/notifications/** → NOTIFICATION-SERVICE
```

Gateway uses `lb://SERVICE-NAME`.

Example:

```yaml
uri: lb://IDENTITY-SERVICE
```

This means:

```text
Gateway
  ↓
LoadBalancer
  ↓
Eureka
  ↓
service instance
```

Do not replace this with hardcoded localhost service URLs in the final architecture.

---

# 19. Eureka — IMPLEMENTED

Eureka is the service registry.

Identity Service registers with Eureka.

API Gateway registers with Eureka and fetches the registry.

Important properties:

```text
register-with-eureka: true
fetch-registry: true
```

Conceptually:

```text
register-with-eureka
→ tell Eureka who I am

fetch-registry
→ get information about registered services
```

Gateway uses Eureka for service discovery and load-balanced routing.

---

# 20. Authentication Architecture — FINAL DECISION

The project uses:

```text
Identity Service
    → issues JWT

API Gateway
    → authenticates / validates JWT

Business microservices
    → authorize requests
```

This is the chosen responsibility split.

## Gateway authentication responsibilities

The Gateway validates:

- JWT signature
- expiration (`exp`)
- issuer (`iss`)
- required claims

The Gateway does **not** decide whether a role is allowed for a business operation.

## Downstream authorization responsibilities

Example:

```text
PROPERTY_OWNER token
    ↓
Gateway authentication succeeds
    ↓
Policy Service
    ↓
Check whether role is allowed for requested operation
    ↓
403 if not allowed
```

Therefore:

```text
Gateway = Authentication
Business service = Authorization
```

---

# 21. Trusted Identity Propagation

After validating JWT, the Gateway should propagate trusted identity data to downstream services.

Required headers:

```text
X-User-Id
X-User-Email
X-User-Role
```

Mapping:

```text
sub   → X-User-Id
email → X-User-Email
role  → X-User-Role
```

The Gateway should remove any client-supplied values for these headers before adding its own values.

This prevents the client from spoofing:

```text
X-User-Role: ADMIN
```

The JWT remains the source of truth.

The trusted-identity flow is:

```text
Client
  ↓
JWT
  ↓
Gateway
  ├── validate JWT
  ├── remove client X-User-* headers
  ├── add trusted X-User-* headers
  ↓
Downstream Service
  ↓
authorization
```

Under the chosen trust model, downstream services do not need the JWT signing secret or independent JWT verification, provided external access to them is blocked and all external traffic enters through the Gateway.

---

# 22. JWT Claims — FINAL DECISION

JWT must contain:

```text
sub   → user_id
email → user email
role  → PROPERTY_OWNER | UNDERWRITER | CLAIMS_OFFICER | ADMIN
iss   → identity-service
iat   → issued-at timestamp
exp   → expiration timestamp
```

The `sub` value can be a numeric user ID converted to a string.

---

# 23. JWT Secret Model

Identity Service signs the token.

API Gateway verifies the token.

Current implementation uses a symmetric HMAC-based JJWT design.

Therefore Identity Service and API Gateway must have the **same signing secret**.

Recommended configuration style:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    issuer: identity-service
```

Secrets should be environment/config driven, not committed directly to source control.

The downstream business services do not need the JWT secret in the selected architecture.

---

# 24. JWT Issuer Validation

The Gateway must check:

```text
iss == identity-service
```

A token with:

```text
iss = another-service
```

must fail validation and return `401 Unauthorized`.

This is separate from signature testing:

- wrong secret → signature verification failure
- right secret + wrong issuer → issuer validation failure

---

# 25. Gateway Security Filter

A custom reactive `JwtAuthenticationWebFilter` exists in the Gateway.

Its responsibilities:

- identify public endpoints
- read `Authorization: Bearer <token>`
- validate JWT
- reject missing/invalid/expired token on protected routes
- propagate trusted identity headers after successful validation
- continue the request chain

A `WebFilter` is a reactive WebFlux filter; it is not the same thing as the complete `SecurityWebFilterChain`, although Spring Security itself is built around WebFlux filters.

---

# 26. Identity Service Routing — IMPLEMENTED

Identity Service uses functional routing.

Internal routes:

```text
POST /auth/register
POST /auth/login
POST /auth/validate
```

Gateway exposes:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/validate
```

This separation is intentional.

---

# 27. Identity Service Layering

The intended implementation pattern is:

```text
Router
   ↓
Handler
   ↓
Service
   ↓
ReactiveCrudRepository
   ↓
R2DBC
   ↓
MySQL
```

No Spring MVC controllers should be introduced.

---

# 28. R2DBC / Database Driver Decision

For business services:

```text
Spring Data R2DBC
        ↓
R2DBC MySQL Driver
        ↓
MySQL
```

Use:

```text
io.asyncer:r2dbc-mysql
```

The normal MySQL Connector/J is JDBC-oriented and is not the R2DBC driver.

Gateway does not need either database driver.

---

# 29. Database Initialization

The R2DBC URL already scopes the connection to a database, e.g.:

```text
r2dbc:mysql://localhost:3306/auth_db
```

Therefore `schema.sql` should:

- create tables
- not create the database
- not use `USE auth_db`

Database creation belongs to environment/setup scripts outside table initialization.

---

# 30. Java Enum + VARCHAR Decision

Java enums exist for:

```text
UserRole
UserStatus
```

But MySQL columns use `VARCHAR`.

A custom `R2dbcConfig` with converters for MySQL ENUM columns was removed because it was unnecessary complexity for the capstone.

Final approach:

```text
Java enum
   ↕
String / VARCHAR
   ↕
MySQL
```

---

# 31. User ID Decision and `save()` Problem

An important issue occurred during Identity Service development.

Original design:

```text
@Id
String userId
```

A UUID was generated manually before calling:

```text
userRepository.save(user)
```

R2DBC saw the non-null ID and interpreted the entity as existing, producing:

```sql
UPDATE users SET ... WHERE user_id = ?
```

The row did not exist, so the operation failed with a resource exception.

The project then chose the simpler design:

```text
user_id BIGINT AUTO_INCREMENT PRIMARY KEY
```

and:

```java
@Id
@GeneratedValue
private Long userId;
```

New entity:

```text
userId = null
```

then:

```text
ReactiveCrudRepository.save()
→ INSERT
```

Existing entity:

```text
userId != null
```

then:

```text
save()
→ UPDATE
```

This avoids adding `@Version`, implementing `Persistable`, or using `R2dbcEntityTemplate` for simple creation paths.

---

# 32. Default Admin

A development default admin is desired.

The password must be BCrypt-hashed with the project's `PasswordEncoder`.

Do not store plaintext admin passwords in `schema.sql`.

The current intended approach is startup initialization through an application lifecycle hook such as `ApplicationReadyEvent`, using the same reactive repository creation flow now that the user ID is database-generated.

Default identity values discussed:

```text
name   = System Admin
email  = admin@leasebond.com
role   = ADMIN
status = ACTIVE
```

If a different credential is used in source/configuration, the code is the implementation source of truth.

---

# 33. Identity Service Testing

Before moving on from Identity Service, test:

## Registration

- successful registration
- password is hashed
- role automatically becomes `PROPERTY_OWNER`
- status automatically becomes `ACTIVE`
- duplicate email rejected
- invalid data rejected
- no role/status accepted from public request

## Login

- correct credentials
- wrong password
- user not found
- inactive/suspended account

## JWT

- exact claims
- signature
- issuer
- expiration
- returned token

## Reactive behavior

- Router
- Handler
- Service
- ReactiveCrudRepository
- R2DBC
- no `.block()` in request flow

---

# 34. API Gateway Testing

Before considering Gateway complete, test:

## Eureka

```text
Eureka Server
API-GATEWAY = UP
IDENTITY-SERVICE = UP
```

## Discovery routing

Gateway must route through:

```text
lb://IDENTITY-SERVICE
```

and not hardcoded `localhost:8081` URLs.

## End-to-end registration

```text
POST /api/v1/auth/register
    ↓
Gateway
    ↓
Eureka
    ↓
Identity Service
```

## End-to-end login

```text
POST /api/v1/auth/login
    ↓
Gateway
    ↓
Eureka
    ↓
Identity Service
    ↓
JWT
```

## JWT negatives

- missing token → 401
- malformed token → 401
- invalid signature → 401
- expired token → 401
- wrong issuer → 401
- missing required claim → 401

## Authorization separation

A valid `PROPERTY_OWNER` JWT should pass Gateway authentication even when the downstream endpoint later requires `UNDERWRITER`.

Gateway authenticates.

Policy Service authorizes.

---

# 35. Future Policy Service Design

The next major implementation target is **Policy Service**.

Responsibilities:

- create insurance applications
- collect property information
- collect tenant information
- collect lease information
- store pre-rental evidence reference
- manage application status
- allow Underwriter review
- allow request for additional information
- allow approve/reject
- allow manual premium setting
- create/activate policy
- provide policy/coverage information

Database:

```text
policy_db
```

Tables:

```text
Applications
Policies
```

Expected reactive architecture:

```text
RouterFunction
   ↓
Handler
   ↓
PolicyService
   ↓
ReactiveCrudRepository
   ↓
R2DBC
   ↓
MySQL
```

Authentication input comes from trusted Gateway identity headers.

Authorization should be implemented in the Policy Service.

Examples:

- Property Owner can create/view own applications.
- Underwriter can review/approve/reject/set premium.
- Admin can perform administrative operations if requirements require them.

---

# 36. Future Claims Service Design

Responsibilities:

- create claim
- select claim type
- submit incident/loss details
- accept claim evidence key/reference
- review coverage
- review evidence
- request additional evidence
- assess loss
- approve/reject
- determine indemnity
- store settlement status/date
- recovery/subrogation workflow

Database:

```text
claims_db
```

Table:

```text
Claims
```

Authentication comes from Gateway identity headers.

Authorization remains in Claims Service.

---

# 37. Future Payment Service Design

Responsibilities:

- accept monthly premium payments
- track payment period
- track transaction reference
- track status
- process indemnity payout

Database:

```text
payment_db
```

Table:

```text
Payments
```

A `payment_type` field distinguishes:

```text
PREMIUM
PAYOUT
```

Monthly premium tracking uses `payment_period`.

---

# 38. Future Notification Service Design

Responsibilities:

- receive notification requests/events
- store notifications
- provide user notification history/status

Database:

```text
notification_db
```

Table:

```text
Notifications
```

Likely notification sources:

- application status changes
- underwriting requests
- premium due/payment success/failure
- claim submitted
- claim approved/rejected
- settlement
- recovery updates

---

# 39. Planned Claims / Policy / Payment / Notification Interaction

Conceptual flow:

```text
Property Owner
   ↓
Policy Service
   ↓
Application
   ↓
Underwriter
   ↓
Premium set
   ↓
Property Owner
   ↓
Payment Service
   ↓
Payment successful
   ↓
Policy becomes active
```

Claim flow:

```text
Property Owner
   ↓
Claims Service
   ↓
Claim submitted
   ↓
Claims Officer
   ↓
Claim approved
   ↓
Payment Service
   ↓
Payout
   ↓
Claim settled
```

Notification flow can later be:

```text
Policy / Claims / Payment
        ↓
      event
        ↓
Notification Service
        ↓
notification record / user notification
```

---

# 40. Important Route and Service Design Rules

Do not create a separate microservice for every noun.

Do not create separate services for:

- Tenant
- Property
- Lease
- Evidence
- Underwriting Engine
- Subrogation Engine
- Settlement

unless requirements materially expand and justify them.

Business ownership is the basis for service boundaries.

---

# 41. Current Development Order

The project planning process followed:

```text
1. Domain research
2. Actor definition
3. User Journey Mapping
4. Microservice → DB mapping
5. DB schema design
6. RTM
7. Microservice communication mapping
8. Authentication/RBAC design
9. Eureka
10. Identity Service
11. API Gateway
12. Policy Service
13. Payment Service
14. Claims Service
15. Notification Service
16. Frontend/integration/testing
```

Current immediate next target:

> **Policy Service**

---

# 42. Rules to Preserve During Future Development

1. Java 17.
2. Maven.
3. Spring WebFlux.
4. Functional Router + Handler for business services.
5. Spring Data R2DBC.
6. ReactiveCrudRepository.
7. MySQL + R2DBC MySQL driver.
8. No JPA/Hibernate.
9. No Spring MVC.
10. Eureka service discovery.
11. Spring Cloud Gateway Reactive Gateway.
12. Gateway owns `/api/v1` public API prefix.
13. Internal services do not need `/api/v1` in their routes.
14. Gateway authenticates JWT.
15. Gateway does not perform business role authorization.
16. Downstream services perform authorization.
17. Trusted identity headers are `X-User-Id`, `X-User-Email`, `X-User-Role`.
18. Client-supplied identity headers must be removed before forwarding.
19. Downstream services do not need JWT secret if they trust only Gateway-originated requests and the trust boundary is enforced.
20. Identity Service issues JWT.
21. JWT secret is shared between Identity Service and Gateway because the current design uses symmetric HMAC.
22. JWT contains `sub`, `email`, `role`, `iss`, `iat`, `exp`.
23. Tenant is not currently a platform actor.
24. Public registration always creates `PROPERTY_OWNER` + `ACTIVE`.
25. Internal roles are Admin-controlled.
26. GCS stores actual evidence files.
27. Database stores GCS object keys.
28. Keep schema/service complexity small for the capstone.
29. Do not add unnecessary microservices or tables.
30. Do not silently replace agreed design with generic architecture preferences.

---

# 43. Final Ground Truth

At the end of this development stage:

```text
Eureka Server             IMPLEMENTED
Identity Service          IMPLEMENTED
API Gateway               IMPLEMENTED / TESTING
Policy Service            NEXT
Claims Service            FUTURE
Payment Service           FUTURE
Notification Service      FUTURE
```

The key architecture is:

```text
                         CLIENT / ANGULAR
                                |
                                v
                       +------------------+
                       |   API GATEWAY    |
                       |      :8080       |
                       |                  |
                       | JWT AUTH         |
                       | Route / Rewrite  |
                       | Trusted Identity |
                       +--------+---------+
                                |
                              Eureka
                                |
            +-------------------+-------------------+
            |                   |                   |
            v                   v                   v
     Identity Service     Policy Service      Claims Service
          :8081                TBD                  TBD
            |
          auth_db
```

The security model is:

```text
Identity Service
    ↓ issues JWT
API Gateway
    ↓ validates JWT
    ↓ adds trusted X-User-* headers
Business Service
    ↓ authorizes user for operation
```

The next implementation target is Policy Service using the same reactive, Eureka-aware architecture and the Gateway-authentication/downstream-authorization trust model.

---

# 44. Continuation Guidance for a New Coding Agent

When continuing this project:

- Inspect the implemented source code before making architectural changes.
- Treat the current Identity Service and API Gateway behavior as the baseline.
- Preserve package naming under `com.thehartford`.
- Preserve the reactive architecture.
- Reuse the same coding style for Router, Handler, Service, Repository, DTO, security/config classes where appropriate.
- Reuse the same Eureka configuration approach.
- Reuse the same JWT/trusted-identity model rather than adding JWT verification independently to every business service.
- Implement Policy Service first.
- Do not assume Claims, Payment, or Notification are already implemented.
- Use the project docs/RTM/user journey mapping to derive planned endpoints and requirements.
- Clearly distinguish actual current implementation from planned design.

This document is a project-development handoff summary, not a replacement for reading the current source code and `docs/` folder before coding.
