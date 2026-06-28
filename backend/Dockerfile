# Backend Spring Boot (Maven). Multi-stage : build Maven → image finale JRE slim.
# Contexte de build : ./backend (self-contained). Voir docker-compose.full.yml.

# --- build (Maven) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache des dépendances : on copie d'abord le pom puis on télécharge.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# --- runtime (JRE slim) ---
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# Profils, datasource, rabbitmq, reco, CORS : tout par variables d'environnement (cf. compose / .env).
ENTRYPOINT ["java", "-jar", "app.jar"]
