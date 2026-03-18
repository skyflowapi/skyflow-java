# =============================================================================
# Skyflow Java SDK v3 — Load Test SUT Image
#
# Builds and runs both:
#   - EchoServer  (mock Skyflow vault)   on $ECHO_PORT  (default 3015)
#   - WrapperServer (SDK under test)     on $WRAPPER_PORT (default 8080)
#
# Build context: repo root
#   docker build -f load-testing/Dockerfile -t skyflow-sut .
#
# Run:
#   docker run -p 8080:8080 -p 3015:3015 skyflow-sut
# =============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Build — install common + v3 SDK, build wrapper fat jar
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Cache common module dependencies
COPY common/pom.xml common/pom.xml
RUN mvn dependency:go-offline -f common/pom.xml -q || true

# Build common module
COPY common/ common/
RUN mvn install -f common/pom.xml -DskipTests -Dgpg.skip=true -q

# Cache v3 SDK dependencies
COPY v3/pom.xml v3/pom.xml
RUN mvn dependency:go-offline -f v3/pom.xml -q || true

# Build v3 SDK
COPY v3/ v3/
RUN mvn install -f v3/pom.xml -DskipTests -Dgpg.skip=true -q

# Cache wrapper dependencies
COPY load-testing/wrapper/pom.xml load-testing/wrapper/pom.xml
RUN mvn dependency:go-offline -f load-testing/wrapper/pom.xml -q || true

# Build wrapper fat jar
COPY load-testing/wrapper/ load-testing/wrapper/
RUN mvn package -f load-testing/wrapper/pom.xml -DskipTests -Dgpg.skip=true -q

# ---------------------------------------------------------------------------
# Stage 2: Compile EchoServer (no Maven needed — single file)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk AS echo-builder

WORKDIR /echo
COPY load-testing/echo-server/EchoServer.java .
RUN javac EchoServer.java

# ---------------------------------------------------------------------------
# Stage 3: Runtime image
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy artifacts
COPY --from=builder    /build/load-testing/wrapper/target/skyflow-load-test-wrapper-1.0.0.jar wrapper.jar
COPY --from=echo-builder /echo/EchoServer.class .

# Startup script — runs EchoServer then WrapperServer
COPY load-testing/docker-entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

# Expose ports
EXPOSE 8080 3015

ENV ECHO_PORT=3015
ENV WRAPPER_PORT=8080
ENV ECHO_WAIT_MS=0
ENV ECHO_ERR_PCT=0
ENV VAULT_ID=mock-vault-id

ENTRYPOINT ["./entrypoint.sh"]
