#!/bin/bash

# Single Service Build Script for Render Deployment
# This script builds Flutter web and copies it to Spring Boot static resources

set -e  # Exit on any error

echo "🚀 Building Amazon Agentic Workstation (Single Service)"
echo "================================================="

# Step 1: Build Flutter Web App
echo "📱 Building Flutter web application..."
cd frontend
flutter pub get
flutter build web --release --base-href="/"

if [ $? -eq 0 ]; then
    echo "✅ Flutter build successful!"
else
    echo "❌ Flutter build failed!"
    exit 1
fi

# Step 2: Create static resources directory in Spring Boot
echo "📁 Preparing Spring Boot static resources..."
cd ../backend
mkdir -p src/main/resources/static

# Step 3: Copy Flutter build to Spring Boot static resources
echo "📋 Copying Flutter build to Spring Boot..."
rm -rf src/main/resources/static/*
cp -r ../frontend/build/web/* src/main/resources/static/

if [ $? -eq 0 ]; then
    echo "✅ Flutter files copied successfully!"
else
    echo "❌ Failed to copy Flutter files!"
    exit 1
fi

# Step 4: Build Spring Boot application
echo "🔨 Building Spring Boot application..."
chmod +x mvnw
./mvnw clean package -DskipTests

if [ $? -eq 0 ]; then
    echo "✅ Spring Boot build successful!"
    echo "🎉 Single service build completed!"
    echo ""
    echo "📦 Build artifacts:"
    echo "   - Spring Boot JAR: backend/target/agenticworkstation-0.0.1-SNAPSHOT.jar"
    echo "   - Flutter web files: backend/src/main/resources/static/"
    echo ""
    echo "🚀 Ready for deployment!"
else
    echo "❌ Spring Boot build failed!"
    exit 1
fi