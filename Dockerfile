# Stage 1: Build
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

# Production image builds compile and execute the test suite.
RUN --mount=type=cache,target=/root/.m2 mvn clean package

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

RUN addgroup -S eema && adduser -S eema -G eema && chown -R eema:eema /app

USER eema

EXPOSE 8080

ENTRYPOINT ["java", "-XX:InitialRAMPercentage=25.0", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
