
FROM maven:3.6-jdk-8 AS build
RUN mkdir /opt/oxo
COPY pom.xml /opt/oxo/pom.xml
COPY oxo-web /opt/oxo/oxo-web
COPY oxo-model /opt/oxo/oxo-model
COPY oxo-indexer /opt/oxo/oxo-indexer
COPY lib /opt/oxo/lib
RUN mvn install:install-file -DcreateChecksum=true -Dpackaging=jar -Dfile=/opt/oxo/lib/sping-security-orcid-stateless-0.0.8.jar -DgroupId=uk.ac.ebi.spot -DartifactId=sping-security-orcid-stateless -Dversion=0.0.8
RUN mvn install:install-file -DcreateChecksum=true -Dpackaging=jar -Dfile=/opt/oxo/lib/spring-security-core-4.1.4.RELEASE.jar -DgroupId=org.springframework.security -DartifactId=spring-security-core -Dversion=4.1.4.RELEASE
RUN mvn install:install-file -DcreateChecksum=true -Dpackaging=jar -Dfile=/opt/oxo/lib/spring-security-oauth2-2.0.13.RELEASE.jar -DgroupId=org.springframework.security.oauth -DartifactId=spring-security-oauth2 -Dversion=2.0.13.RELEASE
RUN mvn install:install-file -DcreateChecksum=true -Dpackaging=jar -Dfile=/opt/oxo/lib/spring-data-jpa-1.10.8.RELEASE.jar -DgroupId=org.springframework.data -DartifactId=spring-data-jpa -Dversion=1.10.8.RELEASE
RUN cd /opt/oxo && mvn clean package -DskipTests

FROM openjdk:8-jre-alpine
RUN apk add bash
COPY --from=build /opt/oxo/oxo-indexer/target/oxo-indexer.jar /opt/oxo-indexer.jar
COPY --from=build /opt/oxo/oxo-indexer/application.properties /opt/application.properties
WORKDIR /opt
ENTRYPOINT ["java", "-Xmx10g", "-jar", "oxo-indexer.jar"]

