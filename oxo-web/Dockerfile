
FROM maven:3.6-jdk-8 AS build
RUN mkdir /opt/oxo
COPY pom.xml /opt/oxo/pom.xml
COPY oxo-web /opt/oxo/oxo-web
COPY oxo-model /opt/oxo/oxo-model
COPY oxo-indexer /opt/oxo/oxo-indexer
RUN cd /opt/oxo && mvn clean package

FROM openjdk:8-jre-alpine
RUN apk add bash
COPY --from=build /opt/oxo/oxo-web/target/oxo-web.war /opt/oxo-web.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/oxo-web.war"]

