# ---- Build stage: compile the Spring Boot fat jar with Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first (only re-downloads when pom.xml changes)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Build the app
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage: small JRE image that just runs the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Keep the JVM inside the 512 MB free-tier limit
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
