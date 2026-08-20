# URL Shortener with Click Analytics

A backend service that shortens URLs and tracks click analytics (count, timestamp, referrer),
built with Java and Spring Boot. Fully containerized with Docker — app, PostgreSQL, and Redis
all run together via a single `docker compose up`.

## Why this project

Most URL-shortener tutorials stop at "generate a code, save it, redirect." This one goes a step
further by logging every click as its own event, so it can answer real analytics questions
(when was this clicked, how many times, from where) — the kind of design decision that comes
up in actual system design interviews. It also layers in caching, containerization, CI, and
rate limiting, so the end-to-end story covers persistence, performance, reproducible deployment,
and abuse protection.

## Tech stack

- **Java 17 / Spring Boot 3** — REST API
- **Spring Data JPA** — persistence layer
- **PostgreSQL** — persistent relational database
- **Redis** — caching layer for short-code lookups, and backing store for rate limiting
- **H2 (in-memory)** — used only for fast unit tests, not for running the app
- **Docker / Docker Compose** — containerized app + Postgres + Redis, one-command startup
- **GitHub Actions** — runs the test suite on every push and pull request to `main`
- **JUnit 5 + Mockito** — unit tests for the service layer
- **Maven** — build tool

## How it works

1. `POST /api/urls` with a JSON body `{ "originalUrl": "https://..." }` generates a random
   7-character Base62 short code, stores the mapping, and returns the short URL.
2. `GET /{code}` looks up the original URL (via the Redis-backed cache), logs a `ClickEvent`
   (timestamp + referrer), increments a denormalized `clickCount` on the URL record for fast
   reads, and issues an HTTP 302 redirect.
3. `GET /api/urls/{code}/analytics` returns the full click history for a given short code.

### Design decisions worth knowing for an interview

- **Random codes, not sequential IDs.** Sequential IDs (1, 2, 3...) are guessable and leak how
  many links exist. Random Base62 codes avoid that, at the cost of needing a collision check
  (handled by retrying generation until a unique code is found — collisions are rare at 7
  characters from a 62-character alphabet, ~3.5 trillion possible codes).
- **Click count is denormalized.** Storing a running `clickCount` on the `ShortUrl` row means
  reading a URL's popularity doesn't require aggregating the full `ClickEvent` table every time.
  The full event log still exists separately for detailed analytics.
- **Both writes happen in one `@Transactional` method**, so the click count and the click log
  can't drift out of sync if one write succeeds and the other fails.
- **Redis caches short-code → original-URL lookups** (`ShortUrlCacheService`, `@Cacheable`),
  since redirects are the hottest read path and don't need to hit Postgres on every request.
  Cache entries expire after a TTL (`spring.cache.redis.time-to-live`) rather than persisting
  indefinitely, so stale entries fall off automatically.
- **Postgres runs in a named Docker volume**, so data survives container restarts — verified by
  stopping and restarting the full stack and confirming a previously created short URL was still
  present with its original `createdAt` timestamp.
- **Tests run against H2 with caching disabled** (`spring.cache.type=none` in the test config),
  so the test suite doesn't depend on a real Postgres or Redis instance being available.
- **Rate limiting on `POST /api/urls` is Redis-backed, not in-memory.** A per-client fixed-window
  counter (`RateLimiterService`) uses Redis `INCR` + `EXPIRE` so the limit is enforced correctly
  even if the app scales to multiple instances behind a load balancer — an in-memory counter
  would let each instance track its own count separately, effectively multiplying the limit.
  Configurable via `rate-limit.max-requests` / `rate-limit.window-seconds`; requests over the
  limit get an HTTP `429 Too Many Requests`. Only URL creation is limited — redirects and
  analytics reads stay unrestricted. If Redis is unreachable, the limiter fails open (allows the
  request) rather than blocking traffic because of a caching-layer outage.

## Running with Docker (recommended)

This starts the app, PostgreSQL, and Redis together — no local database or cache install needed.

```bash
docker compose up --build
```

The app will be available at `http://localhost:8080`. On later runs, once the image is already
built, you can just use `docker compose up`.

> **Note:** if you also have PostgreSQL or Redis running locally on ports `5432` / `6379`
> (e.g. as a Windows service, or a local Memurai install), stop those first — Docker Compose
> will try to bind its own Postgres and Redis containers to the same ports.

To stop everything:

```bash
# Ctrl+C if running in the foreground, or:
docker compose down
```

Postgres data is stored in a named volume (`postgres_data`), so it persists across
`docker compose down` / `up` cycles — only `docker compose down -v` will wipe it.

### Try it

```bash
# Create a short URL
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://github.com"}'

# Use the returned shortCode to test the redirect
curl -i http://localhost:8080/<shortCode>

# View click analytics
curl http://localhost:8080/api/urls/<shortCode>/analytics
```

`POST /api/urls` is rate-limited per client IP (default: 10 requests per 60 seconds — see
`rate-limit.max-requests` / `rate-limit.window-seconds` in `application.properties`). Exceeding
it returns HTTP `429 Too Many Requests`.

## Running locally without Docker

Requires a local PostgreSQL server (database `urlshortener`, user/password `postgres`/`postgres`
by default — see `src/main/resources/application.properties`) and a local Redis server.

```bash
mvn spring-boot:run
```

Hibernate creates the `short_urls` and `click_events` tables automatically on first startup,
based on the `@Entity` classes.

## Running tests

```bash
mvn test
```

Tests use H2 in-memory storage and disable caching, so they don't need Postgres or Redis running.

## Continuous Integration

A GitHub Actions workflow (`.github/workflows/ci.yml`) runs `mvn test` automatically on every
push and pull request to `main`, using JDK 17 with Maven dependency caching.

## Roadmap

- [x] Swap H2 for PostgreSQL (persistent storage)
- [x] Add Redis caching for frequently-accessed short codes
- [x] Containerize with Docker + docker-compose (app + Postgres + Redis)
- [x] Add a GitHub Actions workflow to run tests on every push
- [x] Add rate limiting on URL creation