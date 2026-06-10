# ── Stage 1: Build ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY shared/build.gradle.kts     shared/build.gradle.kts
COPY gateway/build.gradle.kts    gateway/build.gradle.kts
COPY core/build.gradle.kts       core/build.gradle.kts
COPY compliance/build.gradle.kts compliance/build.gradle.kts
COPY analytics/build.gradle.kts  analytics/build.gradle.kts

RUN ./gradlew :core:dependencies --no-daemon 2>/dev/null || true

COPY shared/src  shared/src
COPY core/src    core/src

RUN ./gradlew :core:bootJar --no-daemon -x test

# ── Stage 2: Runtime ──────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S fixpay && adduser -S fixpay -G fixpay

WORKDIR /app
COPY --from=builder /workspace/core/build/libs/*.jar app.jar
RUN chown fixpay:fixpay app.jar

USER fixpay

EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
