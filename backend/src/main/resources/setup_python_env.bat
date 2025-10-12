@echo off
REM Setup script to install Python dependencies for notebook execution

echo 🐍 Setting up Python environment for notebook execution...

REM Check if Python is available
python --version >nul 2>&1
if %errorlevel% neq 0 (
    python3 --version >nul 2>&1
    if %errorlevel% neq 0 (
        echo ❌ Python is not installed or not in PATH
        echo Please install Python 3.8+ and ensure it's available as 'python' or 'python3'
        exit /b 1
    ) else (
        echo ✅ Using python3
        set PYTHON_CMD=python3
    )
) else (
    echo ✅ Using python
    set PYTHON_CMD=python
)

REM Check Python version
for /f "tokens=2" %%i in ('%PYTHON_CMD% --version 2^>^&1') do set PYTHON_VERSION=%%i
echo 📋 Python version: %PYTHON_VERSION%

REM Check if pip is available
%PYTHON_CMD% -m pip --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ pip is not available
    echo Please install pip for Python package management
    exit /b 1
)

echo ✅ pip is available

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0
set REQUIREMENTS_FILE=%SCRIPT_DIR%requirements.txt

if not exist "%REQUIREMENTS_FILE%" (
    echo ❌ requirements.txt not found at: %REQUIREMENTS_FILE%
    exit /b 1
)

echo 📦 Installing Python packages from requirements.txt...
%PYTHON_CMD% -m pip install -r "%REQUIREMENTS_FILE%"

if %errorlevel% equ 0 (
    echo ✅ Successfully installed all Python dependencies
    echo.
    echo 🧪 Testing notebook execution capabilities...
    
    REM Test if nbclient is working
    %PYTHON_CMD% -c "import sys; from nbclient import NotebookClient; from nbformat import read, write, NO_CONVERT; print('✅ Notebook execution libraries imported successfully')" 2>nul
    
    if %errorlevel% equ 0 (
        echo 🎉 Setup complete! The TauBenchValidationService can now execute notebooks.
    ) else (
        echo ❌ Setup failed - notebook libraries are not working properly
        exit /b 1
    )
) else (
    echo ❌ Failed to install Python dependencies
    exit /b 1
)