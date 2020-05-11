OxO is a service for finding mappings (or cross-references) between terms from
ontologies, vocabularies and coding standards. OxO imports mappings from a
variety of sources including the [Ontology Lookup Service](https://www.ebi.ac.uk/ols/index) and a subset of
mappings provided by the [UMLS](https://www.nlm.nih.gov/research/umls/index.html).

# OxO with Docker

OxO is comprised of three components:

* The loader scripts (oxo-loader/), which pull data about terms and mappings from the OLS, OBO xrefs, and UMLS and upload them to neo4j
* The indexer (oxo-indexer/), which indexes terms and mappings found in neo4j in solr
* The Web application (oxo-web/), which provides the user interface

The OxO Web application, solr, and neo4j can be started using docker-compose:

    docker-compose up

The docker-compose configuration, by default, stores all persistent data (the hsqldb, neo4j, and solr data files) in the data/ directory by mounting volumes.  However, these datasets are empty until the loader and indexer have been executed.

## Running the loader

The loader scripts are documented in the README of the oxo-loader/ directory.

## Running the indexer

After using the loader to load data into neo4j, the indexer can be executed
using Docker:

    docker build -f oxo-indexer/Dockerfile -t oxo-indexer
    docker run oxo-indexer


# OxO without Docker

Sometimes it is impractical to use Docker (e.g. for local development).  To get
OxO up and running without Docker, first install:

* neo4j community edition 3.1.1
* solr 5.3.0
* Java 1.8
* Maven

The instructions for the loader are not Docker-specific, and can be found in the
oxo-loader/ directory.  For the indexer and Web application, first compile the
project using Maven:

    mvn clean package

Then run the indexer:

    java -Xmx10g -jar oxo-indexer.jar

The Web application is a standard WAR and can be deployed using e.g. Tomcat.


