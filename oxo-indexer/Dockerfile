
FROM maven:3.5-jdk-8 AS build
COPY pom.xml /opt/OxO/pom.xml
COPY oxo-web /opt/OxO/
COPY oxo-model /opt/OxO/
COPY oxo-indexer /opt/OxO/
RUN cd /opt/OxO && mvn clean package

FROM openjdk:8-jre-alpine
COPY --from=build /opt/OxO/oxo-indexer/target/oxo-indexer.jar /opt/oxo-indexer.jar
RUN ["java", "-Xmx10g", "-jar", "/opt/oxo-indexer.jar"]
