@echo off
echo Building Amazon Agentic Workstation Docker Image
echo ==================================================

echo Building Docker image...
docker build -t agentic-workstation:latest .

if %errorlevel% neq 0 (
    echo Docker build failed!
    exit /b 1
)

echo Docker build successful!
echo.
echo To test locally, run:
echo   docker run -p 8080:8080 agentic-workstation:latest
echo.
echo Or use docker-compose:
echo   docker-compose up