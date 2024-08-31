# Build
FROM gradle:jdk21 AS build
WORKDIR /app
COPY . /app
RUN chmod +x ./gradlew
RUN ./gradlew shadowJar

# Runtime
FROM eclipse-temurin:21

WORKDIR /app

COPY --from=build /app/build/libs/app-all.jar .
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app-all.jar"]
