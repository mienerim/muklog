# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

`backend/` has a Spring Boot project scaffolded (see Backend section below). `frontend/` is still an empty placeholder — no Flutter project exists yet. There are no frontend commands to run until it's scaffolded; update this file once it is.

Do not invent commands or architecture that aren't backed by what's actually in the repo — check `docs/nomlog_devplan_v0.1.md` for the current confirmed plan before assuming a structure exists.

## Backend

Spring Boot 4.1.0, Java 21, Gradle. Group `com.nomlog`, artifact `backend`, base package `com.nomlog.backend`. Dependencies: Spring Web, Spring Data JPA, PostgreSQL driver, Spring Security, Spring Validation, `google-api-client` (Google ID token verification), `jjwt` (app JWT issuance/parsing). No `spring-boot-starter-security-oauth2-client` — deliberately removed, see Auth below.

Commands (run from `backend/`):
- Build: `./gradlew build`
- Compile only: `./gradlew compileJava`
- Run: `./gradlew bootRun`
- Test: `./gradlew test`
- Single test: `./gradlew test --tests "com.nomlog.backend.SomeClassTest"`

`NomlogApplicationTests.contextLoads` requires a live Postgres connection (`DB_HOST` etc.) and fails without one — expected in an environment with no local Postgres, not a regression signal by itself.

Datasource, Google client id, and JWT secret/expiration are read from env vars in `application.properties` (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `GOOGLE_CLIENT_ID`, `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`) with local-dev defaults — set these when running against a real Postgres/Google app instead of editing the properties file directly. `JWT_SECRET` must be at least 32 bytes (HMAC-SHA256 requirement).

