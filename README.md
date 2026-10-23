# URL Shortener

Java/Spring Boot service that turns a long HTTPS URL into a short link, redirects visitors, and reports click analytics.

This is a working prototype for an AI-assisted software engineering interview: the engineer owns design, sequencing, and correctness; AI is used inside tasks.

## What it does

| Method | Path | Auth | Result |
| --- | --- | --- | --- |
| `POST` | `/api/v1/links` | Bearer API key | `201` create a short link |
| `GET` | `/{code}` | Public | `302` redirect |
| `GET` | `/api/v1/links/{code}` | Bearer | `200` metadata |
| `GET` | `/api/v1/links/{code}/stats` | Bearer | `200` click totals and last 30 days |
| `DELETE` | `/api/v1/links/{code}` | Bearer | `204` soft delete |
| `GET` | `/health` | Public | process liveness |
| `GET` | `/ready` | Public | Postgres readiness |

Redirects are always `302` with `Cache-Control: private, no-store`. A `301` would be cached by browsers, which under-counts clicks and makes takedown ineffective.

## Prerequisites

- Java 21
- Docker with Compose v2 (PostgreSQL 16 and Redis 7)
- Maven Wrapper is in the repo (`./mvnw`); you do not need a global Maven install

## Setup

```bash
# 1. Start Postgres and Redis
docker compose up -d
docker compose ps

# 2. Copy env values. Set APP_API_KEY to a long random secret.
cp .env.example .env

# 3. Run tests (starts Postgres/Redis via Testcontainers)
./mvnw test

# 4. Run the service
export $(grep -v '^#' .env | xargs)
./mvnw spring-boot:run
```

Health check:

```bash
curl -i http://localhost:8080/health
```

Create a link:

```bash
curl -i http://localhost:8080/api/v1/links \
  -H "Authorization: Bearer $APP_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://example.com/a"}'
```

Follow it (must be `302`, never `301`):

```bash
curl -sS -D - -o /dev/null http://localhost:8080/$CODE
```

Optional custom alias (brownfield feature):

```bash
curl -i http://localhost:8080/api/v1/links \
  -H "Authorization: Bearer $APP_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"originalUrl":"https://example.com/launch","alias":"launch-day"}'
```

## Design in one paragraph

PostgreSQL is the source of truth. Redis is an accelerator: redirect cache and rate limits. If Redis is down, redirects still work (slower, rate limits fail open) and creates fail closed. The service never fetches a destination URL. Click rows are written after the `302` is decided, on a bounded executor that drops oldest events under overload.

## Documentation

| Document | Contents |
| --- | --- |
| [docs/requirements.md](docs/requirements.md) | Ambiguities, decisions, API contract, non-goals |
| [docs/architecture.md](docs/architecture.md) | Components, flows, data model, failure modes |
| [docs/scenarios.md](docs/scenarios.md) | Greenfield, brownfield, and ambiguous scenarios |
| [docs/engineering-summary.md](docs/engineering-summary.md) | Plan, artifacts, risks, assumptions, limitations |
| [docs/ai-traceability.md](docs/ai-traceability.md) | What AI generated, what was edited, what was rejected |
| [openapi.yaml](openapi.yaml) | HTTP contract |

## Quality gates

```bash
./mvnw test
```

Unit tests cover destination policy, alias rules, expiry/delete guards, and code generation. `ApiFlowIT` and context-load tests need Docker.
