FROM maven:3.9.9-eclipse-temurin-21-jammy AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY . .
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]