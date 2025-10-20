@echo off
REM Migration script to add unique constraint to policy_actions table

echo ========================================
echo Policy Actions - Add Unique Constraint
echo ========================================
echo.

REM Set PostgreSQL connection parameters
set PGHOST=localhost
set PGPORT=5432
set PGUSER=postgres
set PGDATABASE=agenticworkstation

REM Prompt for password
echo Please enter PostgreSQL password for user 'postgres':
set /p PGPASSWORD=

echo.
echo Running migration to add unique constraint...
echo.

REM Run the migration SQL script
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -f "%~dp0migration_add_unique_constraint.sql"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Migration completed successfully!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo Migration failed! Please check errors above.
    echo ========================================
)

echo.
pause
