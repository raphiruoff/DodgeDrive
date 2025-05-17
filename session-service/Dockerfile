FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

COPY build/libs/session-service-0.0.1-SNAPSHOT.jar app.jar


EXPOSE 9101

CMD ["java", "-jar", "app.jar"]
