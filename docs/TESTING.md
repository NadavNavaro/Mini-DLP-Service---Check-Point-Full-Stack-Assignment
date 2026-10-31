# Test matrix

Tests are grouped by behavior, not only by method. Parameterized cases are counted separately by JUnit. The PostgreSQL API suite deliberately repeats the same contract to catch differences between H2 compatibility mode and the deployed database.

| Suite | Scope |
|---|---|
| `KeywordMatcherTest` | Word-boundary matrix, case, Hebrew, combining marks, normalization, phrases, literal regex characters, overlapping distinct keywords, empty input and 30 fixed random seeds with 200 known tokens each |
| `ApiTest` | CRUD lifecycle, generated IDs, Location headers, integer thresholds, 23 invalid rule payloads, a 12-case eight-keyword matrix on both databases, invalid policies, request size boundaries, pagination, errors, policy membership, OR logic, exact threshold, shared rule updates and failed-update rollback |
| `ScanQueryTest` | One SQL select for a policy with 20 rules and multiple keywords; correct counts and no duplicate detections from collection fetching |
| `PostgresApiIT` | Inherits every API test unchanged and runs it against a real, isolated PostgreSQL container |
| `PostgresIT` | Flyway and relationship lifecycle, direct SQL foreign-key enforcement, and 32 concurrent scans with independent expected counts |
| Postman / Newman | Real HTTP against the complete Docker stack; CRUD, OR across multiple rules, validation, HTTP errors and cleanup |

Run `mvn test` without Docker, or `mvn verify` with Docker for the complete suite. The CI workflow runs verification and then Newman against Compose. A passing host verification does not imply CI has run; the published CI result must be checked separately.

The concurrent scan test checks correctness under parallel requests; it is not a throughput benchmark or production capacity guarantee. Randomized matcher inputs use fixed seeds so failures can be reproduced. Assertions use expected token counts, not a second copy of the regex implementation.

Both API database suites check that oversized pagination offsets return 400 rather than a JPA/internal server failure. The standalone HTTP script uses 15-second request timeouts and waits for every request in a batch before deleting its temporary policy and rule.

Optional inspection explanations are checked for passing, failing, zero-count and empty-policy cases, per-keyword totals, multi-rule OR decisions and unchanged default output. The same tests run against PostgreSQL. Query-count verification also covers detailed scans.

Unicode regression coverage checks that case-equivalent keyword entries cannot inflate counts, that configured spelling is preserved, and that valid 100-character Unicode keywords are not expanded by lowercasing. These cases run on H2 and PostgreSQL.
