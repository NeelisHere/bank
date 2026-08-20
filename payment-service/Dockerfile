FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY payment-service/pom.xml payment-service/pom.xml

COPY .mvn .mvn
COPY mvnw .

RUN chmod +x mvnw

RUN ./mvnw -pl payment-service -am dependency:go-offline

COPY payment-service payment-service

RUN ./mvnw -pl payment-service -am clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/payment-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]