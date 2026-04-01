# Non-Functional Requirements

This directory contains non-functional requirements (NFRs) for the DSH system.

## Contents

| File | Description |
|------|-------------|
| *(to be added)* | NFR documents covering performance, security, scalability, etc. |

## NFR Categories

| Category | Examples |
|----------|---------|
| Performance | Response time SLAs, throughput targets |
| Scalability | Concurrent users, document volume |
| Security | Authentication, authorisation, data protection |
| Availability | Uptime targets, failover behaviour |
| Maintainability | Code coverage thresholds, documentation standards |

## Performance Targets (initial)
- Document analysis: 95% of documents &lt; 10 MB analysed within 30 seconds
- REST API: P99 response time &lt; 500 ms for GET endpoints
- Concurrent workers: minimum 10 simultaneous document processing tasks

## References
- Performance benchmarks: `../../testing/performance-benchmarks/`
- Architecture: `../../architecture/system-design.md`
