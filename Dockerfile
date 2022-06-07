FROM amazoncorretto:17.0.2-alpine3.15

COPY build/libs/*.jar app.jar

CMD ["app.jar"]
