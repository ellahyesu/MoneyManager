FROM maven:3.9.6-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml /app/
COPY src /app/src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:11-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
