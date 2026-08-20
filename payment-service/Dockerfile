FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Parent POM
COPY pom.xml .

# Common library
COPY common-lib/pom.xml common-lib/pom.xml
COPY common-lib/src common-lib/src

# Payment service
COPY payment-service/pom.xml payment-service/pom.xml
COPY payment-service/src payment-service/src

# Build payment-service and its dependencies
RUN mvn -pl payment-service -am clean package -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/payment-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]