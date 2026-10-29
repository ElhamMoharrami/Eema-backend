# Stage 1: Build
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

# Download dependencies first
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

RUN mkdir -p uploads/venues uploads/owners

RUN addgroup -S eema && adduser -S eema -G eema && chown -R eema:eema /app

USER eema

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
