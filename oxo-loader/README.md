These Python scripts are used to 

1. To dump a list of datasources to CSV from identifiers.org, OLS, and UMLS (`OlsDatasetExtractor.py`)
2. To load this list of datasources from the the CSV generated in 1. to the neo4j database used by OxO (`OxoNeo4jLoader.py`)
3. To pull terms mappings from OLS and UMLS and generate CSV files (`OlsMappingExtractor.py`, `UmlsMappingExtractor.py`)
2. To load these terms and mappings into the neo4j database used by OxO (`OxoNeo4jLoader.py`)

They are configured by a config.ini file, an example of which is provided as
config.sample.ini.  The path to the config is passed using to each script using
the `-c` option.

# OlsDatasetExtractor

The `OlsDatasetExtractor.py` script pulls metadata about ontologies and databases
registered in OLS, identifiers.org and OBO xrefs, and generates a CSV file.

* OLS is accessed via its API, configurable in config.ini with the `olsurl` option

* The identifiers.org data is loaded from the idorg.xml file, configurable in config.ini with the `idorgDataLocation` option.
  This file is provided in the root of this repository.

* OBO xrefs are loaded from a yaml file, configurable in config.ini with the `oboDbxrefUrl` option

The CSV file is exported to the filename specified in the `exportFileDatasources` configuration option, or the -d command line option.
If specified, the command line option takes precedence over the config.ini option.

Example command:

    python OlsDatasetExtractor.py -c config.ini -i idorg.xml -d datasources.csv

The output CSV file has the form:

    "prefix","idorgNamespace","title","description","sourceType","baseUri","alternatePrefixes","licence","versionInfo"

For example:

    "DrugBank","drugbank","DrugBank","The DrugBank database is a bioinformatics and chemoinformatics resource that combines detailed drug (i.e. chemical, pharmacological and pharmaceutical) data with comprehensive drug target (i.e. sequence, structure, and pathway) information. This collection references drug information.","DATABASE","","drugbank,DRUGBANK,DrugBank","None","None"

This metadata can then be loaded into the OxO neo4j database using the `OxoNeo4jLoader.py` script.  If using the provided
docker-compose neo4j, first copy datasources.csv to the data/neo4jimport directory.

    cp datasources.csv ../data/neo4jimport
    python OxoNeo4jLoader.py -c config.ini -W -d datasources.csv

# OlsMappingExtractor

After the metadata about datasets has been loaded into neo4j, the `OlsMappingExtractor.py` script is used to
pull mappings from OLS and generate two CSV files: one for terms, and one for mappings.  OlsMappingExtractor uses the OxO API.  Therefore, OxO and neo4j must be running.
    
    python OlsMappingExtractor.py -c config.ini -t ols_terms.csv -m ols_mappings.csv

These terms and mappings can then be loaded into neo4j using the `OxoNeo4jLoader.py` script:

    python OxoNeo4jLoader.py -c config.ini -t ols_terms.csv -m ols_mappings.csv

# UmlsMappingExtractor

Like the OlsMappingExtractor, the UmlsMappingExtractor is used to pull mappings from UMLS and generate
CSV files.

    python UmlsMappingExtractor.py -c config.ini -t umls_terms.csv -m umls_mappings.csv

These terms and mappings can then be loaded into neo4j using the `OxoNeo4jLoader.py` script:

    python OxoNeo4jLoader.py -c config.ini -t umls_terms.csv -m umls_mappings.csv

# Docker

These scripts can also be executed using Docker for convenience.  For example,
having prepared a host directory `mydir` with the config.ini and idorg.xml, the
OlsDatasetExtractor can be executed as follows:

    docker build -t oxo-loader .

    docker run -v ./mydir:/mnt -it oxo-loader python OlsDatasetExtractor.py
        -c /mnt/config.ini -i /mnt/idorg.xml -d /mnt/datasources.csv

