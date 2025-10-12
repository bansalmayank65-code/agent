@echo off
echo Building Amazon Agentic Workstation (Single Service)
echo =================================================

echo Building Flutter web application...
cd frontend
call flutter pub get
call flutter build web --release --base-href="/"

if %errorlevel% neq 0 (
    echo Flutter build failed!
    exit /b 1
)

echo Flutter build successful!

echo Preparing Spring Boot static resources...
cd ..\backend
if not exist "src\main\resources\static" mkdir "src\main\resources\static"

echo Copying Flutter build to Spring Boot...
if exist "src\main\resources\static\*" del /q /s "src\main\resources\static\*"
xcopy "..\frontend\build\web\*" "src\main\resources\static\" /s /e /y

if %errorlevel% neq 0 (
    echo Failed to copy Flutter files!
    exit /b 1
)

echo Flutter files copied successfully!

echo Building Spring Boot application...
call mvnw.cmd clean package -DskipTests

if %errorlevel% neq 0 (
    echo Spring Boot build failed!
    exit /b 1
)

echo Single service build completed successfully!
echo.
echo Build artifacts:
echo - Spring Boot JAR: backend\target\agenticworkstation-0.0.1-SNAPSHOT.jar
echo - Flutter web files: backend\src\main\resources\static\
echo.
echo Ready for deployment!