# Сборка: Maven wrapper фиксирует версию Maven, pom.xml (Spring Boot BOM) — версии библиотек
FROM eclipse-temurin:21.0.12.1_1-jdk-noble AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp -q dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp -q -DskipTests package && cp target/smart-profile-*.jar app.jar

FROM eclipse-temurin:21.0.12.1_1-jre-noble
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 app \
    && mkdir -p /data/uploads && chown app /data/uploads
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER app
ENV UPLOAD_DIR=/data/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
