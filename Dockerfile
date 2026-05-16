FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN ./mvnw -B dependency:go-offline

COPY src ./src

RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
