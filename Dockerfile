# FROM MAVEN USING JAVA 11
FROM maven:3.8.1-openjdk-11-slim AS MAVEN_BUILD
# COPY SOURCE CODE TO CONTAINER
COPY target/KMusic.jar /build/
COPY .env /build/.env
WORKDIR /build/
# RUN THE PROJECT
ENTRYPOINT ["java", "-jar", "KMusic.jar"]
