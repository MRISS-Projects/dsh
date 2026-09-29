# Testing Patterns for DSH

## Test layers

Four layers, most tests in the first. Each has a trigger that makes it required.

| Layer | What | Required when | Runs |
|---|---|---|---|
| 1. Unit | Mockito, no Spring context, REST entry points included | always; the 95% gate counts only this layer | everywhere, `mvn -B clean install` |
| 2. Spring context | `*IT`, full or sliced context, `@MockBean` for every external service, MockMvc or Spring's test framework for REST entry points | a story creates or changes a Spring bean class, REST entry points included | staging, deploy, local gate |
| 3. Over the wire | `*HttpIT`, the application forked by Maven, real ports, real external services in Docker | a story adds a REST endpoint or changes one's contract | staging, deploy, local gate |
| 4. External client | Postman collections run by newman: a consumer that sees only the API contract, not the internals | when collections exist, `#137` | staging, deploy, local gate |

- **Layers 2 and 3 share a vantage point.** We write both the client and the provider. Layer 4 is
  the only one that tests as an outsider, which is why it is needed at all.
- **Going forward only.** The layer-2 and layer-3 requirements are enforced at review, not
  mechanically. Existing beans and endpoints are not retrofitted.

## Where integration tests run

Layers 2 to 4 run in three places, and **never on pull requests**: `ci.yml` does not pass
`-DintegrationTests`, and `api-testing.yml` builds with `-DskipTests`.

- **Staging:** every staging build passes `-DintegrationTests`.
- **Deploy:** a deploy workflow from `DEVELOP`. It does not exist yet.
- **The local gate**, in addition to the unit gate:
  - **When it applies:** a story changes code that touches an external system (MongoDB, RabbitMQ,
    Solr, or any other service outside the JVM) or a REST API entry point.
  - **What runs:** after the root `mvn -B clean install` passes,
    `mvn -B clean verify -DintegrationTests -pl <changed modules> -amd`. The changed modules are
    the ones whose code changed.
  - **`-amd`, so the consumers run too.** A change to shared code breaks in the modules that depend
    on it: #139 was a `dsh-data` defect that only `dsh-rest-api`'s IT could see, and `dsh-data`
    has no ITs of its own.
  - **No `-am`, deliberately.** The root build has already installed every upstream module, and
    `-am` would run their integration tests too.

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
- CI enforces the rule with `.github/scripts/check-unit-tests-context-free.sh`. It exempts only an
  `*IT` or `*IntegrationTest` inside an `integration` package, and fails any other test
  referencing `org.springframework.test.context`,
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

A controller has two tests, one at each of layers 1 and 2. An endpoint added or changed in its
contract also gets a layer-3 test.

**Unit (layer 1)** — Mockito over the controller, no context, as `DocumentResourceTest` does: mock the
services, `@InjectMocks` the controller, call its methods directly, assert on the returned DTOs.
Cover every branch here; this is what the coverage gate counts.

**Spring context (layer 2)** — a `@WebMvcTest` slice (or `@SpringBootTest` with MockMvc) starts a context, so it
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

## Layer 2: Spring context

- Named `*IT`, in the module's `integration` package — the name is the selector, no tag is needed
- **Required when a story creates or changes a Spring bean class**, REST entry points included
- Every external service is a `@MockBean`; nothing leaves the JVM
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` for a full context in the test JVM. That is
  still in-process, with `@MockBean`s standing in for MongoDB and RabbitMQ; it is not layer 3
- Clean up test data in `@After`

## Layer 3: over the wire

- Named `*HttpIT`, in the module's `integration` package. Failsafe selects it as an `*IT`
- **Required when a story adds a REST endpoint or changes one's contract**

`dsh-rest-api`'s `http-integration-tests` profile, active under `-DintegrationTests`, runs the
application as a **separate process** and runs **real MongoDB and RabbitMQ** in Docker around the
integration-test phase. `DocumentResourceHttpIT` is the example.

- **No Spring context in the test JVM.** The test is a plain HTTP client (`RestTemplate`) against
  the forked server. Nothing is mocked; a request goes through Mongo and the broker for real
- **The URL comes from `dsh.it.baseUrl`**, which failsafe sets from a port reserved at build time.
  Never hardcode a port, and never fall back to `localhost:8080`: run outside the lifecycle, the
  test must fail and say why
- **Docker is a prerequisite** of `-DintegrationTests`. The plain `mvn -B clean install` gate does
  not need it
- **Containers leak only if startup fails.** A failing *test* still reaches
  `post-integration-test`, which stops the application and then the containers. A failure in
  `spring-boot:start` does not, so the containers survive. Remove them with
  `docker rm -f $(docker ps -aq --filter name=dsh-it-)`

## Layer 4: external client

Postman collections, run by newman against a lifecycle-managed server, test as a consumer that
sees only the API contract. **Not yet in force:** no collections exist, and wiring newman into the
lifecycle is `#137`. Until then no story is required to add one.

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
- Integration tests run under `mvn -B install -DintegrationTests`; where and when is "Where
  integration tests run" above. Profiles in this estate are activated by `-D` properties, never `-P`
- Performance tests run on release: reference `.github/workflows/api-testing.yml`
