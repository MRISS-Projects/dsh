# Test Cases

This directory contains detailed test case specifications for DSH features.

## Contents

| File | Description |
|------|-------------|
| *(to be added)* | Detailed test case documents per feature or component |

## Test Case Format

Each test case should include:
- **ID** – unique identifier (e.g., `TC-DOC-001`)
- **Title** – descriptive name
- **Feature** – reference to feature spec in `../../features/`
- **Acceptance Criteria** – reference to AC in `../../requirements/acceptance-criteria/`
- **Preconditions** – state required before test execution
- **Steps** – numbered steps to reproduce
- **Expected Result** – what should happen
- **Actual Result** – filled in during test execution
- **Status** – Pass / Fail / Blocked

## Automated vs Manual Tests

- Automated test cases should be implemented in JUnit 5 following `/.github/copilot/rules/testing-patterns.md`
- Manual test cases are documented here for exploratory and UAT testing
- Postman-based API test cases are stored in `../../api/postman/`
