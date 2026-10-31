# Build and test with a JDK, then copy only the executable JAR into an unprivileged JRE image.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:resolve
COPY src ./src
RUN mvn -B -ntp verify

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S dlp && adduser -S dlp -G dlp
WORKDIR /app
COPY --from=build --chown=dlp:dlp /build/target/mini-dlp-1.0.0.jar app.jar
USER dlp
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
