# Testing Patterns for DSH

## Testing Pyramid

Follow the classic testing pyramid:

1. **Unit Tests** – Fast, isolated, no Spring context (majority of tests)
2. **Integration Tests** – Slice tests or full Spring context, `*IT` in an `integration` package
3. **API / E2E Tests** – Postman collections in `/specs/api/postman/`

## Frameworks & Libraries

- **JUnit 4** (`@Test`, `@Before`, `@RunWith`) — every test in the repository is JUnit 4; match the
  module, and do not mix in JUnit 5. Migrating is a separate decision
- **Mockito** for mocking dependencies (`@Mock`, `@InjectMocks`, `MockitoJUnitRunner`)
- **AssertJ** for fluent assertions (`assertThat(...)`)
- **Spring Boot Test** for slice and integration tests
- **MockMvc** for controller integration tests
- **PowerMock is forbidden**, in any scope, directly or transitively — here and in every project
  inheriting from parent-poms, enforced by its `ban-powermock` enforcer execution. **Mockito is the
  sanctioned mocking tool, `mockStatic` included.** Static mocking is allowed but discouraged: use it
  only where no seam can reasonably be designed in.

## Unit vs integration

> **A unit test never starts a Spring context.** A test that starts one — full, sliced, or
> hand-built — is an integration test, named `*IT`, placed in an `integration` package.

- The rule is about the context, not about Spring. A unit test may reference production types that
  happen to be Spring classes (`SpringApplication` as a `mockStatic` target, `SpringApplicationBuilder`
  as an argument), and may use context-free test helpers (`MockMultipartFile`,
  `MockMvcBuilders.standaloneSetup`, `ReflectionTestUtils`).
- The `integration` package is `<module base package>.integration`, e.g.
  `com.mriss.dsh.restapi.integration`. `@SpringBootTest` there still finds the application by
  searching upward.
- The name is the selector: surefire runs `*Test`, failsafe runs `*IT` and `*IntegrationTest`. An
  integration test never counts toward the 95% coverage gate — it writes `jacoco-it.exec`, and the
  gate reads `jacoco.exec`.
- A test that uses Spring without needing it is rewritten context-free and stays a unit test. Only a
  test that genuinely exercises the context becomes an `*IT`.
- CI enforces the rule with `.github/scripts/check-unit-tests-context-free.sh`, which fails any
  non-`*IT` test referencing `org.springframework.test.context`,
  `org.springframework.boot.test.context`, `org.springframework.boot.test.autoconfigure` (every
  slice), `org.springframework.boot.test.mock.mockito` (`@MockBean`, `@SpyBean`),
  `webAppContextSetup`, or a hand-built `new …ApplicationContext(`.

## Naming Conventions

- Test class: `<ClassUnderTest>Test` (unit) or `<ClassUnderTest>IT` (integration)
- Test method: `<methodName>_<scenario>_<expectedOutcome>`
  - Example: `analyzeDocument_whenFileIsEmpty_shouldThrowValidationException`

## Unit Test Structure

Use the **Arrange / Act / Assert** (AAA) pattern:

```java
@Test
public void analyzeDocument_whenValidInput_shouldReturnResult() {
    // Arrange
    var input = DocumentFixture.validInput();
    when(repository.save(any())).thenReturn(DocumentFixture.savedDocument());

    // Act
    var result = service.analyzeDocument(input);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
}
```

## Controller Tests

A controller has two tests, one at each level.

**Unit** — Mockito over the controller, no context, as `DocumentResourceTest` does: mock the
services, `@InjectMocks` the controller, call its methods directly, assert on the returned DTOs.
Cover every branch here; this is what the coverage gate counts.

**Integration** — a `@WebMvcTest` slice (or `@SpringBootTest` with MockMvc) starts a context, so it
is an `*IT` in the `integration` package, as `DocumentResourceIT` does:

- Test only the web layer; mock all service dependencies
- Verify HTTP status codes, response body structure, and headers
- Test validation errors by sending invalid payloads

```java
@RunWith(SpringRunner.class)
@WebMvcTest(DocumentController.class)
public class DocumentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @Test
    public void uploadDocument_whenValidFile_shouldReturn202() throws Exception {
        mockMvc.perform(multipart("/api/v1/documents")
                .file("file", "content".getBytes()))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.documentId").exists());
    }
}
```

## Integration Tests (`@SpringBootTest`)

- Named `*IT`, in the module's `integration` package — the name is the selector, no tag is needed
- An integration test is **mandatory** when a task touches the REST API; for other Spring beans it
  is optional by design
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` for full-stack tests
- Clean up test data in `@After`

## Test Data & Fixtures

- Create fixture classes (e.g., `DocumentFixture`, `HighlightFixture`) in a `fixtures` test package
- Use the builder pattern to construct test objects
- Prefer static factory methods: `DocumentFixture.valid()`, `DocumentFixture.withInvalidFormat()`

## Coverage Requirements

- Minimum 95% LINE and 95% BRANCH coverage per module, enforced by `jacoco:check` bound to
  `verify`, so a shortfall fails `mvn -B install`
- A module with production sources that produces no coverage data at all fails the
  `enforce-coverage-data-exists` guard, which closes the gap left by `jacoco:check` skipping a
  module with no exec data
- Both are inherited from `MRISS-Projects/parent-poms`, not declared in this repository — grepping
  here will not find them. `dsh-coverage-report` aggregates coverage for reporting and enforces
  nothing
- `-DskipTests`, `-Dmaven.test.skip=true`, `-Dmaven.test.skip.exec=true` and `-Djacoco.skip=true`
  disarm the data guard along with what they skip; `-Dcoverage.data.check.skip=true` disables it for
  a genuinely exempt module
- `-Denforcer.skip=true` does **not** disable the guard — that execution sets `<skip>` explicitly,
  which beats the parameter's `enforcer.skip` user property — but it *does* disable this
  repository's own `enforce-lowercase-artifact-id` rule, which sets none. It is the wrong tool in
  both directions
- All critical paths (error handling, edge cases) must be explicitly tested

## Performance Tests

- Reference benchmark targets in `/specs/testing/performance-benchmarks/`
- Use JMeter or Gatling for load tests; store scripts in `/specs/testing/performance-benchmarks/`
- Annotate performance-sensitive tests with a JUnit 4 `@Category(PerformanceTest.class)`

## Test Execution

- Unit tests run on every build: `mvn -B install`
- Integration tests run under `mvn -B install -DintegrationTests`; staging always passes the flag.
  Profiles in this estate are activated by `-D` properties, never `-P`
- Performance tests run on release: reference `.github/workflows/api-testing.yml`
