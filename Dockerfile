FROM amazoncorretto:17-al2023-RC-headless
COPY build/libs/store-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
