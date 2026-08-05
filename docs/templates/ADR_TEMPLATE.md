# ADR_TEMPLATE.md

# Architecture Decision Record Template

Copy this file to `docs/adr/ADR-XXX-<slug>.md` for a new decision. Number ADRs sequentially.

Use an ADR only for decisions that are long-lasting and hard to reverse — for example, architecture style, adopting or excluding a technology, aggregate boundary changes, or a transaction/consistency strategy. Do not write an ADR for routine implementation details; a task document (`docs/templates/TASK_TEMPLATE.md`) or a review record is enough for those.

---

# ADR-XXX: <Decision title>

## Status

`Proposed` / `Accepted` / `Rejected` / `Superseded by ADR-YYY`

An ADR is drafted as a proposal by an Architecture Agent. It only becomes `Accepted` once a human approves it — see `AGENTS.md` § AI Roles.

## Date

YYYY-MM-DD

## Context

The situation and forces that make this decision necessary — what problem it solves, which documents (`PRD.md` / `REQUIREMENTS.md` / `DOMAIN_MODEL.md` / `ARCHITECTURE.md`) it is grounded in, and what constraints apply.

## Decision

The decision actually made, stated clearly and concretely.

## Decision Drivers

The factors that most influenced the choice (e.g., team size, current scale, an existing principle from `AGENTS.md` or `ARCHITECTURE.md`).

## Considered Alternatives

For each alternative:

### <Alternative name>

- Description
- Why it was rejected — or deferred, and where the deferral is tracked (e.g., `ARCHITECTURE.md` § Deferred Decisions)

## Consequences

### Positive

...

### Negative

...

### Risks

...

## Validation

How and when this decision will be revisited — a metric, a trigger event, or a scheduled review. Write "Not applicable" if there is none.

## Related Documents

Links to the `ARCHITECTURE.md` / `DOMAIN_MODEL.md` sections, tasks, or other ADRs this decision relates to.
