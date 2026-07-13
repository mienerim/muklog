# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

`backend/` has a Spring Boot project scaffolded (see Backend section below). `frontend/` now has a Flutter project scaffolded with a working Google Sign-In → backend login flow targeting **Android** (see Frontend section below). The auth flow was recently migrated from a web-only prototype (`google_sign_in_web`/`renderButton`) to Android-native `google_sign_in` v7 — do not reintroduce the web-only path.

Do not invent commands or architecture that aren't backed by what's actually in the repo — check `docs/muklog_devplan_v0.1.md` for the current confirmed plan before assuming a structure exists.

## Backend

Spring Boot 4.1.0, Java 21, Gradle. Group `com.muklog`, artifact `backend`, base package `com.muklog.backend`. Dependencies: Spring Web, Spring Data JPA, PostgreSQL driver, Spring Security, Spring Validation, `google-api-client` (Google ID token verification), `jjwt` (app JWT issuance/parsing). No `spring-boot-starter-security-oauth2-client` — deliberately removed, see Auth below.

Commands (run from `backend/`):
- Build: `./gradlew build`
- Compile only: `./gradlew compileJava`
- Run: `./gradlew bootRun`
- Test: `./gradlew test`
- Single test: `./gradlew test --tests "com.muklog.backend.SomeClassTest"`

`MuklogApplicationTests.contextLoads` requires a live Postgres connection (`DB_HOST` etc.) and fails without one — expected in an environment with no local Postgres, not a regression signal by itself.

Datasource, Google client id, and JWT secret/expiration are read from env vars in `application.properties` (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `GOOGLE_CLIENT_ID`, `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`) with local-dev defaults — set these when running against a real Postgres/Google app instead of editing the properties file directly. `JWT_SECRET` must be at least 32 bytes (HMAC-SHA256 requirement). `GOOGLE_CLIENT_ID` is supplied locally via `backend/.env.properties` (git-ignored); `GoogleIdTokenVerifierConfig` now throws at startup if it resolves blank, so a wrong working directory fails loudly instead of returning opaque 401s.

The server listens on **port 8081** (`server.port=${SERVER_PORT:8081}` in `application.properties`) — 8080 was occupied on the dev machine, and the Flutter client targets 8081. This is pinned now, so `bootRun` no longer needs a `--server.port=8081` override.

