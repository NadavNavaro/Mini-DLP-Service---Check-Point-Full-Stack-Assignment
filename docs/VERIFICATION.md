# Verification record

Reviewed and verified on 2026-10-31, including the extracted Desktop project.

## Java test results

| Suite | Passed | Purpose |
|---|---:|---|
| ApiTest | 79 | API contract on H2 in PostgreSQL compatibility mode |
| KeywordMatcherTest | 61 | Whole-word matching and reproducible generated text |
| ScanQueryTest | 1 | One scan query with 20 rules and no duplicated counts |
| PostgresApiIT | 79 | The same API contract on real PostgreSQL 17 |
| PostgresIT | 3 | Relationship integrity, migration/lifecycle and concurrent scans |
| **Total** | **223** | **Zero failures, errors or skips in host verification** |

Parameterized invocations are counted separately. Repeating the API suite on PostgreSQL is intentional: H2 compatibility mode does not replace verification against the deployed engine. The PostgreSQL-specific concurrency case checks 32 scans with separate expected occurrence counts.

The eight-keyword matrix checks 0, 1, 2, 3, 4, 8 and 10 occurrences, multiple thresholds, mixed case, repeated words, punctuation and embedded-substring negatives. Both list endpoints also reject page offsets beyond JPA's signed integer range with a 400 response.

## Docker and real HTTP

* A fresh application image was built from source using Java 21; the application and PostgreSQL healthchecks passed.
* The Docker build separately passed 141 matcher/API/query test cases. PostgreSQL Testcontainers suites skip inside the isolated build container, which has no Docker socket. All 82 PostgreSQL cases ran successfully in the host verification above.
* The complete Postman collection ran against the Docker application backed by PostgreSQL: **46 requests and 65 assertions passed**.
* The delivered HTTP script passed **120 scans in batches of eight concurrent requests**, using all eight demo keywords and non-matching longer words. This is a local correctness check, not a production capacity guarantee.
* The HTTP script supports a configurable base URL, 15-second request timeouts and settling each entire batch before temporary-resource cleanup.
* The application runs as an unprivileged user. PostgreSQL is reachable on the Compose network and has no published host port.
* Compose configuration validation passed.

## Persistence and build portability

A temporary rule was saved, both containers were recreated without deleting the named volume, and the rule was retrieved unchanged. Temporary verification resources were deleted afterward.

The included Windows Maven Wrapper was executed and downloaded/ran Maven 3.9.11 successfully. Host verification used Temurin 25 with compilation targeting Java 21; the Docker build and runtime used Java 21. The Unix wrapper retains LF line endings and its executable Git mode.

## Source and documentation review

* Application classes have English responsibility comments; selected methods explain matching, transaction and relationship decisions.
* The concise README covers startup, API use and key decisions. The linked technical reference maps assignment requirements to implementation and documents full input bounds, responses, configuration and limitations.
* Repeated input occurrences are explicitly documented as separate matches. Duplicate normalized configuration entries are rejected. The eight demo words remain configurable values, not hardcoded global rules.
* JSON deliverables and README JSON examples parse successfully; Maven XML and the delivered Node script pass syntax checks.
* The applied V1 schema migration was not edited during the review. Future schema changes must use new migrations.
* The submission ZIP contains source and technical documentation. Interview preparation and personal submission notes are kept outside the project and excluded from the archive.

## Repository and CI

Source: [GitHub repository](https://github.com/NadavNavaro/Mini-DLP-Service---Check-Point-Full-Stack-Assignment). Automated runs: [Build and verify](https://github.com/NadavNavaro/Mini-DLP-Service---Check-Point-Full-Stack-Assignment/actions/workflows/ci.yml). The tables above record local verification; consult the linked workflow for the latest remote CI result.

## Optional explanations

`include_details: true` was verified on both databases and through Docker/Postman. It reports policy identity, all evaluated rules, per-rule thresholds and decisions, English reasons and positive per-keyword counts. Default/false/null requests preserve the original response shape. Tests cover passing and failing rules, empty text/policies, exact repeated-word counts, independent OR thresholds, invalid flag types and omission of submitted text from diagnostic metadata. Both compact and explained scans retain one policy-loading query and one counting pass.

## Final review

The extracted Desktop project was compared with the submission source before editing, then synchronized with the reviewed changes. The source was clean-built and verified. The actual Desktop project was separately built without Docker build-step caches and started on port 8080; Postman, concurrent HTTP checks, the Unicode regression and persistence were checked against that running copy.

Three Unicode regression cases failed before the fix and passed afterward. Keywords now retain their configured spelling after trimming/NFC normalization. A Unicode-aware case-insensitive set rejects equivalent keyword entries without expanding the stored text; an ordered list preserves configuration order. The same three cases run on each database, accounting for the six additional passing cases in the 223 total. No schema migration or API response shape was changed.

The source keeps the existing small feature/layer structure. Optional explanations and the standalone concurrency script remain because they are documented, independently testable additions. No new frameworks or runtime features were added. The Maven XML and the overall scan decision branch were formatted for easier reading. CI runs automatically on pushes and pull requests; its latest result is linked above.
