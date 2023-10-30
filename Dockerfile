# FROM MAVEN USING JAVA 11
FROM maven:3.8.1-openjdk-11-slim AS MAVEN_BUILD
COPY . /build/
WORKDIR /build
RUN mvn clean install
WORKDIR /build/kmusicbot-core/
COPY ./kmusicbot-core/.env /build/kmusicbot-core/.env
# RUN THE PROJECT
ENTRYPOINT ["mvn", "exec:java"]
