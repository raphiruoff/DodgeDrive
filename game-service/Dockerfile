FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

COPY build/libs/game-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9093

CMD ["java", "-jar", "app.jar"]
