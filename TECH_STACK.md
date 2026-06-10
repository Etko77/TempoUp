# TempoUp — Technology Stack

A technology-by-technology breakdown of the TempoUp backend: **what** each piece is,
**where** it's used in the codebase, and **why** it was chosen. For how the features
themselves work, see [`README.md`](README.md).

## Contents
- [Language & runtime](#language--runtime)
- [Application framework](#application-framework)
- [Web & API layer](#web--api-layer)
- [Persistence](#persistence)
- [Geospatial](#geospatial)
- [Database migrations](#database-migrations)
- [Security & authentication](#security--authentication)
- [Real-time messaging](#real-time-messaging)
- [API documentation](#api-documentation)
- [Developer productivity](#developer-productivity)
- [Build & tooling](#build--tooling)
- [Local infrastructure](#local-infrastructure)
- [Testing](#testing)
- [Quick reference table](#quick-reference-table)

---

## Language & runtime

### Java 21
- **Where:** the whole codebase; `pom.xml` sets `<java.version>21</java.version>`.
- **Why:** current Java LTS. The code leans on modern features — **records** for DTOs and
  config (`AppProperties`, every `*Request`/`*Response`), **switch/pattern matching**
  (`CurrentUser`'s `instanceof AuthPrincipal p`), text blocks for the big native SQL query,
  and `HexFormat` / `Long.compareUnsigned` in security and matching code. Java 21 is also the
  baseline required by Spring Boot 3.x.

---

## Application framework

### Spring Boot 3.3.5
- **Where:** the parent POM (`spring-boot-starter-parent`); `TempoUpApplication` is the
  `@SpringBootApplication` entry point; auto-configuration wires datasource, JPA, security,
  WebSocket, and Flyway.
- **Why:** batteries-included framework that removes boilerplate (dependency injection, embedded
  Tomcat, sensible auto-config, externalized configuration) and provides the entire starter
  ecosystem used below. Version 3.3 brings the Jakarta EE 10 / Spring 6 baseline.

### Spring Framework (Core / Context)
- **Where:** dependency injection and `@Transactional` boundaries throughout the service layer
  (`AuthService`, `MatchingService`, `ChatService`, `SuggestionService`, …).
- **Why:** declarative transactions keep multi-step operations atomic (e.g. swipe → match →
  conversation created together, or rolled back together), and DI keeps the feature packages
  loosely coupled.

---

## Web & API layer

### Spring Web MVC — `spring-boot-starter-web`
- **Where:** all `@RestController`s (`AuthController`, `ProfileController`, `SwipeController`,
  `DiscoveryController`, `ConversationController`, `AdminSportController`, …); runs on the
  embedded **Tomcat** server.
- **Why:** the standard, well-understood way to build REST endpoints in Spring; provides JSON
  (Jackson) serialization, request mapping, and the filter chain that security plugs into.

### Bean Validation — `spring-boot-starter-validation` (Hibernate Validator)
- **Where:** `@Valid` request bodies with constraints like `@Email`, `@NotBlank`, `@Size`,
  `@NotNull` on the DTO records (`RegisterRequest`, `UpdateProfileRequest`, `SwipeRequest`, …).
- **Why:** declarative, fail-fast input validation at the edge; violations are turned into a
  clean `400` with a per-field error map by `GlobalExceptionHandler`.

### Jackson (JSON)
- **Where:** automatic (de)serialization of every request/response DTO; also used by JJWT for
  JWT claim serialization.
- **Why:** the de-facto JSON library in Spring Boot; zero-config for `record` DTOs.

---

## Persistence

### Spring Data JPA — `spring-boot-starter-data-jpa` (Hibernate ORM 6.5)
- **Where:** entities (`User`, `Profile`, `Sport`, `Skill`, `UserSport`, `Match`, `Swipe`,
  `Conversation`, `Message`, …) and repository interfaces (`UserRepository`,
  `MatchRepository`, `MessageRepository`, …); `application.yml` sets `ddl-auto: validate`.
- **Why:** repositories remove hand-written CRUD/JDBC; derived query methods
  (`existsBySwiperIdAndSwipedId`, `findByConversationIdOrderByCreatedAtDesc`, …) and
  pagination come for free. `validate` mode makes Hibernate a *guardrail* that asserts the
  entities match the Flyway-managed schema, while Flyway stays the single source of truth.

### PostgreSQL 16 — `org.postgresql:postgresql` (JDBC driver)
- **Where:** the only datasource; configured via `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`.
- **Why:** a robust relational database with first-class **UUID** support, rich constraints
  (the `CHECK`/`UNIQUE` rules in `V1`), and — critically — the **PostGIS** extension for
  geospatial matching.

### HikariCP (connection pool)
- **Where:** bundled with Spring Boot JDBC; visible in startup logs as `HikariPool-1`.
- **Why:** the default, high-performance JDBC connection pool; no configuration needed.

---

## Geospatial

### PostGIS 3.4 + Hibernate Spatial
- **Where:** `profiles.location` is a `GEOGRAPHY(Point, 4326)` (migration `V1`); the discovery
  query (`DiscoveryRepository`) uses `ST_DWithin` for radius filtering and `ST_Distance` for
  ranking; `hibernate-spatial` lets Hibernate understand spatial column types; a GiST index
  accelerates the queries.
- **Why:** matching is fundamentally "who's near me." Storing locations as **geography**
  (WGS-84) means distances are computed in real meters on a sphere, so radius and
  nearest-first ordering are correct without custom haversine math. PostGIS does the heavy
  lifting inside the database, close to the data.

---

## Database migrations

### Flyway — `flyway-core` + `flyway-database-postgresql`
- **Where:** `src/main/resources/db/migration/V1__init_schema.sql` (tables, extensions,
  indexes, constraints) and `V2__seed_data.sql` (admin user + sport/skill catalog); enabled in
  `application.yml`. Runs automatically on startup.
- **Why:** versioned, repeatable, reviewable schema evolution that's identical across every
  environment. It owns DDL outright (Hibernate is set to `validate`, never auto-DDL), which
  prevents schema drift and accidental data loss. The Postgres-specific module is needed for
  Flyway to support PostgreSQL 16.

---

## Security & authentication

### Spring Security — `spring-boot-starter-security`
- **Where:** `SecurityConfig` (stateless filter chain, route rules, CORS, BCrypt encoder,
  `@EnableMethodSecurity`); `JwtAuthFilter` populates the security context; `CurrentUser` /
  `AuthPrincipal` expose the authenticated user.
- **Why:** the standard Spring authentication/authorization framework. Configured **stateless**
  (no sessions) to fit a token-based mobile API, with declarative URL rules (public auth/docs,
  admin-only `/api/admin/**`, authenticated rest) and method-level security.

### BCrypt (password hashing)
- **Where:** the `PasswordEncoder` bean in `SecurityConfig`; used by `AuthService` on
  register/login.
- **Why:** an adaptive, salted password hash — the appropriate way to store credentials at rest.

### JJWT 0.12 (`jjwt-api` / `jjwt-impl` / `jjwt-jackson`)
- **Where:** `JwtService` builds and verifies the **access token** (HS512, signed with
  `JWT_SECRET`), carrying user id, email, and role.
- **Why:** a clean, well-maintained JWT library. JWTs make the access token **stateless** — the
  server validates the signature without a database lookup, which scales well for a read-heavy
  mobile API.

### Opaque refresh tokens + SHA-256 (JDK crypto)
- **Where:** `AuthService` generates a 48-byte `SecureRandom` token, stores only its SHA-256
  hash (`MessageDigest` + `HexFormat`) in `refresh_tokens`, and rotates it on every refresh.
- **Why:** refresh tokens are long-lived, so they're kept **opaque and revocable** (unlike the
  stateless JWT) and **hashed at rest** so a DB leak yields nothing usable. Rotation limits the
  blast radius of a stolen token.

---

## Real-time messaging

### Spring WebSocket + STOMP — `spring-boot-starter-websocket`
- **Where:** `WebSocketConfig` (`@EnableWebSocketMessageBroker`, the `/ws` endpoint, the
  in-memory simple broker on `/topic`+`/queue`, `/app` and `/user` prefixes);
  `ChatWebSocketController` (`@MessageMapping`); `StompAuthChannelInterceptor` authenticates the
  CONNECT frame; `SimpMessagingTemplate.convertAndSendToUser` pushes messages.
- **Why:** chat needs **server-push**, which plain REST can't do. STOMP gives a simple
  pub/sub messaging model over WebSocket, including **per-user destinations**
  (`/user/queue/messages`) so a message is delivered only to its two participants. SockJS is
  enabled as a browser fallback; React Native uses the native WebSocket.

---

## API documentation

### springdoc-openapi 2.6 (Swagger UI)
- **Where:** `springdoc-openapi-starter-webmvc-ui` dependency; `OpenApiConfig` declares the
  `bearerAuth` JWT security scheme; served at `/swagger-ui.html` and `/v3/api-docs`.
- **Why:** generates live, interactive API docs straight from the controllers and DTOs — the
  primary way to explore and test the backend **without a frontend**. The Bearer scheme lets
  you authorize once and call protected endpoints from the browser.

---

## Developer productivity

### Lombok
- **Where:** entities and some DTOs use `@Getter`/`@Setter`/`@Builder`/`@NoArgsConstructor`/
  `@AllArgsConstructor`; excluded from the final boot jar via the Spring Boot Maven plugin
  config.
- **Why:** removes boilerplate (getters, builders, constructors) from JPA entities, keeping them
  readable. It's a compile-time-only tool, so it doesn't ship in the runtime artifact.

---

## Build & tooling

### Maven + Spring Boot Maven Plugin
- **Where:** `pom.xml`; `spring-boot-maven-plugin` provides `spring-boot:run` and repackaging.
- **Why:** declarative dependency management via the Spring Boot parent BOM (consistent,
  curated versions), and a one-command run/build.

### Maven Wrapper (`mvnw` / `mvnw.cmd` / `.mvn/wrapper/`)
- **Where:** committed at the repo root.
- **Why:** pins and auto-downloads the exact Maven version, so contributors don't need Maven
  pre-installed and everyone builds with the same toolchain.

---

## Local infrastructure

### Docker / Docker Compose
- **Where:** `docker-compose.yml` runs `postgis/postgis:16-3.4` (PostgreSQL 16 with PostGIS
  3.4) with a named volume and a published port.
- **Why:** spins up a production-shaped database (including the PostGIS extension) with one
  command, identical for every developer, with no host-level Postgres install required.
  *(Note: the compose file publishes host port `5433` to avoid clashing with a native Postgres
  on `5432` — see README.)*

---

## Testing

### spring-boot-starter-test & spring-security-test
- **Where:** `test` scope in `pom.xml` (JUnit 5, AssertJ, Mockito, Spring Test, plus security
  test helpers).
- **Why:** the standard Spring testing toolkit for unit and integration tests, including
  utilities for authenticating requests in security-aware tests. *(Test sources are not yet
  present in the repo — the harness is wired and ready.)*

---

## Quick reference table

| Technology                  | Layer / Area          | Where (key files)                                            | Why (one-liner)                                              |
|-----------------------------|-----------------------|--------------------------------------------------------------|-------------------------------------------------------------|
| Java 21                     | Language/runtime      | entire codebase                                              | Modern LTS; records, pattern matching, text blocks          |
| Spring Boot 3.3             | Framework             | `pom.xml`, `TempoUpApplication`                              | Auto-config, embedded server, starter ecosystem             |
| Spring Web MVC              | HTTP API              | all `@RestController`s                                       | REST endpoints + JSON over embedded Tomcat                  |
| Bean Validation             | Input validation      | DTO records, `GlobalExceptionHandler`                       | Declarative, fail-fast request validation                   |
| Spring Data JPA / Hibernate | Persistence           | entities, `*Repository`                                      | Repositories, derived queries, schema-validate guardrail    |
| PostgreSQL 16               | Database              | `docker-compose.yml`, `application.yml`                      | Robust RDBMS with UUIDs, constraints, PostGIS               |
| HikariCP                    | Connection pool       | auto (startup logs)                                          | Fast default JDBC pool                                       |
| PostGIS + Hibernate Spatial | Geospatial            | `V1` schema, `DiscoveryRepository`                           | Accurate meter-based proximity matching in-DB               |
| Flyway                      | Migrations            | `db/migration/V1`, `V2`                                      | Versioned, environment-identical schema; owns DDL           |
| Spring Security             | AuthN/AuthZ           | `SecurityConfig`, `JwtAuthFilter`                            | Stateless token security, URL + method rules                |
| BCrypt                      | Password hashing      | `SecurityConfig`, `AuthService`                              | Adaptive salted credential storage                          |
| JJWT                        | Access tokens         | `JwtService`                                                 | Stateless, signature-verified JWTs                          |
| SHA-256 + SecureRandom      | Refresh tokens        | `AuthService`                                                | Opaque, revocable, hashed-at-rest refresh tokens            |
| Spring WebSocket + STOMP    | Real-time chat        | `WebSocketConfig`, `ChatWebSocketController`, STOMP auth     | Server-push messaging with per-user destinations            |
| springdoc-openapi           | API docs              | `OpenApiConfig`, `/swagger-ui.html`                          | Live, interactive, testable API contract                    |
| Lombok                      | Boilerplate           | entities & DTOs                                              | Compile-time getters/builders; not in runtime jar           |
| Maven + Wrapper             | Build/tooling         | `pom.xml`, `mvnw`                                            | Curated deps; reproducible, install-free builds             |
| Docker Compose              | Local infra           | `docker-compose.yml`                                         | One-command production-shaped PostGIS database              |
| Spring/Security Test        | Testing               | `pom.xml` (test scope)                                       | Standard unit/integration test harness                      |
