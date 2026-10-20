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
- Stores: the interface takes the `*Dao` suffix (e.g., `DocumentDao`); an implementation is prefixed
  with what it is built on (e.g., `InMemoryDocumentDao`)
- Configuration classes: `*Configuration` suffix

## Spring Boot Patterns

- Use `@RestController` for API endpoints; pair with `@RequestMapping` at class level
- Implement service layer with `@Service`; prefer constructor injection over field injection
- Use `@Component` for utilities and helpers
- Reach a service outside the JVM through an interface that names no technology, in its name,
  its signatures, its types or its exceptions (ADR-003, section 3, rule 1)
- A consumer names the interface, never an implementation; the implementation is selected by
  its `dsh.*` key (ADR-003, section 3, rules 2 and 3)
- Use `@Configuration` and `@Bean` for infrastructure wiring
- Prefer `@ConfigurationProperties` over `@Value` for grouped configuration
- Write a Spring placeholder as `${…}` in any file on the classpath; Maven's resource filtering
  leaves it alone in `target/classes`, unless a module adds its own `copy-resources` execution
  there. Classpath resources are filtered with `@…@` only, by a rule inherited from parent-poms
  `products` (`#158`). For a build value, write `@name@` and turn on `<filtering>` for that
  module's `src/main/resources`. The site Markdown is not on the classpath and keeps `${…}`

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
- A unit test never starts a Spring context. `@SpringBootTest` and every slice, `@WebMvcTest`
  included, start one, so a test using them is an integration test: `*IT`, in an `integration` package.
  See the test layers in `testing-patterns.md`

## JavaDoc

- Add JavaDoc to all public classes and public/protected methods
- Include `@param`, `@return`, and `@throws` tags where applicable
- Reference the relevant spec document in class-level JavaDoc (e.g., `@see /specs/features/document-analysis.md`)

## Code Style

- Follow Google Java Style Guide
- Maximum line length: 120 characters
- Use `final` for local variables and parameters where possible
- Avoid raw types; always parameterise generics
