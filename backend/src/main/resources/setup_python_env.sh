#!/bin/bash
# Setup script to install Python dependencies for notebook execution

echo "🐍 Setting up Python environment for notebook execution..."

# Check if Python is available
if ! command -v python &> /dev/null; then
    if ! command -v python3 &> /dev/null; then
        echo "❌ Python is not installed or not in PATH"
        echo "Please install Python 3.8+ and ensure it's available as 'python' or 'python3'"
        exit 1
    else
        echo "✅ Using python3"
        PYTHON_CMD="python3"
    fi
else
    echo "✅ Using python"
    PYTHON_CMD="python"
fi

# Check Python version
PYTHON_VERSION=$($PYTHON_CMD --version 2>&1 | cut -d' ' -f2 | cut -d'.' -f1-2)
echo "📋 Python version: $PYTHON_VERSION"

# Install pip if not available
if ! $PYTHON_CMD -m pip --version &> /dev/null; then
    echo "❌ pip is not available"
    echo "Please install pip for Python package management"
    exit 1
fi

echo "✅ pip is available"

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
REQUIREMENTS_FILE="$SCRIPT_DIR/requirements.txt"

if [ ! -f "$REQUIREMENTS_FILE" ]; then
    echo "❌ requirements.txt not found at: $REQUIREMENTS_FILE"
    exit 1
fi

echo "📦 Installing Python packages from requirements.txt..."
$PYTHON_CMD -m pip install -r "$REQUIREMENTS_FILE"

if [ $? -eq 0 ]; then
    echo "✅ Successfully installed all Python dependencies"
    echo ""
    echo "🧪 Testing notebook execution capabilities..."
    
    # Test if nbclient is working
    $PYTHON_CMD -c "
import sys
try:
    from nbclient import NotebookClient
    from nbformat import read, write, NO_CONVERT
    print('✅ Notebook execution libraries imported successfully')
except ImportError as e:
    print(f'❌ Import error: {e}')
    sys.exit(1)
"
    
    if [ $? -eq 0 ]; then
        echo "🎉 Setup complete! The TauBenchValidationService can now execute notebooks."
    else
        echo "❌ Setup failed - notebook libraries are not working properly"
        exit 1
    fi
else
    echo "❌ Failed to install Python dependencies"
    exit 1
fi