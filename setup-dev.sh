#!/bin/bash

# Local Development Setup Script
# This script sets up the development environment for testing the single service approach

echo "🛠️  Setting up local development environment..."

# Build Flutter for development
echo "📱 Building Flutter for development..."
cd frontend
flutter pub get
flutter build web --release --base-href="/"

# Copy to Spring Boot static resources
echo "📋 Setting up static resources..."
cd ../backend
mkdir -p src/main/resources/static
rm -rf src/main/resources/static/*
cp -r ../frontend/build/web/* src/main/resources/static/

echo "✅ Development setup complete!"
echo ""
echo "To run locally:"
echo "1. cd backend"
echo "2. ./mvnw spring-boot:run"
echo ""
echo "Then access:"
echo "- Frontend: http://localhost:8080/"
echo "- API: http://localhost:8080/api/"
echo "- Health: http://localhost:8080/actuator/health"