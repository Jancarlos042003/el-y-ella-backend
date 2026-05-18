# ── Etapa 1: compilación ─────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Cachear dependencias antes de copiar el código fuente
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q # los tests corren en CI, no en imagen

# ── Etapa 2: imagen de ejecución ──────────────────────────────────────────────
# jammy (Ubuntu 22.04) usa glibc — compatible con Cloud Run y libs nativas (JasperReports, etc.)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Usuario sin privilegios
RUN groupadd -r elyella && useradd -r -g elyella elyella
USER elyella

COPY --from=build /app/target/el-y-ella-backend-1.0.0.jar app.jar

EXPOSE 8080

# PORT: Cloud Run inyecta esta variable; Spring Boot la lee vía SERVER_PORT
# MaxRAMPercentage: evita OOM kill en instancias con poca RAM (Cloud Run default 512 MB)
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75.0 -jar app.jar --server.port=${PORT:-8080}"]
