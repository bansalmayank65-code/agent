# Flutter build stage - Use Ubuntu base for better compatibility
FROM ubuntu:22.04 AS flutter-builder

# Install dependencies
RUN apt-get update && apt-get install -y \
    curl \
    git \
    unzip \
    xz-utils \
    && rm -rf /var/lib/apt/lists/*

# Install Flutter
WORKDIR /opt
RUN curl -fsSL https://storage.googleapis.com/flutter_infra_release/releases/stable/linux/flutter_linux_3.24.3-stable.tar.xz | tar -xJ
ENV PATH="/opt/flutter/bin:$PATH"

# Fix Git ownership issue and set up Flutter
RUN git config --global --add safe.directory /opt/flutter && \
    git config --global --add safe.directory /opt/flutter/.pub-cache && \
    flutter config --enable-web --no-analytics && \
    flutter precache --web

WORKDIR /app
COPY frontend/ ./

# Fix ownership for current directory and build Flutter web app
RUN git config --global --add safe.directory /app && \
    flutter doctor -v && \
    flutter pub get && \
    flutter build web --release --base-href="/" --verbose

# Java build stage
FROM eclipse-temurin:17-jdk-alpine AS java-builder

WORKDIR /app

# Copy entire backend directory to avoid path issues
COPY backend/ ./

# Copy Flutter build from previous stage
COPY --from=flutter-builder /app/build/web ./src/main/resources/static/

# Make mvnw executable and build
RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B && \
    ./mvnw clean package -DskipTests

# Production stage
FROM eclipse-temurin:17-jre-alpine

# Add metadata
LABEL maintainer="bansalmayank65@gmail.com"
LABEL description="Amazon Agentic Workstation - Task orchestration web app"

# Install dumb-init and wget for proper signal handling and health checks
RUN apk add --no-cache dumb-init wget

# Create non-root user for security
RUN addgroup -g 1001 -S appuser && \
    adduser -S appuser -u 1001 -G appuser

# Set working directory
WORKDIR /app

# Copy JAR from java-builder stage
COPY --from=java-builder --chown=appuser:appuser /app/target/agenticworkstation-0.0.1-SNAPSHOT.jar app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]