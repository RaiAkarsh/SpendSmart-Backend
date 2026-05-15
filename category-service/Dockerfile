# syntax=docker/dockerfile:1.6
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package \
    && mv target/*.jar target/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
ENV JAVA_OPTS="-Xms128m -Xmx400m -XX:+UseSerialGC"
EXPOSE 8084
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=${PORT:-8084} -jar app.jar"]
