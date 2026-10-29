# Stage 1: Build
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

# Download dependencies first
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

RUN mkdir -p uploads/venues uploads/owners

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]