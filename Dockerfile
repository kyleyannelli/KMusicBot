# FROM MAVEN USING JAVA 17
FROM maven:3.8.1-openjdk-11-slim AS MAVEN_BUILD
# COPY SOURCE CODE TO CONTAINER
COPY pom.xml /build/
COPY src /build/src/
COPY .env /build/.env
# BUILD THE PROJECT
WORKDIR /build/
RUN mvn install
# RUN THE PROJECT
ENTRYPOINT ["mvn", "exec:java"]
