# Three scenarios

Each scenario is a real slice of this repository, not a slide.

## 1. Greenfield — core shortener

**Prompt interpreted:** “Build a URL shortener with core APIs.”

**Decomposition**

1. Freeze the contract (redirect `302`, API keys, https-only destinations)
2. Schema for API keys, links, idempotency
3. Create + public redirect
4. Unit tests for policy and code generation; API test for create → 302

**What landed**

- `POST /api/v1/links`
- `GET /{code}`
- Flyway `V1__init.sql`
- Destination policy that never fetches the target

**Validation**

- `DestinationPolicyTest` rejects `http`, `javascript:`, localhost, private IPv4, credentials
- `ApiFlowIT` creates a link and follows a `302` with `private, no-store`

**Risk controlled:** open redirect and SSRF by refusing non-https and never resolving DNS.

## 2. Brownfield — custom aliases and analytics

**Prompt interpreted:** enhance an existing service; do not rewrite it.

**Inherited code:** generated 8-character codes, create + redirect already shipping.

**Impacted modules**

- `V2__custom_aliases.sql` (length check + `custom_alias`)
- `LinkService` alias branch vs generated-code retry
- `AliasPolicy`
- `StatsService` + `click_events`
- OpenAPI and README

**Execution**

- Migration first, then API field, then tests
- Alias conflict is `409`; generated collision still retries
- Stats read Postgres, not Redis, so they stay correct after a cache flush

**Validation**

- `AliasPolicyTest`
- `ApiFlowIT.customAliasIsAccepted`
- Stats query bounded to 30 days

**Risk controlled:** alias must not collide with `health` / `ready` / `api`. Soft delete keeps click history.

## 3. Ambiguous — “make it reliable”

**The prompt does not define reliability.** Written interpretation before code:

Reliability for this product means: links can expire, abuse is rate-limited, process vs dependency health are separate, deletes actually stop redirects, and retries do not create duplicate links.

**Decomposition**

1. Expiry checked on every redirect, including cache hits
2. Rate limits: create fail-closed, redirect fail-open
3. `/health` vs `/ready`
4. Idempotency-Key
5. Delete-then-evict; eviction failure → `503`

**Validation**

- `LinkGuardsTest` for expiry and deletion
- Ready probe only talks to Postgres
- Idempotency conflict path in `LinkService`

**Trade-off accepted:** a crash can drop queued click rows. Redirects still succeed. A broker would make clicks durable and is listed as future work.
