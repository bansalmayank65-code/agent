# Flutter build stage
FROM ghcr.io/cirruslabs/flutter:3.24.3 AS flutter-builder

WORKDIR /app
COPY frontend/ ./
RUN flutter pub get && \
    flutter build web --release --base-href="/"

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