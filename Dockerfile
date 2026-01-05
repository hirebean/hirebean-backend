#Build app
FROM gradle:8.12.0-jdk23 AS builder
WORKDIR /app
COPY . .

#Skip test

RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]