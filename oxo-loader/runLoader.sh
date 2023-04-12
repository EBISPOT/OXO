#!/usr/bin/env bash

set -e
#module load python-3.10.2-gcc-9.3.0-gswnsij
pip3 install -r requirements.txt

echo 1	get datasources from OLS

python3 OlsDatasetExtractor.py -c config.ini -i idorg.xml -d datasources.csv

echo 2	Load datasources into neo4j

scp datasources.csv $NEO4J_HOME/import/
python3 OxoNeo4jLoader.py -W -d datasources.csv -c config.ini

echo 3	get the OLS mappings

python3 OlsMappingExtractor.py -c config.ini -t ols_terms.csv -m ols_mappings.csv

echo 4	load the OLS data into Neo4j

scp ols_terms.csv $NEO4J_HOME/import
scp ols_mappings.csv $NEO4J_HOME/import
python3 OxoNeo4jLoader.py -c config.ini -t ols_terms.csv -m ols_mappings.csv


# get the umls mappings
echo 5	get the umls mappings

#bsub  -Is -M 30000 -R "rusage[mem=30000]" "singularity exec -B /nfs/production/parkinso/spot/oxo/dev/loader:/opt/config -B /nfs/production/parkinso/spot/oxo/dev/import:/opt/oxo-loader/data  docker://ebispot/oxo-loader:$OXO_LOADER_VERSION  python /opt/oxo-loader/UmlsMappingExtractor.py -c /opt/config/config.ini -t /opt/oxo-loader/data/umls_terms.csv -m /opt/oxo-loader/data/umls_mappings.csv"

# load the UMLS data into neo4j
echo 6	load the UMLS data into neo4j

#bsub  -Is -M 30000 -R "rusage[mem=30000]" "singularity exec -B /nfs/production/parkinso/spot/oxo/dev/loader:/opt/config docker://ebispot/oxo-loader:$OXO_LOADER_VERSION  python /opt/oxo-loader/OxoNeo4jLoader.py -c /opt/config/config.ini -t umls_terms.csv -m umls_mappings.csv"

# get the LOINC mappings
echo 7	get the LOINC mappings

#bsub  -Is -M 30000 -R "rusage[mem=30000]" "singularity exec -B /nfs/production/parkinso/spot/oxo/dev/loader:/opt/config -B /nfs/production/parkinso/spot/oxo/dev/import:/opt/oxo-loader/data  docker://ebispot/oxo-loader:$OXO_LOADER_VERSION  python /opt/oxo-loader/LoincMappingExtractor.py -c /opt/config/config.ini -t /opt/oxo-loader/data/loinc_terms.csv -m /opt/oxo-loader/data/loinc_mappings.csv"

# load the LOINC data into neo4j
echo 8	load the LOINC data into neo4j

#bsub  -Is -M 30000 -R "rusage[mem=30000]" "singularity exec -B /nfs/production/parkinso/spot/oxo/dev/loader:/opt/config docker://ebispot/oxo-loader:$OXO_LOADER_VERSION  python /opt/oxo-loader/OxoNeo4jLoader.py -c /opt/config/config.ini -t loinc_terms.csv -m loinc_mappings.csv"