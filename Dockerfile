FROM eclipse-temurin:17

WORKDIR /app

COPY /build/libs/hermes-0.0.1-SNAPSHOT.jar /app

EXPOSE 8080

CMD ["java", "-jar", "hermes-0.0.1-SNAPSHOT.jar"]
