FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

COPY build/libs/profile-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9095

CMD ["java", "-jar", "app.jar"]
