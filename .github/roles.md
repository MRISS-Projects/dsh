# Team Roles

This document defines the main roles within the DSH (Document Smart Highlights) project and their respective responsibilities.

---

## Product Manager

**Responsibilities:**
- Create and maintain feature specifications in the `specs/features/` folder
- Define and document functional and non-functional requirements in `specs/requirements/`
- Write acceptance criteria in `specs/requirements/acceptance-criteria/`
- Collaborate with developers to ensure specifications are clear and actionable

**Deliverables:**
- Feature specifications (`specs/features/*.md`)
- User stories and requirements (`specs/requirements/`)
- Acceptance criteria documents

---

## Backend Developer

**Responsibilities:**
- Develop backend services and REST API implementations
- Specialisation: Spring Boot and Spring Framework
- Maintain and evolve the following modules:
  - `DSH-rest-api` – REST API service layer
  - `DSH-doc-analyser` – Document analysis engine
  - `DSH-doc-indexer-worker` – Background indexing worker
  - `DSH-data` – Shared data models and persistence
- Implement features according to specifications in `specs/features/` and `specs/api/`
- Write unit and integration tests following patterns in `specs/testing/`

**Deliverables:**
- Java services, controllers, and repositories
- REST API implementations conforming to `specs/api/openapi/dsh-rest-api.yaml`
- Database integrations and data model implementations
- Unit and integration tests

---

## Frontend Developer

**Responsibilities:**
- Develop the user-facing interface for document highlighting and analysis
- Technology stack to be determined
- Consume REST APIs defined in `specs/api/openapi/dsh-rest-api.yaml`
- Implement UI/UX based on feature specifications in `specs/features/`

**Deliverables:**
- UI components and views
- User experience implementations
- Frontend build configuration

---

## DevOps Developer

**Responsibilities:**
- Create and maintain CI/CD pipelines using GitHub Actions
- Manage and update Maven `pom.xml` build configurations across all modules
- Maintain build and deployment scripts (e.g., `build-ci.sh`, `deploy.sh`)
- Ensure quality gates and automated validation workflows
- Manage infrastructure and deployment configurations

**Deliverables:**
- GitHub Actions workflows (`.github/workflows/`)
- Maven `pom.xml` structures and configurations
- Build scripts and deployment automation
- CI/CD pipeline documentation
