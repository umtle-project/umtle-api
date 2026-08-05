# REVIEW_TEMPLATE.md

# Review Record Template

Copy this file to `docs/reviews/REVIEW-<task-or-change>-<date>.md` when recording a design or code review. It supports the procedure defined in `AGENTS.md` § Review Mode and `docs/DEVELOPMENT.md` § Code Review Workflow.

Only record findings backed by a realistic failure scenario. Do not record speculative concerns or pure style preferences — see "Findings" below.

---

## Review Target

What is being reviewed (PR/branch, task, or design document), with a link or path.

## Review Scope

What this review does and does not cover (e.g., "logic and data integrity of the Attendance API; UI is out of scope").

## Baseline Documents

The documents this review checks the change against — e.g. the relevant `docs/tasks/TASK-XXX-*.md`, `docs/DOMAIN_MODEL.md`, `docs/ARCHITECTURE.md`, `docs/USER_ROLES.md`, related ADRs.

## Reviewing Role

Confirms this review is performed by the Review Agent role, independently of the agent/session that implemented the change under review — see `AGENTS.md` § AI Roles.

## Verification Performed

What was actually checked — tests run, manual checks, static analysis — and the result.

## Findings

List each finding once, most severe first. If a category has nothing, write "None."

### Critical

### Major

### Minor

Each finding must include:

- **Location** — file and line/section
- **Failure scenario** — the concrete input or state that leads to a wrong result or defect, not a hypothetical "this could be an issue"
- **Recommended fix**

## Resolution

For each finding: whether it was fixed, and if not, why it was left as-is (e.g., accepted risk, deferred to a follow-up task).

## Remaining Risks

Any risk that remains after this review, whether or not it is tied to a specific finding.

## Final Conclusion

Approve / Request changes / Reject. This is the Review Agent's recommendation — the merge decision itself is made by a human, per `AGENTS.md`.
