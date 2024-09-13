# Assignment Project Documentation

## Overview

This project is a Spring Boot application designed to manage users, transactions, and Bitcoin pricing. The application is built using Java 21, Spring Boot, PostgreSQL, and Redis, providing a robust backend service. This documentation provides setup instructions, environment details, and references for developers working with this project.

## Prerequisites

Before you begin, ensure you have the following software installed:

- **JDK 21.0.4** or later
- **Apache Maven 3.9.8** or later
- **PostgreSQL 16+**
- **Redis**
- **Docker** (optional, for containerized database setup)

## Initial Setup

### 1. Clone the Repository

Start by cloning the repository to your local machine:

```bash
git clone https://github.com/ChunHisangLee/microservice-walletservice.git
cd microservice-walletservice
```

### 2. Configure the Database

The application uses PostgreSQL as the primary database and Redis for caching. You need to configure the database connections in the application.yml or application.properties file.
#### PostgreSQL Configuration in application.yml:

```yaml
# Default profile (local development and testing)
server:
  port: 8082  # Port for Wallet Service

spring:
  profiles:
    active: default

---
spring:
  config:
    activate:
      on-profile: default
  datasource:
    url: jdbc:postgresql://localhost:5432/walletdb  # Dedicated database for Wallet Service
    username: postgres
    password: Ab123456
    driver-class-name: org.postgresql.Driver
    hikari:
      auto-commit: true
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    open-in-view: false  # Disable open-in-view to avoid potential performance issues

  # RabbitMQ settings if Wallet Service needs to communicate with other services
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# Logging level for the Wallet Service
logging:
  level:
    com.jack.walletservice: INFO

# SpringDoc settings for API documentation
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui-custom.html

# Security settings specific to the Wallet Service
app:
  jwtSecret: Xb34fJd9kPbvmJc84mDkV9b3Xb4fJd9kPbvmJc84mDkV9b3Xb34fJd9kPbvmJc84
  jwtExpirationMs: 3600000

security:
  authentication:
    enabled: true  # Enable authentication for Wallet Service

# Docker profile (for running in containers)
---
spring:
  config:
    activate:
      on-profile: docker
  datasource:
    url: jdbc:postgresql://db:5432/walletdb  # Database connection for Docker environment
    username: postgres
    password: Ab123456
    driver-class-name: org.postgresql.Driver
    hikari:
      auto-commit: true
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    open-in-view: false  # Disable open-in-view in Docker as well

# Logging level for the Wallet Service
logging:
  level:
    com.jack.walletservice: INFO

# SpringDoc settings for API documentation
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui-custom.html

# Security settings specific to the Wallet Service
app:
  jwtSecret: Xb34fJd9kPbvmJc84mDkV9b3Xb4fJd9kPbvmJc84mDkV9b3Xb34fJd9kPbvmJc84
  jwtExpirationMs: 3600000

security:
  authentication:
    enabled: true  # Enable authentication for Wallet Service
```

### 3. Create the PostgreSQL Database

If you haven't already, create the database in PostgreSQL:

```sql
CREATE DATABASE postgres;
```
Initialize the database with the following table scripts:
* [schema.sql](src/main/resources/SQL/schema.sql)

### 4. Build and Run the Application

You can run the application using Maven:

```bash
mvn spring-boot:run
```

Or build the project and run the JAR file:

```bash
mvn clean package
java -jar target/wallet-service-0.0.1-SNAPSHOT.jar
```

### 5. Access the Application

You can access the application and its API documentation via the following link:

* Swagger UI: http://localhost:8082/swagger-ui/index.html

### 6. Docker Setup (Optional)

If you prefer using Docker for PostgreSQL and Redis, use the following commands:
```bash
docker run --name assignment-db -e POSTGRES_DB=walletdb -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=Ab123456 -p 5432:5432 -d postgres
```

### 7. Docker Compose Setup

You can also use Docker Compose to run the entire stack (Spring Boot application, PostgreSQL, and Redis) together. Create a docker-compose.yml file with the following content:

```yaml
services:
  db:
    image: postgres:latest
    environment:
      POSTGRES_DB: walletdb  # Changed to the wallet service's database
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: Ab123456
    ports:
      - "5433:5432"
    networks:
      - assignment-network
    healthcheck:
      test: [ "CMD", "pg_isready", "-U", "postgres" ]
      interval: 10s
      timeout: 5s
      retries: 5

  rabbitmq:
    image: rabbitmq:management
    ports:
      - "5672:5672"   # RabbitMQ service port
      - "15672:15672" # RabbitMQ management UI
    networks:
      - assignment-network
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    healthcheck:
      test: [ "CMD", "rabbitmq-diagnostics", "ping" ]
      interval: 10s
      timeout: 5s
      retries: 5

  walletservice:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8082:8082"  # Changed to match the port for the Wallet Service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/walletdb  # Changed to walletdb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: Ab123456
      SPRING_PROFILES_ACTIVE: docker

      # RabbitMQ Configuration
      SPRING_RABBITMQ_HOST: rabbitmq
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: guest
      SPRING_RABBITMQ_PASSWORD: guest

    depends_on:
      db:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    networks:
      - assignment-network

networks:
  assignment-network:
    driver: bridge
```

To build and run the stack using Docker Compose, run:

```bash
docker-compose up --build
```

### 8. Docker Image Information

The Docker image for this project is available on Docker Hub:

- Docker Hub Username: chunhsianglee
- Docker Repository: assignment
- Image Tag: latest

You can pull the image directly from Docker Hub using:

```bash
docker pull chunhsianglee/microservice-userservice:latest
```

## API Documentation

The API documentation is automatically generated by Swagger. You can access it through the following link:

* Swagger UI: http://localhost:8082/swagger-ui/index.html

This documentation provides detailed information about the available API endpoints, methods, and responses.

## Application Configuration

### The application includes the following additional configuration:

JWT Settings:
* jwtSecret: A secret key for signing JWTs.
* jwtExpirationMs: The JWT expiration time is set to 3600000 milliseconds (1 hour).
* Security: Authentication is disabled for test (security.authentication.enabled: false).

## Development Environment

The application has been developed and tested with the following tools:

* JDK: Version 21.0.4
* Apache Maven: [version 3.9.8](https://maven.apache.org/download.cgi)
* IDE: [IntelliJ IDEA Ultimate 2024.2](https://www.jetbrains.com/idea/download/?section=windows)
  for development.

## Further Reading and References

For more information, consider exploring the following resources:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
  : A comprehensive guide to the Maven project management tool.

*  [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.2.4/maven-plugin/reference/html/)
   : Detailed documentation on the Spring Boot Maven Plugin.

*  [Spring Security Documentation](https://docs.spring.io/spring-security/reference/index.html)
   : Detailed documentation on the Spring Security.

## License

This project is licensed under the MIT License. See the LICENSE file for more details.
