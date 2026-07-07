# Database

This directory contains documentation and seed scripts for the database.
Flyway migration scripts are located in the backend module at:
`backend/src/main/resources/db/migration/`

## Flyway Migrations

- Migration files must be named with the pattern: `V<VERSION>__<DESCRIPTION>.sql`
  Example: `V1__create_users.sql`, `V2__add_index_to_events.sql`
- The version number must be sequential and unique.
- Use double underscores (`__`) between the version and description.
- Never modify a migration file that has already been applied to a database.
  If you need to change a migration, create a new version instead.

## Where Migrations Live

The actual SQL migration scripts are managed by Flyway and stored in:
`backend/src/main/resources/db/migration/`

This `database/` directory at the repository root is intended for:
- Database documentation
- Seed data scripts (not managed by Flyway)
- Backup/restore procedures
- Schema diagrams

Do not place Flyway migration scripts in this directory.
