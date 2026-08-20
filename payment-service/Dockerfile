FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Parent POM
COPY pom.xml .

# Payment service Maven wrapper and POM
COPY payment-service/mvnw payment-service/mvnw
COPY payment-service/.mvn payment-service/.mvn
COPY payment-service/pom.xml payment-service/pom.xml

RUN chmod +x payment-service/mvnw

# Download dependencies
RUN cd payment-service && ./mvnw dependency:go-offline

# Copy payment service source
COPY payment-service/src payment-service/src

# Build
RUN cd payment-service && ./mvnw clean package -DskipTests


# Runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/payment-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]