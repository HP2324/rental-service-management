FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent pom + all module poms so Maven can resolve the multi-module reactor
COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY listing-service/pom.xml listing-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml

# shared/src is needed because payment-service depends on it (-am builds it first)
COPY shared/src shared/src

RUN mvn dependency:go-offline -pl payment-service -am -q

COPY payment-service/src payment-service/src
RUN mvn clean package -pl payment-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/payment-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
