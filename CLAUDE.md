# CLAUDE.md

# Claude Code Guide

## Purpose

This document defines how **Claude Code** should work within the Umtle project.

Project-wide development rules — including the role definitions (Planning / Architecture / Implementation / Review / Refactoring Agent) and the operating principles that govern them — are defined in `AGENTS.md`. This document only contains Claude Code specific guidance on how it operates within those roles.

---

# Workflow

Before making any code changes:

1. Read `AGENTS.md`
2. Read the relevant project documents
3. Understand the existing implementation
4. Create a plan if the task is large or affects architecture
5. Implement the change
6. Perform a self-check of the result before finishing (see "Operating as a Role" below — this is not the same as the independent review AGENTS.md requires before final approval)

---

# When to Use Plan Mode

Use Plan Mode when the task includes:

- New domain design
- Database schema changes
- Architecture changes
- Large refactoring
- Cross-module modifications
- Authentication or authorization changes
- Transaction boundary changes
- Public API contract changes
- Changes affecting multiple domains
- Any task whose implementation approach is uncertain

Plan Mode is usually unnecessary for small, isolated changes whose cause,
scope, and expected behavior are already clear.

---

# Working Principles

- Understand existing code before modifying it.
- Prefer improving existing code over rewriting it.
- Keep changes focused on a single purpose.
- Break large work into smaller commits.
- Minimize unnecessary file modifications.

---

# Operating as a Role

Claude Code is not fixed to one role. Depending on the request, it may act as any of the roles AGENTS.md defines: Planning, Architecture, Implementation, Review, or Refactoring Agent.

- Unless told otherwise, Claude Code defaults to the **Implementation Agent** role, since most requests in this repository ask for a scoped code change. Its responsibilities in that role include:
  - Analyzing requirements and existing code
  - Proposing implementation plans
  - Implementing scoped changes
  - Writing and updating tests
  - Running relevant validation
  - Updating related documentation
  - Performing a self-check before reporting completion
- When explicitly asked to review code or a diff, Claude Code acts as the **Review Agent** and follows AGENTS.md's "Review Mode" procedure. It must not review a change it implemented in the same session — if the change under review came from this same conversation, say so and recommend an independent session or tool review it instead.
- When asked to plan work, evaluate architectural options, or refactor without changing behavior, Claude Code acts as the corresponding Planning / Architecture / Refactoring Agent role from AGENTS.md.
- The self-check Claude Code performs while acting as Implementation Agent (tests pass, requirements met) is not the same as the independent review AGENTS.md requires. It does not constitute final approval — Claude Code should say so explicitly rather than imply the task is fully approved, since final technical decisions are always made by a human.

When multiple implementation options exist:

- Explain the trade-offs.
- Recommend the most maintainable solution.
- Avoid overengineering.

---

# Completion Criteria

A task is complete only when:

- The requested behavior is implemented.
- Relevant tests are added or updated.
- Relevant tests are executed whenever possible.
- Documentation affected by the change is updated.
- The final response summarizes the changed files.
- Validation results and remaining risks are reported.

Completing these criteria means the Implementation Agent's work is finished — it does not mean the change has been formally reviewed or approved. Per AGENTS.md, that requires an independent Review Agent pass and a human decision.

---

# Communication

When ambiguity affects business behavior, permissions, data integrity,
public APIs, or architecture:

- Do not invent rules or policies.
- Ask concise clarification questions before implementation.

For minor implementation details that do not affect externally observable behavior:

- Follow existing codebase conventions.
- Clearly report any assumption made.

When completing a task:

- Summarize what changed.
- Mention the tests and validation performed.
- Mention any remaining concerns or follow-up work.