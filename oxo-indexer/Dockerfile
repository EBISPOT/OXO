
FROM maven:3.6-jdk-8 AS build
RUN mkdir /opt/oxo
COPY pom.xml /opt/oxo/pom.xml
COPY oxo-web /opt/oxo/oxo-web
COPY oxo-model /opt/oxo/oxo-model
COPY oxo-indexer /opt/oxo/oxo-indexer
RUN cd /opt/oxo && mvn clean package -DskipTests

FROM openjdk:8-jre-alpine
RUN apk add bash
COPY --from=build /opt/oxo/oxo-indexer/target/oxo-indexer.jar /opt/oxo-indexer.jar
COPY --from=build /opt/oxo/oxo-indexer/application.properties /opt/application.properties
WORKDIR /opt
ENTRYPOINT ["java", "-Xmx10g", "-jar", "oxo-indexer.jar"]