Package structure is feature-first, one package per bounded concern rather than per technical layer (no repo-wide `controller`/`service`/`repository` packages):
- `user/` — `User` entity (keyed by Google `sub` claim) + repository
- `record/` — `Record` JPA entity (food_name/category/image_url/eaten_at) + repository + `RecordService`/`RecordController` + DTOs (`RecordCreateRequest`/`RecordUpdateRequest`/`RecordResponse`/`AutocompleteField`). The Java `record` DTOs are deliberately never named bare `Record` to avoid colliding with the JPA entity of the same name — keep following that convention for any new DTO here.
- `auth/` — Google ID token verification → app JWT issuance/validation. `AuthController` (`POST /auth/google`), `AuthService`, `GoogleIdTokenService` (wraps `GoogleIdTokenVerifier`), `JwtTokenProvider` (HMAC-SHA256 via jjwt), `JwtAuthenticationFilter` (reads `Authorization: Bearer`, populates `SecurityContextHolder` with the resolved `Long` user id as principal — no `UserDetailsService`/`AuthenticationManager` involved), `CurrentUser.id()` static helper for controllers to read the authenticated user id.
- `stats/` — reserved for week-3 monthly/yearly aggregation endpoints; intentionally has no entity, since stats are computed via aggregation queries over `Record`, not stored
- `health/` — `/health` liveness endpoint (publicly permitted)
- `config/` — `SecurityConfig` (stateless JWT-based REST API — CSRF disabled, no sessions, `/health` and `/auth/**` permitted, everything else requires the JWT filter's authentication) and `JpaAuditingConfig` (`createdAt`/`updatedAt` auditing)
- `common/` — `BaseTimeEntity` (shared auditing fields, extend this from any new entity) and `GlobalExceptionHandler`/`ErrorResponse` (flat `@RestControllerAdvice`, no exception hierarchy — one `@ExceptionHandler` per exception type mapped straight to an HTTP status)

**Auth flow**: the client is a Flutter *mobile* app using Google Sign-In on-device to obtain a Google ID token — NOT a server-side OAuth2 redirect flow. `POST /auth/google` verifies that ID token server-side (audience = `app.google.client-id`), finds-or-creates the `User` by `googleSub`, and returns an app-issued JWT (`accessToken`, ~7 day expiry, no refresh token yet — that's a deliberately deferred follow-up). Every other endpoint authenticates via `Authorization: Bearer <jwt>` through `JwtAuthenticationFilter`. Do not reintroduce `.oauth2Login()` or the `oauth2-client` starter — that's for browser redirect flows and doesn't fit this client.

**Record ownership**: `RecordService` enforces that a record's owner matches the authenticated user id on every read/update/delete; a record that exists but belongs to someone else returns 404 (`RecordNotFoundException`), same as one that doesn't exist at all — this was a deliberate simplicity choice over 403, not an oversight.

Presigned-URL image upload and stats endpoints are not implemented yet (excluded this round — no storage backend chosen).

## What this project is

"뭐먹었지" (working title) is a personal food-logging app: log what you ate with minimal friction, review it as monthly/yearly stats, optionally share a stat/record as an image card with a small group of friends. It is explicitly **not** a calorie/diet-tracking app — the differentiator is "record for fun/reflection," not "manage/lose weight." Full rationale, competitive research, and scope are in `docs/nomlog_PRD_v0.1.md`.

## Confirmed stack (per devplan)

- Backend: Spring Boot + PostgreSQL, Gradle. Auth via on-device Google Sign-In → server-side ID token verification → app-issued JWT (see Backend section above for the concrete implementation).
- Frontend: Flutter (mobile-first; iOS/Android both possible via TestFlight/Play internal test track).
- Deployment target: Cloudtype (or similar) for backend; direct APK / TestFlight for client distribution — no app store listing in this sprint.
- Image storage: S3-compatible or Supabase Storage via presigned upload URLs (not yet chosen).

## Planned architecture (from `docs/nomlog_devplan_v0.1.md`)

The core domain is intentionally small: two entities, `User` and `Record` (food_name, category, image_url, eaten_at). Statistics are **not** a separate stored/aggregated table — monthly/yearly stats are computed as aggregation queries over `Record` directly (`GET /stats/monthly`, `GET /stats/yearly`). Preserve this "no separate stats table" design if implementing stats — it's a deliberate simplicity choice, not an oversight.

API surface:
- `GET /health` — deploy sanity check (implemented)
- `POST /auth/google` — Google ID token → app JWT (implemented)
- `POST /records`, `GET /records?date=`, `PATCH /records/{id}`, `DELETE /records/{id}` (implemented)
- `GET /records/autocomplete?field=FOOD_NAME|CATEGORY&q=` — prefix search over the authenticated user's own past food_name/category values, most-recent-first, capped at 10 results (implemented)
- `GET /stats/monthly?year=&month=`, `GET /stats/yearly?year=` (not yet implemented — week 3)
- Presigned URL endpoint for image upload (not yet implemented — needs a storage backend decision first)

**Design principle that overrides feature convenience**: logging a record must be completable in 1–2 taps / under ~3 seconds. This came directly from competitive research (input friction is the #1 churn driver in this category) and should be weighed against any feature addition that touches the record-creation flow.

**Explicitly out of scope for this sprint** (do not build unless asked): AI food-image recognition, Instagram/delivery-app integration, friend-adding/social feed, region/age comparison stats, Redis/Nginx/MQTT infra, paid app store registration. The social/friend-sharing mechanism (feed-style vs. synchronized time-slot reveal, see PRD §6.1) is a deliberately open question pending user testing feedback — don't pick one and implement it without being asked.

## Repo layout

- `docs/` — PRD (`nomlog_PRD_v0.1.md`) and execution plan (`nomlog_devplan_v0.1.md`), both in Korean. Treat the devplan's checkbox list as the actual backlog/task granularity Claude Code should expect to be given (e.g. "구글 OAuth 연동해줘" rather than "1주차 백엔드 다 해줘").
- `backend/` — Spring Boot project, see Backend section above.
- `frontend/` — empty, awaiting Flutter scaffolding.
- `.claude/skills/db-mocker/` — currently an empty skill stub.
