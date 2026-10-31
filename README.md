# Mini DLP - Check Point Take-Home Assignment

**Author: Nadav**

**Assignment: Check Point - Full Stack / DLP Team**

A single Java 21 / Spring Boot backend with PostgreSQL. It manages keyword inspection rules (Data Types), groups them into policies (Data Sets), and synchronously scans text against a selected policy.

## Run

Prerequisite: Docker with Compose v2 and Linux containers. No local Java or Maven installation is required.

From the project folder containing `docker-compose.yml`:

```sh
docker compose up --build -d --wait
```

Check `http://localhost:8080/actuator/health`; expected response: `{"status":"UP"}`.

```sh
docker compose logs -f app
docker compose down
```

The database uses a persistent named volume. **`docker compose down -v` deletes its data.** If port 8080 is occupied, stop the other service or set `APP_PORT=8081` in `.env` using `.env.example`. Update client URLs accordingly. Demo resources are created by the Postman workflow, not seeded at startup.

## Try the API

Import `postman/mini-dlp.postman_collection.json` into Postman and run the collection in order. It captures generated IDs, verifies responses and removes its demo data. Set `baseUrl` to `http://localhost:8080`; an optional local environment file is included.

Alternatively, with Node.js installed:

```sh
npx --yes newman@6 run postman/mini-dlp.postman_collection.json
```

The collection contains **46 requests and 65 assertions**, including detailed scan explanations and invalid-input cases.

| Method | Endpoint | Purpose |
|---|---|---|
| POST / GET | `/api/datatypes` | Create / list rules |
| GET / PUT / DELETE | `/api/datatypes/{id}` | Read / replace / delete a rule |
| POST / GET | `/api/datasets` | Create / list policies |
| GET / PUT / DELETE | `/api/datasets/{id}` | Read / replace / delete a policy |
| POST | `/api/scan` | Inspect text using one policy |

Create a rule:

```json
{
  "name": "Sensitive Keywords",
  "description": "Demo sensitive terms",
  "type": "keywords",
  "content": ["password", "secret", "confidential", "token", "credential", "apikey", "privatekey", "classified"],
  "threshold": 3
}
```

Create a policy using the returned rule UUID:

```json
{"name": "Outbound Policy", "data_type_ids": ["<rule-uuid>"]}
```

Scan using the policy UUID:

```json
{"text": "Secret secret password secretary", "data_set_id": "<policy-uuid>"}
```

This returns `match` with the matching rule's ID, name and `match_count: 3`. If no rule reaches its threshold, the response is exactly `{"status":"not matched"}`.

Add `"include_details": true` to a scan request for per-rule thresholds, decisions, English reasons and per-keyword counts. It also explains below-threshold results. This optional extension leaves default responses compatible with the assignment.

## Matching decisions

* **Whole words, ignoring case:** `Secret` and `secret!` match; `secretary`, `secret2` and `_secret` do not. Letters, Unicode marks, numbers and underscore are word characters; punctuation separates words.
* **Explicit assumption: count occurrences, not unique words.** `secret secret password` counts as three matches. The assignment does not explicitly define repeated-word counting.
* Keywords are trimmed and Unicode-normalized while preserving their spelling. Case-equivalent duplicates are rejected to prevent double counting. Phrases are literal, with exact internal whitespace.
* Each rule has its own positive integer threshold. **OR across rules:** any passing rule produces `match`, and all passing rules are returned.
* Empty text or an empty policy never matches. Referenced rules cannot be deleted until their policy references are removed.
* Rules and policies receive generated UUIDs. Scan text/results are not persisted, and request bodies are not logged.

## Structure and tests

Feature packages are `datatype`, `dataset`, `scan`, and shared error handling in `common`. Controllers handle HTTP, services own business logic/transactions, and repositories access PostgreSQL. Flyway creates the schema; Hibernate validates it. Scans fetch policy configuration in one query.

With JDK 21+ and Maven installed:

```sh
mvn test
mvn verify
```

`test` uses H2; `verify` additionally runs the API contract and integration tests on real PostgreSQL via Testcontainers. PostgreSQL tests skip if Docker is unavailable. Maven is optional: use `./mvnw` on Linux/macOS or `.\mvnw.cmd` on Windows.

For a concurrent HTTP check with Node.js 22+ and the service running:

```sh
node scripts/verify-http.mjs
```

It verifies 120 scans in batches of eight and removes temporary data. This is a correctness check, not a production capacity benchmark. GitHub Actions automates Java, PostgreSQL, Docker, Postman and concurrent HTTP checks.

* [Technical reference](docs/REFERENCE.md): input limits, HTTP errors, complete response examples, configuration and tradeoffs.
* [Test matrix](docs/TESTING.md) and [verified results](docs/VERIFICATION.md): 223 passing Java test cases and real HTTP checks.

This local evaluation service does not implement authentication or production deployment controls.
