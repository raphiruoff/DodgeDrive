FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY build/libs/websocket-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
