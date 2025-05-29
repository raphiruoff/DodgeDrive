FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

COPY build/libs/friends-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9097

CMD ["java", "-jar", "app.jar"]
