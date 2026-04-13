---
name: API Change Request
about: Request a change to the DSH REST API
title: "[API] "
labels: api-change, needs-review
assignees: ''
---

## API Change Summary
<!-- Describe the API change being requested -->

## Type of Change
- [ ] New endpoint
- [ ] Modify existing endpoint (breaking change)
- [ ] Modify existing endpoint (non-breaking change)
- [ ] Deprecate endpoint
- [ ] Remove endpoint
- [ ] Schema change

## Affected Endpoints

| Method | Path | Description of Change |
|--------|------|-----------------------|
| | | |

## Motivation
<!-- Why is this change needed? What problem does it solve? -->

## Proposed OpenAPI Changes
<!-- Describe or paste the proposed changes to /specs/api/openapi/dsh-rest-api.yaml -->

```yaml
# Paste relevant OpenAPI YAML snippet here
```

## Breaking Change Assessment
- [ ] This is a **breaking change** (requires API version bump)
- [ ] This is a **non-breaking change** (backwards compatible)

**Impact on existing consumers:**
<!-- Describe how existing API consumers will be affected -->

## Migration Plan
<!-- If breaking, describe how existing clients should migrate -->

## Acceptance Criteria
- [ ] OpenAPI spec updated in `specs/api/openapi/dsh-rest-api.yaml`
- [ ] Implementation updated in `dsh-rest-api` module
- [ ] Postman collection updated in `specs/api/postman/`
- [ ] API endpoint spec updated in `specs/features/rest-api-endpoints.md`
- [ ] Tests added/updated

## References
<!-- Any relevant links, RFCs, or prior discussions -->
