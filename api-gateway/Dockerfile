# ================================================================
# api-gateway — Dockerfile
# ================================================================
# Multi-stage build:
#   1. "builder" — compiles this service using the full Maven reactor
#      from the repo root (needed because it's part of a multi-module
#      project and depends on common-lib).
#   2. "runtime" — copies just the built jar into a slim JRE image.
#
# IMPORTANT: the build context for this Dockerfile must be the REPO
# ROOT, not this folder — see docker-compose.yml: build.context: "."
# Maven needs to see every module declared in the root pom.xml to
# resolve the reactor, even though it only builds this one.
# ================================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copy the whole multi-module project. Simpler and more reliable than
# hand-picking individual pom.xml files — fine for a project this size.
COPY . .

# Build only api-gateway + the modules it depends on (-am = "also make").
# Tests are skipped here; run them separately (mvn test / CI), not at
# image-build time.
# --mount=type=cache keeps the Maven repo (~/.m2) between builds, so
# rebuilds only download dependencies that actually changed — minutes
# saved per build. Requires BuildKit (default in Docker Desktop).
RUN --mount=type=cache,target=/root/.m2 mvn -pl api-gateway -am clean package -DskipTests -q

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as a non-root user — basic container hygiene.
RUN useradd --system --create-home --shell /usr/sbin/nologin guruai

COPY --from=builder /build/api-gateway/target/api-gateway-1.0.0.jar app.jar

USER guruai
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
