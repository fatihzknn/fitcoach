# Root-level Dockerfile so Railway (and any platform building from the repo root)
# can build the backend without a "root directory" setting. Mirrors backend/Dockerfile
# but with monorepo-aware paths.

# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY backend/src ./src
RUN mvn -q -DskipTests package

# --- run stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
