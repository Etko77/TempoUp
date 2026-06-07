# TempoUp API

Backend for **TempoUp** — a sport-partner matching app. Users build a profile, pick the
sports and skills they care about, and then *swipe* through nearby people who play the same
things. When two people like each other it's a **match**, which opens a private real-time
chat. Think "dating-app mechanics, but for finding a gym / ski / tennis buddy."

**Stack:** Spring Boot 3.3 · Java 21 · PostgreSQL + PostGIS · Flyway · Spring Security + JWT ·
STOMP WebSocket chat · springdoc OpenAPI. See [`TECH_STACK.md`](TECH_STACK.md) for a full
breakdown of every technology, where it's used, and why.

---

## Table of contents
- [How the app works (end to end)](#how-the-app-works-end-to-end)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Running locally](#running-locally)
- [Configuration](#configuration)
- [How each subsystem works](#how-each-subsystem-works)
  - [Data model](#data-model)
  - [Authentication & tokens](#authentication--tokens)
  - [Request authorization](#request-authorization)
  - [Sports, skills & community suggestions](#sports-skills--community-suggestions)
  - [Profiles & geolocation](#profiles--geolocation)
  - [Discovery: the swipe feed](#discovery-the-swipe-feed)
  - [Swiping & matching](#swiping--matching)
  - [Chat: REST + WebSocket](#chat-rest--websocket)
  - [Error handling](#error-handling)
- [Seeded data & accounts](#seeded-data--accounts)
- [API reference](#api-reference)
- [Testing the backend without a frontend](#testing-the-backend-without-a-frontend)
- [Project layout](#project-layout)
- [Production notes / next steps](#production-notes--next-steps)

---

## How the app works (end to end)

A typical user journey, and what happens on the server for each step:

1. **Register** (`POST /api/auth/register`) → a `users` row + an empty `profiles` row are
   created, and the server returns a short-lived **access token** and a long-lived
   **refresh token**.
2. **Complete the profile** (`PUT /api/profiles/me`) → name, bio, city, and crucially a
   **location** (latitude/longitude), which is stored as a PostGIS geographic point.
3. **Pick sports & skills** (`PUT /api/sports/mine`) → e.g. "Gym (priority) + Bench Press,
   Squat". This is what the matching algorithm compares between users.
4. **Open the swipe feed** (`GET /api/discovery`) → the server returns nearby candidates,
   ranked by how much sporting overlap you share and how close they are.
5. **Swipe** (`POST /api/swipes`) with `LIKE` or `PASS`. If you `LIKE` someone who already
   liked you, the server atomically creates a **match** and a **conversation**.
6. **Chat** → list matches/conversations over REST, and exchange messages in real time over
   a STOMP WebSocket. Messages are persisted and pushed to both participants.

Admins additionally moderate a queue of user-submitted **sport/skill suggestions**, turning
approved ones into real catalog entries.

---

## Architecture

The codebase is organized **by feature** (a "package-per-feature" / vertical-slice layout)
rather than by technical layer. Each feature package contains its own controller(s),
service(s), JPA entities, repositories, and DTOs:

```
com.tempoup.api
├── auth        — registration, login, refresh-token rotation, logout
├── user        — User entity + Role (the identity/credential record)
├── profile     — public-facing profile, including geolocation
├── sport       — sports & skills catalog, a user's chosen sports/skills,
│                 and the community suggestion + admin-moderation flow
├── matching    — discovery feed, swipes, matches (the core mechanic)
├── chat        — conversations, messages, REST + STOMP WebSocket delivery
├── common
│   ├── security   — JWT service, auth filter, STOMP auth, principal helpers
│   └── exception  — ApiException + global @RestControllerAdvice handler
└── config      — Security, WebSocket, OpenAPI, and typed app properties
```

Within a feature the layering is conventional:
**Controller** (HTTP/STOMP edge, validation) → **Service** (`@Transactional` business logic)
→ **Repository** (Spring Data JPA) → **Entity** (mapped to a Flyway-managed table).
DTOs (Java `record`s) cross the HTTP boundary so entities are never serialized directly.

---

## Prerequisites
- **JDK 21** (the build targets Java 21 — JDK 17 will *not* compile it)
- **Docker** (for the PostgreSQL + PostGIS database)
- Maven is **not** required to be installed — a **Maven Wrapper** (`./mvnw` / `mvnw.cmd`) is
  committed to the repo and downloads the correct Maven version on first use.

## Running locally

```bash
# 1. Start the database (PostgreSQL 16 + PostGIS 3.4)
docker compose up -d

# 2. Run the app — Flyway applies the schema + seed data automatically on startup
./mvnw spring-boot:run         # macOS/Linux
.\mvnw.cmd spring-boot:run     # Windows PowerShell
```

- API:        http://localhost:8080
- Swagger UI:  http://localhost:8080/swagger-ui.html
- OpenAPI doc: http://localhost:8080/v3/api-docs

> **⚠️ Database port note.** Postgres' default port is `5432`. If you already run a native
> PostgreSQL on your machine, it will occupy `5432` and the app will silently connect to the
> *wrong* database (you'll see `password authentication failed for user "tempoup"`). To avoid
> that, the bundled `docker-compose.yml` publishes the container on host port **`5433`**.
> When the host port differs from the app's default, point the app at it with an env var:
>
> ```bash
> # macOS/Linux
> DB_URL=jdbc:postgresql://localhost:5433/tempoup ./mvnw spring-boot:run
> # Windows PowerShell
> $env:DB_URL='jdbc:postgresql://localhost:5433/tempoup'; .\mvnw.cmd spring-boot:run
> ```
>
> If you have **no** native Postgres, you can instead change the compose mapping back to
> `"5432:5432"` and drop the `DB_URL` override entirely.

### Running from IntelliJ IDEA
1. Right-click `pom.xml` → **Add as Maven Project** (so the IDE resolves dependencies and
   marks `src/main/java` as sources — otherwise the green ▶ has no classpath).
2. **Project Structure** → set the **SDK to Java 21**.
3. Run `TempoUpApplication`. A ready-made **"TempoUp API"** Spring Boot run configuration is
   committed under `.idea/runConfigurations/`, pre-set with the `DB_URL=…5433` env var.

---

## Configuration

All settings live in `src/main/resources/application.yml` and are overridable via environment
variables (shown with their defaults):

| Env var            | Default                                         | Purpose                                            |
|--------------------|-------------------------------------------------|----------------------------------------------------|
| `DB_URL`           | `jdbc:postgresql://localhost:5432/tempoup`      | JDBC URL of the database                           |
| `DB_USERNAME`      | `tempoup`                                        | DB user                                            |
| `DB_PASSWORD`      | `tempoup`                                        | DB password                                        |
| `SERVER_PORT`      | `8080`                                           | HTTP port                                          |
| `JWT_SECRET`       | a long dev placeholder                           | HMAC signing key (**must** be ≥ 32 bytes)          |
| `JWT_ACCESS_TTL`   | `60`                                             | Access-token lifetime (minutes)                    |
| `JWT_REFRESH_TTL`  | `30`                                             | Refresh-token lifetime (days)                      |
| `CORS_ORIGINS`     | Expo/React-Native dev origins                    | Comma-separated allowed CORS origins               |
| `MATCH_RADIUS_KM`  | `50`                                             | Default discovery radius                           |
| `MATCH_FEED_SIZE`  | `30`                                             | Max candidates returned per feed call              |
| `SQL_LOG`          | `info`                                           | Set to `debug` to log generated SQL                |

Typed configuration is bound into the immutable `AppProperties` record
(`@ConfigurationProperties(prefix = "app")`) and injected wherever needed. Flyway owns the
schema; Hibernate runs with `ddl-auto: validate`, so the app refuses to start if the entities
and the migrated schema ever drift apart.

---

## How each subsystem works

### Data model
Defined entirely in Flyway migration `V1__init_schema.sql` (PostGIS + `uuid-ossp` extensions
are enabled there). All primary keys are UUIDs.

| Table               | Holds                                                                          |
|---------------------|--------------------------------------------------------------------------------|
| `users`             | Credentials & role (`USER` / `ADMIN`), enabled flag                            |
| `refresh_tokens`    | SHA-256 hashes of issued refresh tokens, with expiry & revoked flags           |
| `profiles`          | 1:1 with users; display name, bio, city, photo, and a `GEOGRAPHY(Point,4326)`  |
| `sports` / `skills` | The catalog; skills belong to a sport (`uq_skill_per_sport`)                    |
| `user_sports`       | A user's chosen sports, with proficiency + a `is_priority` flag                |
| `user_skills`       | Skills chosen under one of the user's `user_sports`                            |
| `sport_suggestions` | Community-submitted sports/skills awaiting admin review                        |
| `swipes`            | One row per (swiper → swiped), `LIKE`/`PASS`; can't swipe yourself             |
| `matches`           | A mutual like; stored in **canonical order** (`user_a_id < user_b_id`)         |
| `conversations`     | 1:1 with a match                                                               |
| `messages`          | Chat messages within a conversation                                            |

A spatial **GiST index** on `profiles.location` makes "within radius" queries fast.

### Authentication & tokens
Two-token scheme (`AuthService`, `JwtService`):

- **Access token** — a stateless **JWT** (HS512, signed with `JWT_SECRET`) carrying the user
  id (`sub`), email, and role. Default lifetime 60 min. Sent as `Authorization: Bearer <jwt>`
  on every protected request. Nothing about it is stored server-side.
- **Refresh token** — an **opaque** 48-byte random string (base64url). Only its **SHA-256
  hash** is stored in `refresh_tokens`, so a database leak never exposes usable tokens.
  Default lifetime 30 days.

Token lifecycle:
- **Register/Login** → password is BCrypt-checked, then a fresh access + refresh pair is issued.
- **Refresh** (`POST /api/auth/refresh`) → the presented refresh token is looked up by hash;
  if valid and unexpired it is **rotated** (the old one is revoked and a new pair issued).
- **Logout** → all of the user's refresh tokens are revoked.

### Request authorization
`SecurityConfig` runs the API **stateless** (no HTTP session), with CSRF disabled and CORS
configured from `AppProperties`. On each request, `JwtAuthFilter` (a `OncePerRequestFilter`)
reads the `Bearer` token, validates it, and populates the Spring Security context with an
`AuthPrincipal` (user id + role) and a `ROLE_<role>` authority. An invalid token simply leaves
the request unauthenticated. Route rules:

- **Public:** `/api/auth/**`, the Swagger/OpenAPI endpoints, the `/ws/**` handshake, and
  read-only catalog `GET /api/sports` & `GET /api/sports/{id}/skills`.
- **Admin only:** `/api/admin/**` (`hasRole('ADMIN')`).
- **Everything else:** authenticated.

`@EnableMethodSecurity` is on, and controllers read the current user via the `CurrentUser`
helper rather than trusting any client-supplied id.

### Sports, skills & community suggestions
The catalog (`sport` package) is seeded but extensible by the community:

- Any user can submit a **suggestion** for a new sport or a new skill under an existing sport
  (`POST /api/sports/suggestions`). It is saved as `PENDING`.
- Admins review the queue (`GET /api/admin/suggestions`) and **approve** or **reject**.
  Approval is where a suggestion becomes a real `sports`/`skills` row; `SuggestionService`
  guards against duplicates and against re-reviewing an already-decided suggestion (idempotency).

Users attach sports to themselves with proficiency and an `is_priority` flag, plus specific
skills — this is the raw material the matching score is built from.

### Profiles & geolocation
`PUT /api/profiles/me` accepts `latitude`/`longitude`, which the service converts into a
PostGIS `GEOGRAPHY(Point, 4326)` (WGS-84). Storing it as *geography* (not plain geometry) means
distance math is in **real meters on a sphere**, so radius filtering and distance ranking are
accurate without manual haversine code.

### Discovery: the swipe feed
`GET /api/discovery?radiusKm=&limit=` is powered by a single native SQL query in
`DiscoveryRepository`. For the current user it computes, per candidate:

```
score = (shared sports)              * 10
      + (shared sports either side flagged "priority") * 5
      + (shared skills)              * 2
```

and returns candidates **ordered by score desc, then nearest first**. The query:
- excludes yourself, disabled users, and anyone you've already swiped on;
- filters by `ST_DWithin(location, location, radiusMeters)` when both users have a location
  (users without a location are still allowed through, just unranked by distance);
- reports distance via `ST_Distance(...) / 1000` (km).

It returns a lightweight `DiscoveryRow` projection, not full entities.

### Swiping & matching
`POST /api/swipes` (`MatchingService.swipe`) records a `LIKE`/`PASS`, rejecting self-swipes,
swipes at non-existent users, and duplicate swipes. When a `LIKE` lands on someone who has
**already** liked you, the service — in a single transaction — creates the `matches` row and
its `conversations` row and returns `matched: true` with the new ids.

> **Implementation detail — canonical match ordering.** The `matches` table enforces
> `CHECK (user_a_id < user_b_id)` so each pair has exactly one row regardless of who swiped
> first. The two ids must therefore be sorted to agree with **PostgreSQL's unsigned** UUID
> ordering. `Match.ordered(...)` does this with `Long.compareUnsigned` on the UUID halves —
> *not* `UUID.compareTo`, which sorts the halves as **signed** longs and disagrees with the
> database whenever one UUID's leading byte is ≥ `0x80` (that mismatch would violate the
> CHECK constraint and fail the match).

### Chat: REST + WebSocket
Two complementary interfaces over the same persisted data (`chat` package):

- **REST** — `GET /api/conversations`, `GET /api/conversations/{id}/messages` (paginated),
  `POST /api/conversations/{id}/messages`. Every call authorizes that the caller is one of the
  conversation's two participants.
- **WebSocket (STOMP)** — clients open a STOMP connection at `/ws` (SockJS fallback available).
  The JWT is read from the `Authorization` header **on the CONNECT frame** by
  `StompAuthChannelInterceptor`, which sets the session principal's name to the user id.
  - Send a message to `/app/conversations/{conversationId}/send` with body `{"content":"…"}`.
  - The server persists it and pushes it to **both** participants' private queues via
    `convertAndSendToUser(userId, "/queue/messages", …)` — so subscribe to
    `/user/queue/messages` to receive.

The broker is an **in-memory simple broker** (`/topic`, `/queue`) — fine for a single instance;
see [production notes](#production-notes--next-steps) for scaling.

### Error handling
`GlobalExceptionHandler` (`@RestControllerAdvice`) produces a consistent JSON `ErrorResponse`
(`timestamp`, `status`, `error`, `message`, `fieldErrors`):

- `ApiException` → its carried status (e.g. `404`, `409`, `401`) — the app's intentional errors.
- Bean-validation failures → `400` with a per-field `fieldErrors` map.
- Spring Security `AccessDeniedException` → `403`.
- Anything else → `500`. *(Note: malformed request bodies — e.g. a non-UUID where a UUID is
  expected — currently fall through to this generic `500` rather than a `400`.)*

---

## Seeded data & accounts
Applied by `V2__seed_data.sql` on first startup:

- **Admin account:** `admin@tempoup.bg` / `admin123` — **change before any real use.**
- A starter catalog of **12 sports** with skills (Gym has 10 skills, Weightlifting 2, etc.).

Regular users are created via registration; only the admin is seeded.

---

## API reference

| Area        | Method & path                                                   | Notes                              |
|-------------|-----------------------------------------------------------------|------------------------------------|
| Auth        | `POST /api/auth/register` · `/login` · `/refresh` · `/logout`   | register/login return token pair   |
| Profile     | `GET` / `PUT /api/profiles/me`                                  | `PUT` accepts `latitude`/`longitude` |
| Catalog     | `GET /api/sports` · `GET /api/sports/{id}/skills`               | public                             |
| My sports   | `GET` / `PUT /api/sports/mine` · `DELETE /api/sports/mine/{sportId}` |                               |
| Suggestions | `POST /api/sports/suggestions` · `GET /api/sports/suggestions/mine` |                                |
| Admin queue | `GET /api/admin/suggestions` · `POST …/{id}/approve` · `…/{id}/reject` | `ADMIN` only                 |
| Discovery   | `GET /api/discovery?radiusKm=50&limit=30`                       | the swipe feed                     |
| Swipe       | `POST /api/swipes`                                              | returns `matched=true` on a match  |
| Matches     | `GET /api/matches`                                              |                                    |
| Chat (REST) | `GET /api/conversations` · `GET`/`POST …/{id}/messages`         |                                    |
| Chat (WS)   | STOMP `/ws` → send `/app/conversations/{id}/send`, subscribe `/user/queue/messages` |       |

The full, always-up-to-date contract is the live Swagger UI / OpenAPI document.

---

## Testing the backend without a frontend

The easiest path is **Swagger UI** (`/swagger-ui.html`):

1. `POST /api/auth/register` → copy the `accessToken` from the response.
2. Click **Authorize 🔒** (top-right), paste the token — Swagger adds the `Bearer ` prefix and
   sends it on every call.
3. All protected endpoints are now callable from the page.

**Producing a real match** (the headline feature) requires two users:

1. Register **two** users; copy each `accessToken`.
2. For each: `PUT /api/profiles/me` with nearby `latitude`/`longitude`, and
   `PUT /api/sports/mine` with the **same** sport (e.g. Gym
   `10000000-0000-0000-0000-000000000001`).
3. As user A: `GET /api/discovery` → copy user B's `userId` from a candidate (don't hand-type
   UUIDs).
4. A `LIKE`s B, then B `LIKE`s A (`POST /api/swipes`) → the second call returns
   `matched: true` with a `matchId` and `conversationId`.
5. Exchange messages via `GET`/`POST /api/conversations/{id}/messages`, or over STOMP.

> STOMP chat can't be exercised with `curl`. Use a STOMP-capable client (Postman's WebSocket
> request, or a small `@stomp/stompjs` script) and put `Authorization: Bearer <token>` on the
> CONNECT frame.

---

## Project layout

```
TempoUp/
├── docker-compose.yml                 # PostgreSQL + PostGIS for local dev
├── pom.xml                            # Maven build (Spring Boot parent)
├── mvnw / mvnw.cmd / .mvn/            # Maven Wrapper (no system Maven needed)
├── README.md                         # this file
├── TECH_STACK.md                     # technology-by-technology rationale
└── src/main/
    ├── java/com/tempoup/api/…         # feature packages (see Architecture)
    └── resources/
        ├── application.yml            # configuration
        └── db/migration/             # Flyway migrations (schema + seed)
```

---

## Production notes / next steps
- **Secrets:** move `JWT_SECRET` (and DB credentials) to a real secret store; never ship the
  dev default. Rotate the seeded admin password immediately.
- **WebSocket scale:** the in-memory STOMP broker is single-instance only. To run more than one
  app instance, switch to a RabbitMQ/Redis STOMP relay so messages fan out across nodes.
- **Bad-input mapping:** consider mapping malformed request bodies / type-mismatches to `400`
  instead of the generic `500`.
- **CORS:** the defaults target Expo/React-Native dev origins — tighten `CORS_ORIGINS` for
  production.
