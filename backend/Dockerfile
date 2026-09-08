FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
COPY target/hr-leave-portal-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
