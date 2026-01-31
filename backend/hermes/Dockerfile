# -------- Builder --------
FROM gradle:9.2.1-jdk17-alpine AS builder
WORKDIR /home/gradle/project

COPY build.gradle settings.gradle gradle.properties* ./
COPY gradle gradle
RUN gradle dependencies --no-daemon || true

COPY . .
RUN gradle clean bootJar --no-daemon

# -------- Runtime --------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# create non-root user
RUN addgroup -S app && adduser -S -G app app

COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# ensure the jar + working dir are readable by the non-root user
RUN chown -R app:app /app

ENV SPRING_PROFILES_ACTIVE=prod

USER app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
