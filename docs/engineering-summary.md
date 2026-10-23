# Engineering summary

## Plan and rationale

Java 21 and Spring Boot 4 were chosen because that is the interview stack, and because Spring’s layering (web → service → persistence) maps cleanly onto “controllers do not query.” PostgreSQL is the system of record; Redis is optional for correctness except on delete invalidation. Analytics are SQL aggregates, not Redis counters, so a cache flush cannot lie about totals.

Work was sequenced as greenfield (create + redirect), then brownfield (aliases + stats), then reliability (expiry, rate limits, probes, idempotency). That matches the assignment’s three scenario types.

## Artifacts

- Runnable Spring Boot service (`./mvnw spring-boot:run`)
- Docker Compose for Postgres 16 and Redis 7
- Flyway migrations `V1` (core) and `V2` (aliases)
- Unit tests and Testcontainers API tests
- OpenAPI, architecture, requirements, scenario write-ups, AI traceability

## Risks, trade-offs, validation

| Risk | Mitigation | Residual |
| --- | --- | --- |
| Open redirect | https-only, no credentials, 302 + no-store | Public hostnames that later point at private IPs (no DNS at validate time, by design) |
| SSRF | Never fetch or resolve destinations | Same DNS limitation |
| Abuse | API keys; create rate limit fail-closed; redirect fail-open | Redirect limiter is best-effort |
| Stale cache after delete | Evict after commit; `503` if eviction fails | Process crash between commit and eviction, bounded by TTL |
| Click loss | Bounded async executor, discard oldest | Hard crash loses queued rows |
| Code collision | DB unique constraint + 3 retries | `503` if occupancy is far beyond prototype scale |

## Assumptions

- Single region, moderate volume
- One demo API key seeded from `APP_API_KEY`
- Docker available for integration tests
- No PII stored: IPs hashed with a salt before insert; API keys stored as SHA-256

## Limitations

- Not a key-management system (no rotation UI, no per-team keys beyond the table)
- No click retention sweeper
- No multi-region failover
- Integration tests require Docker
- Redis optional for reads, required for a trustworthy delete
- Prototype; not production-hardened (no WAF, no multi-AZ, no audit SIEM)
