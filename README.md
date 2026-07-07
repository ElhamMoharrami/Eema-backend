# Eema Backend

## Stack
- Java 21 (LTS)
- Spring Boot 3
- PostgreSQL
- Flyway
- Maven

## Prerequisites
- Java 21 JDK
- Docker (for PostgreSQL)
- Maven

## How to run locally
1. Start PostgreSQL using Docker Compose:
   ```bash
   docker-compose up -d
   ```
2. Start the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```

## Environment Variables
Refer to `.env.example` for a complete list of required variables.

## API Base URL
- Local: `http://localhost:8080`
- Production: Configured via deployment

## Database Migrations
Flyway runs automatically on startup to apply migrations.

## Building for Production
```bash
./mvnw clean package -DskipTests
```
This will produce an executable JAR in `target/`.

## CORS Configuration
The `CORS_ALLOWED_ORIGINS` environment variable must be a comma-separated list of allowed origins (no trailing slashes). Example:
```
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://your-frontend.vercel.app
```
This is read by `application.yml` as a `List<String>`.

# Eema-backend
