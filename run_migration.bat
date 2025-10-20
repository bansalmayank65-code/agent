@echo off
REM Migration script to create policy_actions table in PostgreSQL
REM Update the connection parameters below according to your database setup

SET PGHOST=localhost
SET PGPORT=5432
SET PGDATABASE=agenticworkstation
SET PGUSER=postgres

echo Running migration to create policy_actions table...
echo.

psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -f backend\src\main\resources\migration_policy_actions.sql

echo.
echo Migration completed! Press any key to exit...
pause > nul
