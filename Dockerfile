# Multi-stage build for optimized Docker image

# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build (skip tests completely)
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.test.skip=true -B

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create non-root user for security
RUN groupadd -r novelist && useradd -r -g novelist novelist

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R novelist:novelist /app

# Switch to non-root user
USER novelist

# Expose application port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
