# Flyway Migrations

Flyway applies SQL migrations placed in this directory on application startup.

- Naming: `V<version>__<description>.sql` (e.g. `V1__create_student_table.sql`).
- Migrations are additive and forward-only — never edit or delete a migration that has already been applied to any shared environment.
- No domain schema has been defined yet, so this directory currently has no migration files. Add the first migration together with the task that introduces the corresponding domain/table, following `docs/DOMAIN_MODEL.md` and `docs/ARCHITECTURE.md`.
