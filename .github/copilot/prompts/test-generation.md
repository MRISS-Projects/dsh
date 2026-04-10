# Test Generation Prompts

Use these prompt templates with GitHub Copilot Chat to generate consistent tests for DSH components.

---

## Unit Test for Service

```
Generate JUnit 5 unit tests for [ServiceName] in the DSH project.

Requirements:
- Test class name: [ServiceName]Test
- Follow Arrange/Act/Assert pattern
- Use Mockito to mock: [list dependencies]
- Test cases:
  - Happy path: [describe expected success scenario]
  - Validation failure: [describe invalid input scenario]
  - External dependency failure: [describe error propagation]
- Use AssertJ for assertions
- Follow patterns in /.github/copilot/rules/testing-patterns.md
- Cover acceptance criteria from /specs/requirements/acceptance-criteria/[feature].md
```

---

## Controller Slice Test

```
Generate @WebMvcTest tests for [ControllerName] in the DSH project.

Requirements:
- Test class name: [ControllerName]Test
- Mock service dependencies with @MockBean
- Test cases for each endpoint:
  - [HTTP method] [path]: success (expected status [code])
  - [HTTP method] [path]: validation error (expected status 400)
  - [HTTP method] [path]: not found (expected status 404)
- Use MockMvc with jsonPath assertions
- Verify response body structure matches /specs/api/openapi/dsh-rest-api.yaml
- Follow patterns in /.github/copilot/rules/testing-patterns.md
```

---

## Integration Test

```
Generate a @SpringBootTest integration test for [FeatureName] in the DSH project.

Requirements:
- Test class name: [FeatureName]IT (integration test naming convention)
- Tag with @Tag("integration")
- Use Testcontainers for [MongoDB/Solr] dependencies
- Test the full flow: [describe end-to-end scenario]
- Clean up test data in @AfterEach
- Reference acceptance criteria in /specs/requirements/acceptance-criteria/[feature].md
- Use @SpringBootTest(webEnvironment = RANDOM_PORT)
```

---

## Repository Test

```
Generate a Spring Data repository test for [RepositoryName] in the DSH project.

Requirements:
- Use @DataMongoTest or @DataJpaTest slice context
- Use Testcontainers for the database
- Test cases:
  - Save and retrieve entity
  - Custom query method: [method name] with [scenario]
  - Edge case: [describe edge case]
- Use AssertJ assertions
- Include @AfterEach cleanup
```

---

## Performance Test

```
Generate a performance test for [OperationName] in the DSH project.

Requirements:
- Tag with @Tag("performance")
- Measure execution time using [JUnit 5 @Timeout / Micrometer]
- Target: complete [operation] within [X] ms for [input size]
- Test with dataset from /dsh-test-dataset/
- Reference benchmark targets in /specs/testing/performance-benchmarks/
- Log results for CI reporting
```
