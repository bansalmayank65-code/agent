#!/bin/bash

# Render Backend Deployment Script
echo "🚀 Starting Amazon Agentic Workstation Backend Deployment..."

# Set Java options for Render's memory limits
export JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
export MAVEN_OPTS="-Xmx512m -Xms256m"

# Navigate to backend directory
cd backend

# Clean and build the application
echo "📦 Building application..."
./mvnw clean package -DskipTests -Dmaven.javadoc.skip=true

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo "🎯 Starting application on port $PORT..."
    java $JAVA_OPTS -Dserver.port=$PORT -jar target/agenticworkstation-0.0.1-SNAPSHOT.jar
else
    echo "❌ Build failed!"
    exit 1
fi