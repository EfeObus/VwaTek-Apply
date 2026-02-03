# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy gradle wrapper and config (context is project root)
COPY gradle gradle
COPY gradlew gradlew
COPY build.gradle.kts build.gradle.kts
COPY gradle.properties gradle.properties

# Copy backend module
COPY backend/build.gradle.kts backend/build.gradle.kts
COPY backend/src backend/src

# Create a backend-only settings.gradle.kts (exclude shared/webApp to avoid KMP issues)
RUN echo 'pluginManagement { repositories { google(); gradlePluginPortal(); mavenCentral() } }\n\
dependencyResolutionManagement { repositories { google(); mavenCentral() } }\n\
rootProject.name = "VwaTekApply"\n\
include(":backend")' > settings.gradle.kts

# Build the backend application (shadowJar creates executable fat JAR)
RUN chmod +x gradlew && ./gradlew :backend:shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the fat jar (shadow jar with all dependencies)
COPY --from=build /app/backend/build/libs/backend-all.jar app.jar

# Cloud Run uses PORT environment variable
ENV PORT=8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
