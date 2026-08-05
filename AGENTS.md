# AGENTS.md

# Umtle AI Development Guide

## Purpose

This document defines the project-wide rules that **all AI agents** must follow when working on the Umtle project.

These rules apply regardless of the AI tool being used (Claude Code, Codex, Cursor, Gemini CLI, etc.) and regardless of which role that tool is currently performing.

---

# Project Overview

Umtle is a web-based academy management platform that connects administrators, teachers, students, and parents.

This repository contains the Kotlin + Spring Boot backend API.

---

# AI Roles

AI work on this project is organized by **role**, not by which AI product performs it. Any AI tool or model (Claude, Codex, Gemini, Cursor, etc.) may perform any of the roles below, depending on the situation. Tool-specific configuration files (such as `CLAUDE.md`) describe how a given tool operates within these roles — they do not define separate rules.

- **Planning Agent** — Analyzes requirements and breaks work into scoped units (see `docs/tasks/`). Defines scope and completion criteria. Does not implement code.
- **Architecture Agent** — Evaluates structural and technical decisions against `docs/ARCHITECTURE.md` and `docs/DOMAIN_MODEL.md`, and drafts proposals (including ADRs under `docs/adr/`) when a decision is needed. Its output is a proposal; it does not unilaterally finalize architectural decisions.
- **Implementation Agent** — Implements code and tests within the scope defined by a task document. Does not give final approval to its own output.
- **Review Agent** — Independently reviews an implementation's output against project documentation, following the review procedure in "Review Mode" below. The agent or session that implemented a change must not also act as the Review Agent for that same change.
- **Refactoring Agent** — Improves existing code structure within documented scope, without changing external behavior.

A single AI tool may move between roles across a work session (for example, planning a task and then implementing it), but **the same session must not both implement a change and act as its Review Agent.** Review of a change must happen in an independent agent or session.

---

# Operating Principles

- AI works from project documentation, not from inference alone (see "Documentation Priority" below).
- The agent that implements a change does not give final approval to its own work.
- Review of a change is performed by an independent agent, separate from the one that implemented it.
- Final technical decisions are always made by a human.
- Project documentation takes precedence over AI reasoning.
- When documentation and code conflict, report the conflict — do not silently resolve it by rewriting documentation (see "Documentation Changes" below).

---

# Documentation Priority

When making implementation or design decisions, always follow the documents in the following order.

1. `docs/PRD.md`
2. `docs/REQUIREMENTS.md`
3. `docs/USER_ROLES.md`
4. `docs/DOMAIN_MODEL.md`
5. `docs/ARCHITECTURE.md`
6. `docs/DECISIONS.md`

Lower-priority documents must never contradict higher-priority documents.

If ambiguity affects business behavior, permissions, data integrity,
public APIs, or architecture, do not invent requirements or policies.
Ask for clarification before implementation.

For minor implementation details that do not affect externally observable behavior,
follow existing codebase conventions and clearly report the assumption.

Use each document according to its responsibility:

- `docs/PRD.md`: product goals, product scope, and excluded scope
- `docs/REQUIREMENTS.md`: functional behavior and acceptance criteria
- `docs/USER_ROLES.md`: roles, permissions, and access rules
- `docs/DOMAIN_MODEL.md`: domain concepts, invariants, and aggregate boundaries
- `docs/ARCHITECTURE.md`: technical structure and implementation principles
- `docs/DECISIONS.md`: summary of accepted technical decisions
- `docs/adr/`: detailed architectural decision records
- `docs/tasks/`: scope and completion criteria for individual work items

After reviewing the core documents, also read:

1. The relevant document under `docs/tasks/`, if one exists
2. Relevant records under `docs/adr/`, if any exist

Task documents define the scope of an individual unit of work.
ADR documents define accepted architectural and technical decisions.

A task document must not override product requirements, permissions,
domain rules, or accepted architectural decisions.

---

# Development Principles

- Keep the architecture as simple as possible.
- Prefer maintainability over premature extensibility.
- Follow the existing project structure before introducing new patterns.
- Avoid unnecessary abstractions.
- Design around the domain, not the framework.
- Write code that is easy for both humans and AI to understand.

---

# Implementation Principles

These principles apply whenever an AI is acting as the Implementation Agent.

- Implement features only within the scope defined by the project documentation.
- Prefer extending the existing design over creating new structures.
- Keep changes small and easy to review.
- Add new dependencies only when they provide clear value.
- Design for testability.
- Keep documentation and implementation consistent.

---

# Documentation Changes

Do not modify product requirements or business rules merely to match
the current implementation.

When implementation and documentation conflict:

1. Stop the conflicting implementation.
2. Report the conflicting documents or code.
3. Do not resolve the conflict by silently changing documentation.
4. Update requirements or business-rule documents only when the task
   explicitly includes that change or the user approves the decision.

Implementation changes must follow the documentation.
Documentation must not be rewritten afterward solely to justify an implementation.

---

# Review Mode

This section defines how the Review Agent operates. It must be performed independently of the session or agent that implemented the change under review — ideally in a separate session, and ideally by a different AI tool.

When explicitly asked to review code or a diff:

- Do not modify files unless fixes are explicitly requested.
- Review the change against the relevant requirements, task documents,
  domain rules, and architectural decisions.
- Prioritize correctness over formatting preferences.
- Check permissions, transactions, data integrity, concurrency,
  performance, exception handling, and missing tests.
- Classify findings as Critical, Major, or Minor.
- Include the relevant file location, failure scenario, and recommended correction.
- Do not report speculative issues without a realistic failure scenario.

---

# Validation

Before completing a task:

- Run relevant Gradle tests whenever possible.
- If tests cannot be executed, clearly explain why.
- Report known limitations or remaining risks.
- Ensure documentation changes do not conflict with higher-priority documents.

A task is complete only when:

- The requested behavior is implemented.
- Relevant tests are added or updated.
- Relevant tests are executed whenever possible.
- Documentation affected by the change is updated.
- Changed files and validation results are summarized.
- Remaining risks or unverified behavior are reported.

---

# Prohibited

- Do not introduce undocumented business rules.
- Do not contradict existing project documentation.
- Do not refactor unrelated code without a clear reason.
- Do not sacrifice readability for cleverness.
- Do not act as the Review Agent for a change you implemented in the same session.
- Do not treat an agent's own output as final approval; final technical decisions are made by a human.

---

# Related Documents

- `docs/DEVELOPMENT.md` defines the Git workflow, branch strategy, PR process, and how the roles above map onto day-to-day development steps.
- `docs/templates/` holds the templates for task documents, ADRs, and review records (`TASK_TEMPLATE.md`, `ADR_TEMPLATE.md`, `REVIEW_TEMPLATE.md`).
- `docs/prompts/` holds reusable prompts for starting an Implementation Agent or Review Agent session (`IMPLEMENT_TASK.md`, `REVIEW_CHANGE.md`).
- `docs/reviews/` holds completed review records produced by the Review Agent.