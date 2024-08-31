# Build
FROM gradle:jdk21 AS build
WORKDIR /app
COPY . /app
RUN chmod +x ./gradlew
RUN ./gradlew jar

# Runtime
FROM eclipse-temurin:21

WORKDIR /app

COPY --from=build /app/build/libs/app.jar .
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
