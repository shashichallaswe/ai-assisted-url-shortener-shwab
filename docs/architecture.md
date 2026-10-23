# Architecture

## Components

```
                    Fastify is not used. This is Spring Boot 4 / Java 21.

  client ──► Spring MVC
               api/            HTTP, filters, error shape
               security/      destination policy, API keys, rate limits
               service/        business rules
               persistence/   Spring Data JPA
               cache/         Redis behind an interface
               domain/        entities and guards
               support/       hashing, errors
                    │                │
                    ▼                ▼
              PostgreSQL 16      Redis 7
              source of truth    cache, rate limits
```

Dependencies point one way: a controller does not query, a service does not set HTTP status, a repository does not decide policy.

## Create flow

`POST /api/v1/links`

1. Bearer API key → SHA-256 lookup; missing/invalid/revoked → `401`
2. Rate limit per API key (fail closed if Redis is down)
3. Validate body; destination policy (https only, no credentials, no private/localhost literals)
4. Optional alias; otherwise generate 8-character code from a readable alphabet
5. Unique `code` is enforced by Postgres; generated-code collisions retry 3 times then `503`
6. Optional idempotency key recorded in the same transaction
7. `201` with `code`, `shortUrl`, `originalUrl`, `expiresAt`, `customAlias`

Create does not warm the redirect cache. Most links are never clicked.

## Redirect flow

`GET /{code}`

1. Shape check. Anything outside `[A-Za-z0-9_-]{4,32}` is `404` with no I/O
2. Rate limit per hashed IP; Redis down → fail open
3. Cache lookup. Hit: re-check expiry and deletion against the clock
4. Miss: Postgres lookup. Missing codes get a short negative cache
5. Respond `302` + `Cache-Control: private, no-store`
6. After the decision, enqueue a click row. Failures are logged and dropped

## Analytics

Postgres is the source of truth for clicks. The stats endpoint counts rows and groups the last 30 UTC days. Redis is not used as a counter, so a Redis outage cannot desynchronize totals.

## Custom aliases (brownfield)

Flyway `V1` required 8-character codes. `V2` relaxed length to 4–32 and added `custom_alias`. The create API gained an optional `alias` field. Generated-code collision retry still applies; alias conflicts are `409` with no retry.

## Cache invalidation

Delete commits `deleted_at`, then evicts the cache. If eviction fails, the API returns `503` so the caller retries. A successful delete that left a live cache entry would keep redirecting.

Expiry needs no invalidation event: cache TTL is capped at remaining lifetime, and every hit re-checks `expires_at`.

## Failure modes

| Failure | Behaviour |
| --- | --- |
| Redis down | Redirects use Postgres; redirect rate limits fail open; creates fail closed |
| Redis down during delete | `503` |
| Postgres down | `/ready` is `503`; writes and cache-miss redirects fail |
| Click executor full | Oldest click tasks dropped |
| Code collision 3 times | `503` |

## Scale-up path (not built)

Read replica for stats, Redis Cluster, a click stream instead of an in-process executor, pre-allocated code blocks. Named so the design has a next step without implementing it in a prototype.
