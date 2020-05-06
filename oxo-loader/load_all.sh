python /app/OlsDatasetExtractor.py -c /app/config/config.ini -i /app/config/idorg.xml -d /app/data/datasources.csv
python /app/OxoNeo4jLoader.py -W -d datasources.csv -c /app/config/config.ini

python /app/OlsMappingExtractor.py -c /app/config/config.ini -t /app/data/ols_terms.csv -m /app/data/ols_mappings.csv
python /app/OxoNeo4jLoader.py -c /app/config/config.ini -t ols_terms.csv -m ols_mappings.csv

python /app/UmlsMappingExtractor.py -c /app/config/config.ini -t /app/data/umls_terms.csv -m /app/data/umls_mappings.csv
python /app/OxoNeo4jLoader.py -c /app/config/config.ini -t umls_terms.csv -m umls_mappings.csv
