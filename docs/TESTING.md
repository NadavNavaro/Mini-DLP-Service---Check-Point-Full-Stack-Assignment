# Tests

| Suite | Cases | Scope |
|---|---:|---|
| ApiTest | 83 | CRUD, validation, thresholds, OR logic, Unicode, optional descriptions and explanations on H2 |
| PostgresApiIT | 83 | The same API contract on real PostgreSQL via Testcontainers |
| KeywordMatcherTest | 63 | Whole-word boundaries, literals, case, repeats, Unicode, phrases and fixed-seed inputs |
| ScanQueryTest | 2 | One fetch query for 20 rules, with correct counts, including detailed scans |
| PostgresIT | 3 | Database constraints, lifecycle and independent concurrent scans |
| **Total** | **234** | Parameterized cases count separately; PostgreSQL deliberately repeats the API contract |

`mvn test` runs H2/matcher/query tests. `mvn verify` additionally runs PostgreSQL tests, which skip if Docker is unavailable. The CI job first checks Docker availability.

The Postman collection exercises the running Docker stack: 46 requests and 71 assertions, including missing/null descriptions and unchanged scan responses. `node scripts/verify-http.mjs` verifies 120 scans in batches of eight and removes temporary resources.

Tests cover below/exact/above thresholds and all eight demo keywords. Repetitions count separately; distinct overlapping keywords also count independently, as documented in the technical reference. Matcher generated inputs use fixed seeds so failures are reproducible. Concurrency checks verify correctness, not production capacity.

Refactor regressions verify pattern reuse/eviction, independent counts with shared patterns, immutable policy snapshots after transaction closure, fresh rule edits with a warm cache, blocking policy IDs, and pagination headers while preserving array response bodies.
