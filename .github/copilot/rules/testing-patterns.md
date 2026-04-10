# Testing Patterns for DSH

## Testing Pyramid
Follow the classic testing pyramid:
1. **Unit Tests** – Fast, isolated, no Spring context (majority of tests)
2. **Integration Tests** – Slice tests or full Spring context with test containers
3. **API / E2E Tests** – Postman collections in `/specs/api/postman/`

## Frameworks & Libraries
- **JUnit 5** (`@Test`, `@ParameterizedTest`, `@ExtendWith`)
- **Mockito** for mocking dependencies (`@Mock`, `@InjectMocks`, `MockitoExtension`)
- **AssertJ** for fluent assertions (`assertThat(...)`)
- **Spring Boot Test** for slice and integration tests
- **Testcontainers** for MongoDB/Solr integration tests
- **MockMvc** for controller slice tests (`@WebMvcTest`)

## Naming Conventions
- Test class: `<ClassUnderTest>Test` (unit) or `<ClassUnderTest>IT` (integration)
- Test method: `<methodName>_<scenario>_<expectedOutcome>`
  - Example: `analyzeDocument_whenFileIsEmpty_shouldThrowValidationException`

## Unit Test Structure
Use the **Arrange / Act / Assert** (AAA) pattern:
```java
@Test
void analyzeDocument_whenValidInput_shouldReturnResult() {
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

## Controller Tests (`@WebMvcTest`)
- Test only the web layer; mock all service dependencies
- Verify HTTP status codes, response body structure, and headers
- Test validation errors by sending invalid payloads

```java
@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @Test
    void uploadDocument_whenValidFile_shouldReturn202() throws Exception {
        mockMvc.perform(multipart("/api/v1/documents")
                .file("file", "content".getBytes()))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.documentId").exists());
    }
}
```

## Integration Tests (`@SpringBootTest`)
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` for full-stack tests
- Use Testcontainers for external dependencies (MongoDB, Solr)
- Annotate with `@Tag("integration")` so they can be excluded from fast builds
- Clean up test data in `@AfterEach`

## Test Data & Fixtures
- Create fixture classes (e.g., `DocumentFixture`, `HighlightFixture`) in a `fixtures` test package
- Use the builder pattern to construct test objects
- Prefer static factory methods: `DocumentFixture.valid()`, `DocumentFixture.withInvalidFormat()`

## Coverage Requirements
- Minimum 80% line coverage for service classes
- Minimum 70% line coverage overall (enforced by DSH-Coverage-Report module)
- All critical paths (error handling, edge cases) must be explicitly tested

## Performance Tests
- Reference benchmark targets in `/specs/testing/performance-benchmarks/`
- Use JMeter or Gatling for load tests; store scripts in `/specs/testing/performance-benchmarks/`
- Annotate performance-sensitive tests with `@Tag("performance")`

## Test Execution
- Unit tests run on every build: `mvn test`
- Integration tests run in CI: `mvn verify -P integration-tests`
- Performance tests run on release: reference `.github/workflows/api-testing.yml`
