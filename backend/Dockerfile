# ─────────────────────────────────────────────
# Stage 1: Build the JAR using Maven
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer cache – only re-downloads deps if pom changes)
# NOTE: whether these paths need a "backend/" prefix depends on the Railway service's
# "Root Directory" setting — if it's set to "backend", the build context is already
# scoped there and these must be bare paths (as below). If Root Directory is unset
# (context = repo root), prefix each COPY source with "backend/" instead.
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
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=builder /app/target/canteen-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
