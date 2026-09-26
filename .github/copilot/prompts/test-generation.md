# Test Generation Prompts

Use these prompt templates with GitHub Copilot Chat to generate consistent tests for DSH components.

---

## Unit Test for Service

```text
As a Backend Developer (see /.github/roles.md), generate JUnit 4 unit tests for
[ServiceName] in the DSH project.

Requirements:
- Test class name: [ServiceName]Test
- No Spring context: no @SpringBootTest, no slice annotation, no @MockBean
  (see "Unit vs integration" in testing-patterns.md)
- Follow Arrange/Act/Assert pattern
- Use Mockito (@Mock, @InjectMocks) to mock: [list dependencies]
- Test cases:
  - Happy path: [describe expected success scenario]
  - Validation failure: [describe invalid input scenario]
  - External dependency failure: [describe error propagation]
- Use AssertJ for assertions
- Follow patterns in /.github/copilot/rules/testing-patterns.md
- Cover acceptance criteria from /specs/requirements/acceptance-criteria/[feature].md
```

---

## Controller Unit Test

```text
As a Backend Developer (see /.github/roles.md), generate unit tests for
[ControllerName] in the DSH project.

Requirements:
- Test class name: [ControllerName]Test
- No Spring context: Mockito over the controller (@Mock services, @InjectMocks controller),
  calling its methods directly
- Cover every branch of every endpoint method
- Follow patterns in /.github/copilot/rules/testing-patterns.md
```

---

## Controller Slice Test

```text
As a Backend Developer (see /.github/roles.md), generate @WebMvcTest tests for
[ControllerName] in the DSH project.

Requirements:
- Test class name: [ControllerName]IT, in the module's integration package
  (a slice starts a Spring context, so it is an integration test)
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

```text
As a Backend Developer (see /.github/roles.md), generate a @SpringBootTest
integration test for [FeatureName] in the DSH project.

Requirements:
- Test class name: [FeatureName]IT, in the module's integration package
  (e.g. com.mriss.dsh.restapi.integration); the name is the selector, no tag is needed
- Test the full flow: [describe end-to-end scenario]
- Clean up test data in @After
- Reference acceptance criteria in /specs/requirements/acceptance-criteria/[feature].md
- Use @SpringBootTest(webEnvironment = RANDOM_PORT)
```

---

## Repository Test

```text
As a Backend Developer (see /.github/roles.md), generate a Spring Data repository
test for [RepositoryName] in the DSH project.

Requirements:
- Test class name: [RepositoryName]IT, in the module's integration package
  (a slice starts a Spring context, so it is an integration test)
- Use @DataMongoTest or @DataJpaTest slice context
- Test cases:
  - Save and retrieve entity
  - Custom query method: [method name] with [scenario]
  - Edge case: [describe edge case]
- Use AssertJ assertions
- Include @After cleanup
```

---

## Performance Test

```text
As a Backend Developer (see /.github/roles.md), generate a performance test for
[OperationName] in the DSH project.

Requirements:
- Mark with a JUnit 4 @Category(PerformanceTest.class)
- Measure execution time using [JUnit 4 @Test(timeout = ...) / Micrometer]
- Target: complete [operation] within [X] ms for [input size]
- Test with dataset from /dsh-test-dataset/
- Reference benchmark targets in /specs/testing/performance-benchmarks/
- Log results for CI reporting
```
