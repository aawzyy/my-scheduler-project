FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# PERHATIKAN: Kita copy app-v2.jar
COPY target/scheduler-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]