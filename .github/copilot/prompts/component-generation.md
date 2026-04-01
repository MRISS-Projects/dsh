# Component Generation Prompts

Use these prompt templates with GitHub Copilot Chat to scaffold new DSH components consistently.

---

## New Spring Boot Service

```
Generate a Spring Boot @Service class for [ServiceName] in the DSH project.

Requirements:
- Package: com.dsh.[module]
- Dependencies to inject: [list dependencies]
- Methods: [describe each method with input/output]
- Error handling: throw custom exceptions for [error scenarios]
- Logging: use @Slf4j, log entry/exit at DEBUG, errors at ERROR
- Follow conventions in /.github/copilot/rules/java-conventions.md
- Include JavaDoc referencing /specs/features/[feature].md
```

---

## New REST Controller

```
Generate a Spring Boot @RestController for [ResourceName] in the DSH project.

Requirements:
- Package: com.dsh.api.controller
- Base path: /api/v1/[resource-path]
- Endpoints (from /specs/api/openapi/dsh-rest-api.yaml):
  - [HTTP method] [path] – [description]
- Use ResponseEntity<T> for all responses
- Inject [ServiceName] via constructor injection
- Add @Valid to request body parameters
- Return appropriate HTTP status codes per /.github/copilot/rules/api-standards.md
- Include @Operation and @ApiResponse annotations for OpenAPI documentation
```

---

## New DTO Class

```
Generate a DTO record/class for [DtoName] in the DSH project.

Requirements:
- Package: com.dsh.api.dto
- Fields: [list fields with types and validation constraints]
- Use Jakarta Bean Validation annotations
- Use Lombok @Builder and @Value (or Java record if immutable)
- Include JavaDoc for each field explaining its purpose
- Reference schema in /specs/api/schemas/[schema-name].json
```

---

## New Repository

```
Generate a Spring Data [MongoDB/JPA] repository for [EntityName] in the DSH project.

Requirements:
- Package: com.dsh.data.repository
- Entity: [EntityName] (describe fields)
- Custom query methods: [list methods]
- Use @Query annotation where JPQL/MQL is needed
- Include documentation referencing /specs/architecture/data-models/
```

---

## New Exception Class

```
Generate a custom exception class for [ExceptionName] in the DSH project.

Requirements:
- Package: com.dsh.core.exception
- Extends: RuntimeException
- HTTP status mapping: [status code]
- Fields: [error code, message, details]
- Include a static factory method for common scenarios
```

---

## New Configuration Class

```
Generate a Spring @Configuration class for [ConfigName] in the DSH project.

Requirements:
- Package: com.dsh.[module].config
- @ConfigurationProperties prefix: dsh.[prefix]
- Properties: [list configuration properties with types and defaults]
- Bean definitions: [describe beans to create]
- Include validation annotations on properties
```