Package structure is feature-first, one package per bounded concern rather than per technical layer (no repo-wide `controller`/`service`/`repository` packages):
- `user/` — `User` entity (keyed by Google `sub` claim) + repository
- `record/` — `Record` JPA entity (food_name/category/image_url/eaten_at) + repository + `RecordService`/`RecordController` + DTOs (`RecordCreateRequest`/`RecordUpdateRequest`/`RecordResponse`/`AutocompleteField`). The Java `record` DTOs are deliberately never named bare `Record` to avoid colliding with the JPA entity of the same name — keep following that convention for any new DTO here.
- `auth/` — Google ID token verification → app JWT issuance/validation. `AuthController` (`POST /auth/google`), `AuthService`, `GoogleIdTokenService` (wraps `GoogleIdTokenVerifier`), `JwtTokenProvider` (HMAC-SHA256 via jjwt), `JwtAuthenticationFilter` (reads `Authorization: Bearer`, populates `SecurityContextHolder` with the resolved `Long` user id as principal — no `UserDetailsService`/`AuthenticationManager` involved), `CurrentUser.id()` static helper for controllers to read the authenticated user id.
- `stats/` — reserved for week-3 monthly/yearly aggregation endpoints; intentionally has no entity, since stats are computed via aggregation queries over `Record`, not stored
- `health/` — `/health` liveness endpoint (publicly permitted)
- `config/` — `SecurityConfig` (stateless JWT-based REST API — CSRF disabled, no sessions, `/health` and `/auth/**` permitted, everything else requires the JWT filter's authentication) and `JpaAuditingConfig` (`createdAt`/`updatedAt` auditing)
- `common/` — `BaseTimeEntity` (shared auditing fields, extend this from any new entity) and `GlobalExceptionHandler`/`ErrorResponse` (flat `@RestControllerAdvice`, no exception hierarchy — one `@ExceptionHandler` per exception type mapped straight to an HTTP status)

**Auth flow**: the client is a Flutter *Android* app using Google Sign-In on-device to obtain a Google ID token — NOT a server-side OAuth2 redirect flow. `POST /auth/google` verifies that ID token server-side (audience = `app.google.client-id`), finds-or-creates the `User` by `googleSub`, and returns an app-issued JWT (`accessToken`, ~7 day expiry, no refresh token yet — that's a deliberately deferred follow-up). Every other endpoint authenticates via `Authorization: Bearer <jwt>` through `JwtAuthenticationFilter`. Do not reintroduce `.oauth2Login()` or the `oauth2-client` starter — that's for browser redirect flows and doesn't fit this client.

The **audience is the *web* OAuth client id** (`app.google.client-id` = `GOOGLE_CLIENT_ID`). This is deliberate and matches how the Android client is configured: the Flutter app passes that same web client id as `serverClientId`, so Google issues an ID token whose `aud` is the web client — which is exactly what the backend verifies. The Android OAuth client (type 1) exists only to authorize the app by package name + SHA-1; it is never referenced by id in code and is not the audience. Don't "fix" the backend to expect an android-type client id — that would break verification.

`SecurityConfig` still carries a dev-only CORS bean allowing `http://localhost:5000` — that's a leftover from the earlier Flutter-web prototype and is irrelevant to the native Android client (native HTTP isn't subject to CORS). Leave it or repurpose it only if web is revived.

**Record ownership**: `RecordService` enforces that a record's owner matches the authenticated user id on every read/update/delete; a record that exists but belongs to someone else returns 404 (`RecordNotFoundException`), same as one that doesn't exist at all — this was a deliberate simplicity choice over 403, not an oversight.

Presigned-URL image upload and stats endpoints are not implemented yet (excluded this round — no storage backend chosen).

## Frontend

Flutter app in `frontend/`, **Android-first** (iOS/web not wired up). Package/`applicationId` `com.muklog.muklog`, `minSdk 23` (required by `google_sign_in` v7's Credential Manager). Key deps: `google_sign_in: ^7.2.0`, `http`. The web-only prototype deps/imports (`google_sign_in_web`, `web_only.dart`, `renderButton`) were removed — do not bring them back.

Commands (run from `frontend/`):
- Get deps: `flutter pub get`
- Analyze: `flutter analyze`
- Run on emulator: `flutter run -d <android-emulator>`
- Debug keystore SHA-1 (needed for Google Cloud setup below): `cd android && ./gradlew signingReport`

Structure: `lib/main.dart` (launches `LoginScreen`) → `lib/screens/login_screen.dart` (a "Google로 로그인" button calls `GoogleSignIn.instance.authenticate()`; results arrive via the `authenticationEvents` stream) → `lib/services/auth_service.dart` (`AuthService` singleton). `AuthService` initializes with `serverClientId: kServerClientId` (the web client id), obtains `account.authentication.idToken`, POSTs `{"idToken": ...}` to `$kBackendBaseUrl/auth/google`, and stores the returned `accessToken` in memory (no persistence yet).

Two dev-environment constants in `auth_service.dart`:
- `kServerClientId` — the **web** OAuth client id (see Auth flow above for why it's the web one).
- `kBackendBaseUrl` — `http://10.0.2.2:8081` (Android emulator's alias for the host machine's localhost; not `localhost`). `AndroidManifest.xml` sets `usesCleartextTraffic="true"` to permit this plaintext HTTP in dev — switch to HTTPS and drop that for any real deployment.

**Required manual setup in Google Cloud Console** (can't be done in code; without it login fails with `No idToken returned` or 401):
1. Build once (`flutter run` / `./gradlew signingReport`) to generate the debug keystore, then read its `SHA1:`.
2. In the **same** Google Cloud project as the web client (`925205002034-…`), create an **Android OAuth client (type 1)** with package `com.muklog.muklog` + that debug SHA-1.
3. Keep the web client — it stays the `serverClientId` / audience.
4. If the OAuth consent screen is in "testing", add the login Google account as a test user.

## What this project is

"muklog" (working title) is a personal food-logging app: log what you ate with minimal friction, review it as monthly/yearly stats, optionally share a stat/record as an image card with a small group of friends. It is explicitly **not** a calorie/diet-tracking app — the differentiator is "record for fun/reflection," not "manage/lose weight." Full rationale, competitive research, and scope are in `docs/muklog_PRD_v0.1.md`.

## Confirmed stack (per devplan)

- Backend: Spring Boot + PostgreSQL, Gradle. Auth via on-device Google Sign-In → server-side ID token verification → app-issued JWT (see Backend section above for the concrete implementation).
- Frontend: Flutter (mobile-first; currently Android-only in the repo — iOS possible later via TestFlight).
- Deployment target: Cloudtype (or similar) for backend; direct APK / TestFlight for client distribution — no app store listing in this sprint.
- Image storage: S3-compatible or Supabase Storage via presigned upload URLs (not yet chosen).

## Planned architecture (from `docs/muklog_devplan_v0.1.md`)

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

- `docs/` — PRD (`muklog_PRD_v0.1.md`) and execution plan (`muklog_devplan_v0.1.md`), both in Korean. Treat the devplan's checkbox list as the actual backlog/task granularity Claude Code should expect to be given (e.g. "구글 OAuth 연동해줘" rather than "1주차 백엔드 다 해줘").
- `backend/` — Spring Boot project, see Backend section above.
- `frontend/` — Flutter app (Android-first), see Frontend section above.
- `.claude/skills/db-mocker/` — currently an empty skill stub.
