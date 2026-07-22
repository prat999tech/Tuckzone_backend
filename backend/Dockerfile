# ─────────────────────────────────────────────
# Stage 1: Build the JAR using Maven
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer cache – only re-downloads deps if pom changes)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source and build
COPY src src
RUN ./mvnw clean package -DskipTests -q

# ─────────────────────────────────────────────
# Stage 2: Run the JAR on a slim JRE image
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=builder /app/target/canteen-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
