FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# PERHATIKAN: Kita copy app-v2.jar
COPY target/app-v2.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]