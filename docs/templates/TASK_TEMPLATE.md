# TASK_TEMPLATE.md

# Task Specification Template

Copy this file to `docs/tasks/TASK-XXX-<slug>.md` and fill in each section for a specific unit of work. `TASK-XXX` is a sequential ID (`TASK-001`, `TASK-002`, ...).

For a small or simple task, remove sections that do not apply — for example, a task with no schema change does not need "Database Impact". Keep "Scope", "Acceptance Criteria", and "Definition of Done" in every task regardless of size.

---

## Task ID and Title

`TASK-XXX: <short title>`

## Status

One of: `Draft` / `Ready` / `In Progress` / `In Review` / `Done` / `Blocked`

## Purpose

What this task achieves and why it is needed, in one or two sentences.

## Background

The context that led to this task — a user request, a gap identified in `docs/REQUIREMENTS.md`, a follow-up from an ADR or a Deferred Decision in `docs/ARCHITECTURE.md`, etc.

## Related Documents and Requirement IDs

- `docs/REQUIREMENTS.md` section(s): ...
- `docs/DOMAIN_MODEL.md` section(s): ...
- Related ADRs (`docs/adr/`): ...

`docs/REQUIREMENTS.md` does not yet use formal requirement IDs — reference the section number/title until IDs are introduced.

## Users and Permissions

Which roles (Admin / Teacher / Student / Parent) this task affects, and what each may do. If `docs/USER_ROLES.md` does not yet define the permission this task needs, say so explicitly rather than deciding it here.

## Preconditions

What must already exist or already be true before this task can start (prior data, prior tasks, prior decisions).

## Scope

What this task implements. This defines what the Implementation Agent is authorized to build — be specific.

## Out of Scope

What this task explicitly does not do, even if related. Prevents scope creep during implementation.

## Functional Scenarios

The user-facing scenarios this task must support, e.g. "Given ..., when ..., then ...".

## Business Rules

Domain rules and invariants this task must enforce. Only include rules already established in `docs/DOMAIN_MODEL.md` / `docs/REQUIREMENTS.md`, or explicitly decided for this task — do not invent business rules here.

## API Changes

New or changed endpoints, request/response shape, and status codes. Omit if this task has no API-facing change.

## Domain Impact

Which domain concepts/aggregates from `docs/DOMAIN_MODEL.md` this task touches, and how.

## Database Impact

Schema changes this task requires (tables, columns, migrations). Omit if there is none.

## Exception and Error Handling

Expected error cases and how they should be handled or reported.

## Test Scenarios

The test cases that must exist for this task (happy path, edge cases, permission checks).

## Acceptance Criteria

The concrete, checkable conditions that determine whether this task's behavior is correct.

## Definition of Done

Reference `docs/DEVELOPMENT.md` § Definition of Done as the baseline. List anything specific to this task in addition to that baseline, if any.

## Open Questions

Anything still undecided. Do not resolve these by assumption — flag them per `AGENTS.md` § Documentation Priority.

## Related ADRs

Links to `docs/adr/ADR-XXX-*.md` that this task depends on or is affected by.
