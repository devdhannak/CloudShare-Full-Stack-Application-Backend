# ================================
# Stage 1: Build
# ================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first (for dependency caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# ================================
# Stage 2: Run
# ================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from Stage 1
COPY --from=builder /app/target/*.jar app.jar

# Expose the port Spring Boot runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]