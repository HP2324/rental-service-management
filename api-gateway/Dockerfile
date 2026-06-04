FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent pom + all module poms so Maven can resolve the multi-module reactor
COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY listing-service/pom.xml listing-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml

# api-gateway has no project dependencies, so no shared/src needed
RUN mvn dependency:go-offline -pl api-gateway -q

COPY api-gateway/src api-gateway/src
RUN mvn clean package -pl api-gateway -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
