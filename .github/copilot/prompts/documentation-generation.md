# Documentation Generation Prompts

Use these prompt templates with GitHub Copilot Chat to generate consistent documentation for DSH.

---

## Feature Specification

```
As a Product Manager (see /.github/roles.md), generate a feature specification
document for [FeatureName] in the DSH project.

Save to: specs/features/[feature-name].md

Include:
- Overview and purpose
- Functional requirements (FR001, FR002, ...) with description, input, output, and acceptance criteria
- Technical implementation notes referencing relevant DSH modules
- Data flow description
- Testing requirements (unit and integration)
- References to related specs and architecture documents
```

---

## API Endpoint Documentation

```
As a Backend Developer (see /.github/roles.md), document the [HTTP method] [path]
endpoint for the DSH REST API.

Add to: specs/features/rest-api-endpoints.md

Include:
- Endpoint purpose and description
- Request parameters (path, query, headers, body)
- Response schema with examples
- Error cases and status codes
- Authentication requirements
- Rate limiting or performance notes
- Corresponding OpenAPI definition in /specs/api/openapi/dsh-rest-api.yaml
```

---

## Architecture Decision Record (ADR)

```
As a Backend Developer (see /.github/roles.md), generate an Architecture Decision
Record for the decision to [describe decision].

Save to: specs/architecture/[adr-number]-[short-title].md

Include:
- Date
- Status (Proposed / Accepted / Deprecated / Superseded)
- Context: why is this decision needed?
- Decision: what was decided?
- Consequences: positive and negative outcomes
- Alternatives considered
```

---

## OpenAPI Schema

```
As a Backend Developer (see /.github/roles.md), generate an OpenAPI 3.0.3 schema
component for [SchemaName] in the DSH API.

Add to: specs/api/openapi/dsh-rest-api.yaml under components/schemas

Requirements:
- Include all fields described in /specs/features/[feature].md
- Use appropriate types and formats
- Add descriptions for each property
- Mark required fields
- Include validation constraints (minLength, minimum, pattern, enum)
- Add an example object
```

---

## JavaDoc for Class

```
As a Backend Developer (see /.github/roles.md), generate comprehensive JavaDoc
for [ClassName] in the DSH project.

Requirements:
- Class-level JavaDoc: describe purpose, responsibilities, and usage
- Reference relevant spec: @see /specs/features/[feature].md
- Method-level JavaDoc: describe what each method does, not how
- Include @param, @return, @throws for all public methods
- Use {@link} to reference related classes
```

---

## README for Module

```
As a Backend Developer (see /.github/roles.md), generate a README.md for the
[ModuleName] Maven module in the DSH project.

Include:
- Module purpose and responsibilities
- Key classes and their roles
- Configuration properties (application.properties/yml keys)
- How to build and run locally
- Dependencies on other DSH modules
- Links to relevant specifications in /specs/
```
