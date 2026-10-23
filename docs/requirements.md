# Requirements and decisions

The assignment asks for a URL shortener with core APIs, analytics, and reliability, without specifying redirect semantics, authentication, or analytics grain. Those gaps are closed here so implementation does not re-negotiate them.

## Problem

Build a backend service that:

1. Turns a long HTTPS URL into a short code
2. Redirects visitors from that code to the destination
3. Reports how often each link is used
4. Stays safe and available under abuse

It is consumed over HTTP by other software, not by a person in a browser.

## Ambiguities and the interpretation chosen

| Ambiguity | Choice | Why |
| --- | --- | --- |
| Redirect status code | `302 Found` plus `Cache-Control: private, no-store` | `301` is cached by browsers. The second click never hits the service, so analytics under-count and a takedown cannot take effect |
| Who may create links | API key on writes and admin reads; redirect stays public | An unauthenticated create endpoint is an open-redirect factory |
| Analytics grain | One row per click; stats endpoint aggregates last 30 days | Useful without building a warehouse |
| Reliability | Expiry, rate limits, health/ready, cache invalidation on delete, idempotent creates | Those are the failure modes a shortener actually has |
| Custom aliases | Not in the first slice; added as a brownfield change | Gives a real change against inherited schema and code |
| UI | None | Not requested; time is better spent on API quality and tests |
| Destination checks | Structural only; never fetch or DNS-resolve | Fetching a user-supplied URL is SSRF |

## API contract

JSON is camelCase. Errors are:

```json
{ "error": { "code": "VALIDATION_ERROR", "message": "...", "details": [] } }
```

| Method | Path | Auth | Success | Failures |
| --- | --- | --- | --- | --- |
| `POST /api/v1/links` | Bearer | `201` | `400`, `401`, `409`, `429` |
| `GET /{code}` | Public | `302` | `404`, `429` |
| `GET /api/v1/links/{code}` | Bearer | `200` | `401`, `404` |
| `GET /api/v1/links/{code}/stats` | Bearer | `200` | `401`, `404` |
| `DELETE /api/v1/links/{code}` | Bearer | `204` | `401`, `404`, `503` if cache invalidation fails |
| `GET /health` | Public | `200` | never checks dependencies |
| `GET /ready` | Public | `200` | `503` if Postgres is down |

Create body: `{ "originalUrl": "https://...", "expiresAt": "...", "alias": "optional" }`.

Optional `Idempotency-Key`: same key + same body returns the original `201`; same key + different body is `409`.

Expired, deleted, and unknown codes are all `404` to anonymous callers.

## Out of scope

- Web UI, accounts, custom domains
- QR codes, geo/device analytics
- Fetching or previewing destinations
- `301` redirects
- Editing a destination after create
- Click retention policy (prototype keeps rows indefinitely)

## Non-functional assumptions

- Single region, single writer, tens of requests per second locally
- Redirect path does no write on the request thread
- Redis may be cold, stale, or gone; Postgres is authoritative
- Click loss under overload is better than failing a redirect
