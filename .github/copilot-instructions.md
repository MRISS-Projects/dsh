# DSH Project Copilot Instructions

## Project Overview
Document Smart Highlights (DSH) is a Java-based system for intelligent document analysis and highlighting. The system consists of multiple Maven modules including document analyzers, indexers, and REST APIs, built on Spring Boot and Spring Framework.

## Architecture Summary
- **dsh-doc-analyser** – Core document analysis engine
- **dsh-doc-indexer-worker** – Background indexing worker
- **dsh-rest-api** – REST API service layer
- **dsh-data** – Shared data models and persistence
- **dsh-solr** – Apache Solr integration
- **dsh-coverage-report** – Aggregated test coverage reporting

## Development Standards

### Code Structure
- Follow Maven multi-module project structure (see `pom.xml` and `parent-pom.xml`)
- Use Spring Boot for microservices (existing pattern in dsh-rest-api)
- Implement clean architecture patterns
- Reference specifications in `/specs/` directory before implementing features
- **All Maven module artifactIds must be lowercase** (only lowercase letters, digits, and hyphens). This is enforced by `maven-enforcer-plugin` in the root `pom.xml`.

### API Development
- All APIs must conform to OpenAPI specifications in `/specs/api/openapi/`
- Use DTOs defined in `/specs/api/schemas/`
- Follow RESTful conventions documented in `/specs/features/rest-api-endpoints.md`
- Adhere to API standards in `/.github/copilot/rules/api-standards.md`

### Testing Patterns
- Unit tests: Follow patterns in `/.github/copilot/rules/testing-patterns.md`
- Integration tests: Reference `/specs/testing/test-plans/`
- API tests: Use Postman collections in `/specs/api/postman/`
- Performance tests: Reference `/specs/testing/performance-benchmarks/`

### Documentation
- Auto-generate API documentation from OpenAPI specs
- Reference architecture diagrams in `/specs/architecture/`
- Update feature specifications in `/specs/features/` when implementing new functionality

## File References

| Resource | Location |
|----------|----------|
| Architecture overview | `/specs/architecture/system-design.md` |
| API specification | `/specs/api/openapi/dsh-rest-api.yaml` |
| Java conventions | `/.github/copilot/rules/java-conventions.md` |
| API standards | `/.github/copilot/rules/api-standards.md` |
| Testing patterns | `/.github/copilot/rules/testing-patterns.md` |
| Component generation | `/.github/copilot/prompts/component-generation.md` |
| Test generation | `/.github/copilot/prompts/test-generation.md` |
| Documentation generation | `/.github/copilot/prompts/documentation-generation.md` |
| Feature implementation | `/.github/copilot/prompts/feature-implementation.md` |
| Development guide | `/docs/copilot/development-guide.md` |
| Team roles | `/.github/roles.md` |

## Wiki References

The `/docs/wiki/` directory contains Markdown copies of the [DSH GitHub Wiki](https://github.com/MRISS-Projects/dsh/wiki) pages, automatically synced daily by `.github/workflows/wiki-sync.yml`. Use these files as `@workspace` context in Copilot prompts exactly like any spec file.

| Wiki Page | Location in repo |
|-----------|-----------------|
| Wiki index / Home | `/docs/wiki/Home.md` |
| *(additional pages appear here after first sync)* | `/docs/wiki/<Page-Name>.md` |

> **Note:** Wiki files are auto-generated. Edit them only via the [GitHub Wiki](https://github.com/MRISS-Projects/dsh/wiki). Run **Actions → Wiki Sync → Run workflow** to pull the latest content immediately.

## Component Guidelines

### Document Analyzer (dsh-doc-analyser)
- Implement analysis algorithms per `/specs/features/document-analysis.md`
- Use component patterns from `/specs/architecture/component-diagrams/`
- Follow Java conventions in `/.github/copilot/rules/java-conventions.md`

### REST API (dsh-rest-api)
- Follow OpenAPI spec: `/specs/api/openapi/dsh-rest-api.yaml`
- Implement endpoints per `/specs/features/rest-api-endpoints.md`
- Use Spring Boot patterns and annotations as described in `/.github/copilot/rules/java-conventions.md`

### Indexer Worker (dsh-doc-indexer-worker)
- Follow workflow specification: `/specs/features/indexing-workflow.md`
- Reference performance benchmarks: `/specs/testing/performance-benchmarks/`

## Code Generation Preferences
- Generate comprehensive JavaDoc comments for all public classes and methods
- Include unit tests with each component
- Follow existing Spring Boot patterns (constructor injection, `@Service`, `@RestController`)
- Use builder patterns for complex DTOs
- Include Bean Validation annotations (`@NotNull`, `@Valid`, etc.)
- Use SLF4J with `@Slf4j` (Lombok) for logging
- Prefer immutable value objects where applicable

## Testing Approach
- Generate tests based on specifications in `/specs/testing/`
- Include integration tests for API endpoints
- Generate performance tests for indexing operations
- Reference acceptance criteria in `/specs/requirements/acceptance-criteria/`
- Follow patterns described in `/.github/copilot/rules/testing-patterns.md`
