FROM amazoncorretto:21-alpine-jdk

WORKDIR /app

COPY build/libs/consistency-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9090

CMD ["java", "-jar", "app.jar"]
