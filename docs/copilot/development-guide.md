# GitHub Copilot Development Guide

## Introduction
This guide explains how to use GitHub Copilot effectively in the DSH (Document Smart Highlights) project. It covers setup, workflow integration, and how to leverage the spec-driven structure for optimal code generation.

## Setup

### Repository Configuration
The DSH project is pre-configured for Copilot via `.github/copilot-instructions.md`. This file provides Copilot with:
- Project architecture overview
- Module responsibilities
- File references to specifications
- Code generation preferences

Copilot will automatically use this context when suggesting code in this repository.

### Recommended VS Code Extensions
- **GitHub Copilot** – AI pair programmer
- **GitHub Copilot Chat** – Conversational Copilot interface
- **Extension Pack for Java** – Java language support
- **Spring Boot Extension Pack** – Spring Boot tooling

## Workflow

### Spec-First Development

1. **Read the specification** – Before writing code, read the relevant spec in `specs/features/`
2. **Check the OpenAPI spec** – For API work, review `specs/api/openapi/dsh-rest-api.yaml`
3. **Use prompt templates** – Copy a template from `.github/copilot/prompts/` to Copilot Chat
4. **Generate and review** – Generate code with Copilot and review against the spec
5. **Add tests** – Use test generation prompts from `.github/copilot/prompts/test-generation.md`

### Copilot Chat Workflow

Open Copilot Chat (`Ctrl+Shift+I`) and attach relevant spec files as context:

```
@workspace /specs/features/document-analysis.md
Generate a Spring Boot service for document analysis following the conventions in
/.github/copilot/rules/java-conventions.md
```

### Inline Completion Tips

- Write descriptive method signatures and JavaDoc before the body – Copilot uses this as context
- Name variables clearly according to the domain (e.g., `analysisResult`, `highlightCollection`)
- Start with the happy path; then ask Copilot for error handling scenarios

## Available Prompt Templates

See `.github/copilot/prompts/` for ready-to-use prompt templates:

| Template | Use Case |
|----------|---------|
| `component-generation.md` | Scaffold services, controllers, DTOs, repositories |
| `test-generation.md` | Generate unit, integration, and performance tests |
| `documentation-generation.md` | Generate JavaDoc, feature specs, OpenAPI schemas |

## Best Practices
See `./best-practices.md` for detailed Copilot usage best practices.

## Example Sessions
See `./prompt-examples.md` for real example prompts and their expected outputs.
