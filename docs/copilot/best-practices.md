# GitHub Copilot Best Practices for DSH

## General Principles

### Be Specific
The more context you provide, the better Copilot's suggestions. Always reference:
- The relevant spec file (`/specs/features/...`)
- The applicable convention file (`/.github/copilot/rules/...`)
- The target module and package

### Review Every Suggestion
Copilot suggestions are a starting point, not a final answer. Always:
- Verify the generated code compiles and tests pass
- Check that it matches the specification
- Review for security issues (SQL injection, sensitive data exposure, etc.)
- Ensure it follows the conventions in `/.github/copilot/rules/`

### Keep Specs Up to Date
Copilot's quality improves when specs are accurate and current:
- Update `specs/features/` when implementing new functionality
- Keep `specs/api/openapi/dsh-rest-api.yaml` in sync with the implementation
- Add JavaDoc that references spec files so Copilot has in-code context

---

## Effective Prompting

### Use Structured Prompts
Copy templates from `.github/copilot/prompts/` and fill in the blanks. Structured prompts produce more consistent, higher-quality output.

### Provide Counter-Examples
Tell Copilot what you **don't** want:
```
Generate a service WITHOUT field injection (@Autowired on fields).
Use constructor injection only.
```

### Iterate in Steps
For complex components, generate incrementally:
1. Generate the class skeleton (fields, constructor, empty methods)
2. Generate each method individually with specific instructions
3. Generate the test class separately

### Attach Relevant Files
In Copilot Chat, use `@workspace` and attach spec files to give Copilot additional context:
```
@workspace Based on /specs/features/smart-highlighting.md and
/specs/api/openapi/dsh-rest-api.yaml, generate...
```

---

## Security

- Never ask Copilot to generate code that embeds credentials, API keys, or secrets
- Review generated SQL/MQL queries for injection vulnerabilities
- Ensure generated authentication/authorisation code is reviewed by a senior developer
- Do not accept Copilot suggestions for cryptographic implementations without expert review

---

## Team Conventions

| Role | Primary Copilot Use Cases |
|------|--------------------------|
| Product Manager | Generate feature specs, acceptance criteria, issue descriptions |
| Backend Developer | Generate services, controllers, tests, OpenAPI schemas |
| Frontend Developer | Generate UI components (once tech stack is decided) |
| DevOps Developer | Generate GitHub Actions workflows, Maven configurations |

---

## Troubleshooting

**Copilot suggestions are off-topic or incorrect:**
- Ensure `.github/copilot-instructions.md` is not empty
- Try attaching the relevant spec file explicitly in Copilot Chat
- Use a more specific prompt from `.github/copilot/prompts/`

**Generated code doesn't compile:**
- Check that all referenced classes exist in the current module
- Verify package names match the conventions in `/.github/copilot/rules/java-conventions.md`
- Run `mvn compile` to surface errors early

**Generated tests are too shallow:**
- Specify the exact test cases you want (happy path, edge cases, error scenarios)
- Reference the acceptance criteria from `specs/requirements/acceptance-criteria/`
