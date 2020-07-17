#!/usr/bin/env bash

python /opt/oxo-loader/OlsDatasetExtractor.py -c $CONFIG_INI -i $IDORG_XML -d $NEO4JIMPORT/datasources.csv
python /opt/oxo-loader/OxoNeo4jLoader.py -W -d datasources.csv -c $CONFIG_INI

python /opt/oxo-loader/OlsMappingExtractor.py -c $CONFIG_INI -t $NEO4JIMPORT/ols_terms.csv -m $NEO4JIMPORT/ols_mappings.csv
python /opt/oxo-loader/OxoNeo4jLoader.py -c $CONFIG_INI -t ols_terms.csv -m ols_mappings.csv

#python /app/UmlsMappingExtractor.py -c /app/config/config.ini -t $NEO4JIMPORT/umls_terms.csv -m $NEO4JIMPORT/umls_mappings.csv
#python /app/OxoNeo4jLoader.py -c $CONFIG_INI -t umls_terms.csv -m umls_mappings.csv
