# Java Development Conventions for DSH

## Package Structure
- `com.dsh.analyzer` – Document analysis components
- `com.dsh.api` – REST API controllers and DTOs
- `com.dsh.indexer` – Document indexing services
- `com.dsh.core` – Shared utilities and models
- `com.dsh.data` – Persistence and data access

## Naming Conventions
- Services: `*Service` suffix (e.g., `DocumentAnalysisService`)
- Controllers: `*Controller` suffix (e.g., `DocumentController`)
- DTOs: `*Dto` suffix (e.g., `AnalysisResultDto`)
- Entities: Descriptive names without suffix (e.g., `Document`, `Highlight`)
- Repositories: `*Repository` suffix (e.g., `DocumentRepository`)
- Configuration classes: `*Configuration` suffix

## Spring Boot Patterns
- Use `@RestController` for API endpoints; pair with `@RequestMapping` at class level
- Implement service layer with `@Service`; prefer constructor injection over field injection
- Use `@Component` for utilities and helpers
- Follow repository pattern with Spring Data (e.g., `MongoRepository`, `JpaRepository`)
- Use `@Configuration` and `@Bean` for infrastructure wiring
- Prefer `@ConfigurationProperties` over `@Value` for grouped configuration

## Dependency Injection
- Always use constructor injection (not `@Autowired` on fields)
- Declare dependencies as `private final` fields
- Use Lombok `@RequiredArgsConstructor` to reduce boilerplate

## Error Handling
- Define custom exceptions extending `RuntimeException` in a dedicated `exception` package
- Implement a global exception handler with `@RestControllerAdvice`
- Return `ProblemDetail` (Spring 6+) or a structured error response for all error cases
- Map exceptions to appropriate HTTP status codes as defined in the API spec

## Logging
- Use SLF4J via Lombok `@Slf4j` annotation
- Include correlation/request IDs for distributed tracing
- Log method entry/exit at `DEBUG` level for services
- Log errors with full stack traces at `ERROR` level
- Avoid logging sensitive data (PII, credentials)

## Validation
- Use Jakarta Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Size`, `@Valid`, etc.)
- Validate DTOs at the controller boundary using `@Valid`
- Define custom validators for complex business rules

## Testing
- See `/.github/copilot/rules/testing-patterns.md` for full testing conventions
- Every public service method should have unit test coverage
- Use `@SpringBootTest` for integration tests; `@WebMvcTest` for controller slice tests

## JavaDoc
- Add JavaDoc to all public classes and public/protected methods
- Include `@param`, `@return`, and `@throws` tags where applicable
- Reference the relevant spec document in class-level JavaDoc (e.g., `@see /specs/features/document-analysis.md`)

## Code Style
- Follow Google Java Style Guide
- Maximum line length: 120 characters
- Use `final` for local variables and parameters where possible
- Avoid raw types; always parameterise generics
