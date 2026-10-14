# Backend API — multi-stage build (Maven -> slim JRE). Works on Render, Docker Compose or any container host.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
# Tuned for small containers (512 MB): measured ~185 MB resident with the full Damascus network loaded.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=64m -XX:TieredStopAtLevel=1"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
