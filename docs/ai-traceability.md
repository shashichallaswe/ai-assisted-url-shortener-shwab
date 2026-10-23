# AI traceability

AI assisted inside tasks. The engineer owns correctness, sequencing, and whether output ships.

| Date | Area | Generated | Edited | Rejected | Why rejected | Gates |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-08-23 | Scaffold | Spring Boot 4 Maven tree from start.spring.io | Removed H2; pinned Postgres 16 / Redis 7; Java 21 | Spring Boot 3.x starter | start.spring.io no longer offers Boot 3; Boot 4 is the current default | compile |
| 2026-08-23 | Requirements | Ambiguity table and API surface | Tightened 404 parity for expired/deleted; hashed IPs; base URL from config | `301` redirects; fetching destinations; UI; geo analytics | `301` breaks analytics/takedown; fetching is SSRF; UI not requested | docs |
| 2026-08-23 | Domain/policy | Destination and alias policies | Private IPv4 ranges; no DNS | DNS lookup of destinations; allowlist-by-substring `https` | SSRF; substring checks are bypassable | unit tests |
| 2026-08-23 | Create/redirect | Controllers, services, JPA entities, Flyway | Collision retry vs alias 409; 302 + no-store; fail-open redirect limiter | SELECT-before-insert uniqueness; caching API keys; warming cache on create | TOCTOU; revocation lag; most links never clicked | unit + IT |
| 2026-08-23 | Analytics | Click rows + stats query | 30-day bound; async persist that cannot fail a 302 | Redis as source of click totals | Desync after Redis flush | IT |
| 2026-08-23 | Brownfield alias | V2 migration + alias field | Reserved names; 4–32 charset | Treating alias collision as retryable | User chose the string; they need a 409 | unit + IT |

Human sign-off still required for: destination policy, credential handling, cache invalidation on delete, and rate-limit fail-open/fail-closed choices.
