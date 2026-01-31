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
RUN addgroup -S -g 10001 app && adduser -S -D -u 10001 -G app app

COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# ensure the jar + working dir are readable by the non-root user
RUN chown -R 10001:10001 /app

ENV SPRING_PROFILES_ACTIVE=prod

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]