# ── Etapa 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copiar solo el pom.xml primero para aprovechar el cache de capas de Docker.
# Si el código cambia pero las dependencias no, Maven no las vuelve a descargar.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B 2>/dev/null || true

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# ── Etapa 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario sin privilegios para no correr como root
RUN addgroup -S elyella && adduser -S elyella -G elyella
USER elyella

COPY --from=build /app/target/el-y-ella-backend-1.0.0.jar app.jar

# Cloud Run inyecta PORT; si no existe, usar 8080 por defecto
ENV PORT=8080
EXPOSE $PORT

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
