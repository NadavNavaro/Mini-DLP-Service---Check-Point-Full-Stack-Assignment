# Verification

Verified locally on 2026-11-01 after simplifying configuration and description handling.

| Check | Result |
|---|---|
| Clean Maven build and tests | 229 passed, zero failures/errors/skips |
| Real PostgreSQL subset | 85 passed (82 shared API cases and 3 integration cases) |
| Java 21 Docker build | 144 H2/matcher/query cases passed; PostgreSQL suites intentionally skip inside the build container |
| Running Docker + PostgreSQL | Both containers healthy |
| Postman / Newman | 46 requests and 67 assertions passed |
| Concurrent HTTP scans | 120 passed, eight requests per batch |

The host uses Temurin 25 with Java 21 compilation; Docker build/runtime and GitHub Actions use Java 21. PostgreSQL is isolated in Testcontainers for integration tests and in its own persistent Compose service for runtime checks.

The scan query-count test covers compact and explained results. Missing, null and blank descriptions are tested on creation and full replacement, on both databases. Existing matching/threshold/error behavior remains covered.

## Simplifications

* Scans use a read-only transaction with default isolation; one query loads all policy configuration.
* The form-post setting was removed. JSON has no pre-parsing byte limit; the post-parsing text-length constraint and this limitation are documented.
* Description is optional and normalized to an empty string without a schema change.
* Repetitive comments and duplicated reference material were reduced. Optional explanations remain opt-in; overlapping keyword occurrences retain their documented independent-count behavior.

Earlier verification also confirmed data survives recreation of both Compose containers, the Maven Wrapper works, the application runs without root privileges and PostgreSQL is not published on a host port.

The ZIP contains only source and technical documentation; personal interview/submission notes remain outside it. Archive integrity, README JSON examples and documentation links are checked during packaging; the Unix Maven launcher has executable archive permissions.

## Repository and CI

[Source repository](https://github.com/NadavNavaro/Mini-DLP-Service---Check-Point-Full-Stack-Assignment) | [Build and verify workflow](https://github.com/NadavNavaro/Mini-DLP-Service---Check-Point-Full-Stack-Assignment/actions/workflows/ci.yml)

The table records local checks. The linked workflow is the source for the latest remote CI result.
