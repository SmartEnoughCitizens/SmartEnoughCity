# -------- Builder --------
FROM gradle:9.2.1-jdk17-alpine AS builder
WORKDIR /home/gradle/project
    
COPY build.gradle settings.gradle gradle.properties* ./
COPY gradle gradle
RUN gradle dependencies --no-daemon || return 0

COPY . .
RUN gradle clean bootJar --no-daemon

# -------- Runtime --------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
