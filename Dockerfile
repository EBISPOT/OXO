
#use alpine base for minimal size
#includes OpenJDK 8 (and Maven 3)
FROM openjdk:8-jre-alpine

COPY oxo-web/target/oxo-web.war /home/oxo.war