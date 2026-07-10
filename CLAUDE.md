# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This repo is pre-implementation. `backend/` and `frontend/` are empty placeholder directories — no code, build tooling, or tests exist yet. The only real content is the planning docs in `docs/`. There are no build/lint/test commands to run because there is nothing to build yet; once a Spring Boot project and Flutter project are scaffolded into `backend/` and `frontend/`, this file should be updated with their actual commands (Gradle wrapper tasks, `flutter test`, etc.).

Do not invent commands or architecture that aren't backed by what's actually in the repo — check `docs/nomlog_devplan_v0.1.md` for the current confirmed plan before assuming a structure exists.

## What this project is

"뭐먹었지" (working title) is a personal food-logging app: log what you ate with minimal friction, review it as monthly/yearly stats, optionally share a stat/record as an image card with a small group of friends. It is explicitly **not** a calorie/diet-tracking app — the differentiator is "record for fun/reflection," not "manage/lose weight." Full rationale, competitive research, and scope are in `docs/nomlog_PRD_v0.1.md`.

## Confirmed stack (per devplan)

- Backend: Spring Boot + PostgreSQL, Gradle. Auth via Google OAuth2 (Spring Security OAuth2 Client) issuing JWTs.
- Frontend: Flutter (mobile-first; iOS/Android both possible via TestFlight/Play internal test track).
- Deployment target: Cloudtype (or similar) for backend; direct APK / TestFlight for client distribution — no app store listing in this sprint.
- Image storage: S3-compatible or Supabase Storage via presigned upload URLs (not yet chosen).

## Planned architecture (from `docs/nomlog_devplan_v0.1.md`)

The core domain is intentionally small: two entities, `User` and `Record` (food_name, category, image_url, eaten_at). Statistics are **not** a separate stored/aggregated table — monthly/yearly stats are computed as aggregation queries over `Record` directly (`GET /stats/monthly`, `GET /stats/yearly`). Preserve this "no separate stats table" design if implementing stats — it's a deliberate simplicity choice, not an oversight.

Planned API surface:
- `GET /health` — deploy sanity check
- `POST /records`, `GET /records?date=`, `PATCH /records/{id}`, `DELETE /records/{id}`
- `GET /records/autocomplete?q=` — prefix search over the user's own past food_name/category values, used to reduce input friction
- `GET /stats/monthly?year=&month=`, `GET /stats/yearly?year=`
- Presigned URL endpoint for image upload

**Design principle that overrides feature convenience**: logging a record must be completable in 1–2 taps / under ~3 seconds. This came directly from competitive research (input friction is the #1 churn driver in this category) and should be weighed against any feature addition that touches the record-creation flow.

**Explicitly out of scope for this sprint** (do not build unless asked): AI food-image recognition, Instagram/delivery-app integration, friend-adding/social feed, region/age comparison stats, Redis/Nginx/MQTT infra, paid app store registration. The social/friend-sharing mechanism (feed-style vs. synchronized time-slot reveal, see PRD §6.1) is a deliberately open question pending user testing feedback — don't pick one and implement it without being asked.

## Repo layout

- `docs/` — PRD (`nomlog_PRD_v0.1.md`) and execution plan (`nomlog_devplan_v0.1.md`), both in Korean. Treat the devplan's checkbox list as the actual backlog/task granularity Claude Code should expect to be given (e.g. "구글 OAuth 연동해줘" rather than "1주차 백엔드 다 해줘").
- `backend/`, `frontend/` — empty, awaiting scaffolding.
- `.claude/skills/db-mocker/` — currently an empty skill stub.
