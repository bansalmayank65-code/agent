@echo off
REM Migration script to add performance indexes to policy_actions table

echo ========================================
echo Policy Actions - Add Performance Indexes
echo ========================================
echo.
echo This will add composite indexes to optimize query performance:
echo - env_name + interface_num + last_updated_at (DESC)
echo - env_name + interface_num + policy_cat1
echo - env_name + interface_num + policy_cat1 + policy_cat2
echo - env_name + interface_num + policy_cat1 + last_updated_at (DESC)
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
echo Running migration to add performance indexes...
echo This may take a few moments depending on table size.
echo.

REM Run the migration SQL script
psql -h %PGHOST% -p %PGPORT% -U %PGUSER% -d %PGDATABASE% -f "%~dp0migration_add_performance_indexes.sql"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Migration completed successfully!
    echo Queries should now be much faster.
    echo ========================================
) else (
    echo.
    echo ========================================
    echo Migration failed! Please check errors above.
    echo ========================================
)

echo.
pause
