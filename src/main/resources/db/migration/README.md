# Flyway Migrations

Flyway applies SQL migrations placed in this directory on application startup.

- Naming: `V<version>__<description>.sql` (e.g. `V1__create_student_table.sql`).
- Migrations are additive and forward-only — never edit or delete a migration that has already been applied to any shared environment.
- `V1__create_students_table.sql` introduces the first domain table for TASK-002.
