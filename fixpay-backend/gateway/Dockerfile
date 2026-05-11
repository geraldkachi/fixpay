# ── Stage 1: Build ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy Gradle wrapper and config first for layer caching
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY shared/build.gradle.kts     shared/build.gradle.kts
COPY gateway/build.gradle.kts    gateway/build.gradle.kts
COPY core/build.gradle.kts       core/build.gradle.kts
COPY compliance/build.gradle.kts compliance/build.gradle.kts
COPY analytics/build.gradle.kts  analytics/build.gradle.kts

# Download dependencies before copying source (cache this layer)
RUN ./gradlew :gateway:dependencies --no-daemon 2>/dev/null || true

# Copy source
COPY shared/src     shared/src
COPY gateway/src    gateway/src

RUN ./gradlew :gateway:bootJar --no-daemon -x test

# ── Stage 2: Runtime ──────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S fixpay && adduser -S fixpay -G fixpay

WORKDIR /app
COPY --from=builder /workspace/gateway/build/libs/*.jar app.jar
RUN chown fixpay:fixpay app.jar

USER fixpay

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
