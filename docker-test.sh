#!/bin/bash

# Docker Build and Test Script for Local Development

set -e

echo "🐳 Building Amazon Agentic Workstation Docker Image"
echo "=================================================="

# Build the Docker image
echo "📦 Building Docker image..."
docker build -t agentic-workstation:latest .

if [ $? -eq 0 ]; then
    echo "✅ Docker build successful!"
else
    echo "❌ Docker build failed!"
    exit 1
fi

# Test the image locally
echo "🧪 Testing Docker image..."
docker run --rm -d \
    --name agentic-test \
    -p 8080:8080 \
    -e SPRING_PROFILES_ACTIVE=dev \
    -e DATABASE_URL="jdbc:h2:mem:testdb" \
    agentic-workstation:latest

# Wait for application to start
echo "⏳ Waiting for application to start..."
sleep 30

# Health check
echo "🏥 Performing health check..."
if curl -f http://localhost:8080/actuator/health; then
    echo "✅ Health check passed!"
else
    echo "❌ Health check failed!"
    docker logs agentic-test
    docker stop agentic-test
    exit 1
fi

# Test frontend
echo "🎨 Testing frontend..."
if curl -f http://localhost:8080/; then
    echo "✅ Frontend accessible!"
else
    echo "❌ Frontend test failed!"
    docker logs agentic-test
    docker stop agentic-test
    exit 1
fi

# Cleanup
echo "🧹 Cleaning up..."
docker stop agentic-test

echo "🎉 All tests passed! Docker image is ready for deployment."
echo ""
echo "To run locally:"
echo "  docker run -p 8080:8080 agentic-workstation:latest"
echo ""
echo "Or use docker-compose:"
echo "  docker-compose up"