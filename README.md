# Smart Expense Tracker

A REST API (Java 17 + Spring Boot 3) for tracking personal expenses, with a
React (Vite) frontend on top of it.

## What's included

- **Backend** — `src/backend`: Spring Boot REST API. Expenses are kept in
  memory and mirrored to a JSON file (`expenses-data.json`) on every write, so
  data survives a restart without needing a database.
- **Frontend** — `src/frontend`: a small React app (add expense form,
  filterable table, delete, running total + per-category totals).
- **Tests** — `tests/backend`: JUnit 5 tests (unit + MockMvc integration
  tests) for the backend. See note on layout below.
- **Bonus implemented**: OpenAPI/Swagger docs, at `/swagger-ui.html` once the
  backend is running.

### A note on the `tests/` folder

The assignment's Maven project puts source under `src/` and tests under
`tests/` at the repo root. Maven's own convention is `src/test/java` *inside*
the module. To satisfy both, `src/backend/pom.xml` points Maven's
`testSourceDirectory` at `../../tests/backend/java` — so `mvn test` (run from
`src/backend`) picks up the tests from the top-level `tests/` folder exactly
as laid out below. Nothing else about the Maven layout changes.

## Requirements

- Java 17+ (JDK)
- Maven 3.8+ (a system `mvn` on your PATH — no wrapper is bundled)
- Node.js 18+ and npm (only needed for the frontend)

## Backend — install, run, test

All commands below are run from `src/backend`.

```bash
cd src/backend

# install dependencies (downloads from Maven Central)
mvn -q dependency:go-offline

# run the tests
mvn test

# start the server (http://localhost:8080)
mvn spring-boot:run
```

Once running:
- API base URL: `http://localhost:8080/api/expenses`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

To build a standalone jar instead: `mvn clean package`, then
`java -jar target/expense-tracker-api.jar`.

### API endpoints

| Method | Path                          | Description                                   |
|--------|-------------------------------|------------------------------------------------|
| POST   | `/api/expenses`                | Add an expense (`title`, `amount`, `category`, `date`) |
| GET    | `/api/expenses`                | List all expenses                             |
| GET    | `/api/expenses?category=Food`  | Filter expenses by category (case-insensitive) |
| GET    | `/api/expenses/{id}`           | Get a single expense                          |
| GET    | `/api/expenses/summary`        | Overall total + totals per category           |
| DELETE | `/api/expenses/{id}`           | Delete an expense                             |

Example request body for `POST /api/expenses`:

```json
{
  "title": "Groceries",
  "amount": 45.50,
  "category": "Food",
  "date": "2026-07-01"
}
```

`id` is always server-generated — don't send it. Validation errors and
"not found" errors come back as a consistent JSON shape:

```json
{
  "timestamp": "2026-07-01T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": ["amount: amount must be greater than zero"]
}
```

## Frontend — install and run

All commands below are run from `src/frontend`. The backend must already be
running on `localhost:8080` (CORS is pre-configured for `localhost:5173`).

```bash
cd src/frontend

# install dependencies
npm install

# start the dev server (http://localhost:5173)
npm run dev
```

Open `http://localhost:5173` in a browser. There is no separate frontend
test suite — the assignment's evaluated deliverable is the API, and the
frontend tests were left out of scope to keep the 4-hour target honest (see
`AI_NOTES.md`).

## Project layout

```
your-repo/
  README.md
  AI_NOTES.md
  src/
    backend/          # Spring Boot API (Maven project)
    frontend/          # React (Vite) app
  tests/
    backend/           # JUnit tests for the backend (see note above)
```

## Design notes

- **Storage**: an in-memory `Map` backed by a JSON file
  (`app.data-file` in `application.properties`, default
  `expenses-data.json`) written on every add/delete. Reads never touch disk;
  writes are synchronized with a `ReentrantReadWriteLock`.
- **Validation**: Bean Validation (`@NotBlank`, `@Positive`, `@NotNull`) on
  the request DTO; a `@RestControllerAdvice` turns validation failures and
  not-found errors into a single consistent JSON error shape.
- **IDs**: server-generated (`AtomicLong`), never accepted from the client.
