# URL Shortener with Click Analytics

A backend service that shortens URLs and tracks click analytics (count, timestamp, referrer),
built with Java and Spring Boot.

## Why this project

Most URL-shortener tutorials stop at "generate a code, save it, redirect." This one goes a step
further by logging every click as its own event, so it can answer real analytics questions
(when was this clicked, how many times, from where) — the kind of design decision that comes
up in actual system design interviews.

## Tech stack

- **Java 17 / Spring Boot 3** — REST API
- **Spring Data JPA** — persistence layer
- **H2 (in-memory)** — zero-setup local database (swappable for PostgreSQL — see Roadmap)
- **JUnit 5 + Mockito** — unit tests for the service layer
- **Maven** — build tool

## How it works

1. `POST /api/urls` with a JSON body `{ "originalUrl": "https://..." }` generates a random
   7-character Base62 short code, stores the mapping, and returns the short URL.
2. `GET /{code}` looks up the original URL, logs a `ClickEvent` (timestamp + referrer), increments
   a denormalized `clickCount` on the URL record for fast reads, and issues an HTTP 302 redirect.
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

## Running locally

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. No database setup needed — H2 runs in memory.

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

You can also browse the H2 database directly at `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:urlshortener`, user: `sa`, no password).

## Running tests

```bash
mvn test
```

## Roadmap

- [ ] Swap H2 for PostgreSQL (persistent storage)
- [ ] Add Redis caching for frequently-accessed short codes
- [ ] Containerize with Docker + docker-compose (app + Postgres + Redis)
- [ ] Add a GitHub Actions workflow to run tests on every push
- [ ] Add rate limiting on URL creation
