#!/bin/bash

# Render Frontend Build Script
echo "🚀 Building Amazon Agentic Workstation Frontend..."

# Navigate to frontend directory
cd frontend

# Install Flutter dependencies
echo "📦 Installing Flutter dependencies..."
flutter pub get

# Build for web with optimizations
echo "🔨 Building Flutter web application..."
flutter build web \
  --web-renderer html \
  --release \
  --no-tree-shake-icons \
  --base-href="/"

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Frontend build successful!"
    echo "📁 Build artifacts located in: frontend/build/web"
else
    echo "❌ Frontend build failed!"
    exit 1
fi