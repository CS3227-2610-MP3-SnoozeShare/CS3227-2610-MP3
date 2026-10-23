FROM gradle:9.7.1-jdk25 AS build
WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN ./gradlew dependencies --no-daemon > /dev/null
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
