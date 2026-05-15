# syntax=docker/dockerfile:1.6
# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Render injects the PORT env var. Spring Boot will bind to it via -Dserver.port.
# Keep JVM memory small to fit Render's 512 MB free tier.
ENV JAVA_OPTS="-Xms128m -Xmx400m -XX:+UseSerialGC"
EXPOSE 8761
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=${PORT:-8761} -jar app.jar"]
