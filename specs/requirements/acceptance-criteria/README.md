# Acceptance Criteria

This directory contains acceptance criteria definitions that are used to validate feature implementations.

## Contents

| File | Description |
|------|-------------|
| *(to be added)* | Acceptance criteria documents per feature |

## Format

Each acceptance criteria document should follow the **Given / When / Then** (GWT) pattern:

```gherkin
Given [a precondition]
When [an action is taken]
Then [an expected outcome occurs]
```

## Relationship to Other Specs

- **Feature specs** (`../../features/`) include summary acceptance criteria inline
- **Acceptance criteria documents** here provide the full, formal, testable criteria
- **Test cases** (`../../testing/test-cases/`) implement these criteria as executable tests
- **Issue templates** (`.github/ISSUE_TEMPLATE/feature-specification.md`) reference these criteria

## Usage in CI

Acceptance criteria documents may be referenced by automated tests via `@Tag` annotations. See `/.github/copilot/rules/testing-patterns.md` for testing conventions.
