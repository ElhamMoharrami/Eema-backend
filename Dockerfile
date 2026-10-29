# Stage 1: Build
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

# Tests run in CI and via `mvn test`; image assembly avoids rerunning them on every local startup.
RUN --mount=type=cache,target=/root/.m2 mvn -Dmaven.test.skip=true clean package

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

RUN mkdir -p uploads/venues uploads/owners

RUN addgroup -S eema && adduser -S eema -G eema && chown -R eema:eema /app

USER eema

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
