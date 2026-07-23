# Stage 1: build
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src ./src

RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests


# Stage 2: runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

RUN mkdir -p uploads/venues uploads/owners

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]