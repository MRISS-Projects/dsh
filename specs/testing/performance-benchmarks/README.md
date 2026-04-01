# Performance Benchmarks

This directory contains performance benchmark specifications and targets for the DSH system.

## Contents

| File | Description |
|------|-------------|
| *(to be added)* | Performance test scripts and benchmark targets |

## Benchmark Targets

### Document Analysis

| Scenario | Target | Measurement |
|----------|--------|-------------|
| Document analysis (< 10 MB) | P95 ≤ 30 seconds | End-to-end (upload → completed) |
| Document analysis (< 1 MB) | P95 ≤ 5 seconds | End-to-end |
| Concurrent processing | ≥ 10 documents simultaneously | Worker throughput |

### REST API

| Endpoint | Target | Percentile |
|----------|--------|-----------|
| `GET /documents/{id}` | ≤ 100 ms | P99 |
| `GET /documents/{id}/highlights` | ≤ 200 ms | P99 |
| `POST /documents` (acceptance only) | ≤ 500 ms | P99 |

## Tooling

- **JMeter** – load testing scripts (`.jmx` files)
- **Gatling** – Scala-based load tests (`.scala` files)
- **JUnit 5 `@Timeout`** – per-test SLA enforcement in integration tests

## References

- Non-functional requirements: `../../requirements/non-functional/`
- Testing patterns: `/.github/copilot/rules/testing-patterns.md`
- CI workflow: `/.github/workflows/api-testing.yml`
