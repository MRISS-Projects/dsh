---
name: User Story
about: An INVEST-framed story ready to be specced and built
title: "[STORY] "
labels: story
assignees: ''
---

## Story

**As a** <role>
**I want** <capability>
**So that** <benefit>

## Context

<!-- Which PRD wave does this belong to? Link the wave in specs/product/PRD.md. -->

- Wave:
- Related ADR / spec:

## Acceptance Criteria

<!-- Each one must be checkable by a test. "Works well" is not a criterion. -->

- [ ] AC001:
- [ ] AC002:

## INVEST check

- [ ] **Independent** - buildable without waiting on another open story
- [ ] **Negotiable** - states the need, not a prescribed implementation
- [ ] **Valuable** - the beneficiary is named above
- [ ] **Estimable** - the work is knowable; no spike needed first
- [ ] **Small** - fits one task branch, days not weeks
- [ ] **Testable** - every AC above is checkable by a test

## Affected Modules

- [ ] dsh-rest-api
- [ ] dsh-doc-analyser
- [ ] dsh-doc-indexer-worker
- [ ] dsh-data
- [ ] dsh-solr (replacement proposed - see ADR-001)
- [ ] CI / build

## Out of Scope

<!-- Explicitly list what this story will NOT include. -->
