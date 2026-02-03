# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy gradle wrapper and config (context is project root)
COPY gradle gradle
COPY gradlew gradlew
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts

# Copy shared module (needed for backend dependencies)
COPY shared/build.gradle.kts shared/build.gradle.kts
COPY shared/src shared/src

# Copy backend module
COPY backend/build.gradle.kts backend/build.gradle.kts
COPY backend/src backend/src

# Build the backend application
RUN chmod +x gradlew && ./gradlew :backend:build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=build /app/backend/build/libs/*.jar app.jar

# Cloud Run uses PORT environment variable
ENV PORT=8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
