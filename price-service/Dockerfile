# Stage 1: Build the application using Maven
FROM maven:3.8.6 AS build

# Install OpenJDK 21 manually
RUN apt-get update && apt-get install -y openjdk-21-jdk

WORKDIR /app

# Copy the Maven project files
COPY pom.xml ./
COPY src ./src

# Stage 2: Use a smaller OpenJDK image for the final artifact
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the built artifact from the previous stage
COPY target/price-service-0.0.1-SNAPSHOT.jar /app/price-service.jar
# Expose the port the application will run on
EXPOSE 8080

# Set a default timezone
ENV TZ=UTC

# Add a health check to verify if the service is running correctly
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Use exec form of CMD to ensure signals are received by the JVM process
CMD ["java", "-jar", "/app/price-service.jar"]
