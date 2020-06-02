
import csv
from configparser import ConfigParser
from optparse import OptionParser
import OxoCsvBuilder
import OxoClient
import datetime

parser = OptionParser()
parser.add_option("-t", "--terms", help="terms csv export file")
parser.add_option("-m", "--mappings", help="mappings csv export file")
parser.add_option("-c", "--config", help="config file", default="config.ini")

(options, args) = parser.parse_args()

config = ConfigParser()
config.read(options.config)

exportFileTerms=config.get("Paths","exportFileTerms")
if options.terms:
    exportFileTerms = options.terms

exportFileMappings=config.get("Paths","exportFileMappings")
if options.mappings:
    exportFileMappings = options.mappings

loinc_mapping_file = open(config.get("LOINC", "PartRelatedCodeMapping"))
loinc_mapping_reader = csv.reader(loinc_mapping_file, delimiter=',', quotechar='"')

loinc_part_file = open(config.get("LOINC", "Part"))
loinc_part_reader = csv.reader(loinc_part_file, delimiter=',', quotechar='"')

OXO = OxoClient.OXO()
OXO.oxoUrl = config.get("Basics","oxoUrl")
datasets = OXO.getOxODatasets()

loincDatasource = None

for dataset in datasets:
    print(dataset)
    if dataset['prefix'] == "LNC":
        loincDatasource = dataset

mappings = []
terms = {}


next(loinc_part_reader)

for row in loinc_part_reader:
    part_number=row[0]
    part_type_name=row[1]
    part_name=row[2]
    part_display_name=row[3]
    status=row[4]
    
    terms['LNC:' + part_number] = {
            "prefix": 'LNC',
            "id": part_number,
            "curie": 'LNC:' + part_number,
            "uri": 'http://loinc.org/#' + part_number,
            "label": part_name
        }





next(loinc_mapping_reader)

for row in loinc_mapping_reader:
    part_number=row[0]
    part_name=row[1]
    rpart_type_name=row[2]
    ext_code_id=row[3]
    ext_code_display_name=row[4]
    ext_code_system=row[5]
    equivalence=row[6]
    content_origin=row[7]
    ext_code_system_version=row[8]
    ext_code_system_copyright_notice=row[9]

    from_curie="LNC:" + part_number
    to_curie=ext_code_id

    if ext_code_system == "http://pubchem.ncbi.nlm.nih.gov":
        # 5997
        continue
    elif ext_code_system == "http://www.nlm.nih.gov/research/umls/rxnorm":
        # 260101
        continue
    elif ext_code_system == "http://www.radlex.org":
        # RID88
        continue
    elif ext_code_system == "http://snomed.info/sct":
        to_curie = "SNOMEDCT:" + ext_code_id
    elif ext_code_system == "https://www.ebi.ac.uk/chebi":
        to_curie = ext_code_id
    elif ext_code_system == "https://www.ncbi.nlm.nih.gov/taxonomy":
        to_curie = "NCIT:" + ext_code_id
    elif ext_code_system == "https://www.ncbi.nlm.nih.gov/gene":
        # 6329
        continue
    elif ext_code_system == "https://www.ncbi.nlm.nih.gov/clinvar":
        # 7111
        continue

    mapping = {
        'fromId': from_curie,
        'toId': to_curie,
        'datasourcePrefix': "LNC",
        'sourceType': "ONTOLOGY",
        'scope': "RELATED"
    }

    mappings.append(mapping)

print("Generating CSV files for neo loading...")

builder = OxoCsvBuilder.Builder()

builder.exportTermsToCsv(exportFileTerms, terms)
builder.exportMappingsToCsv(exportFileMappings, mappings, { 'LNC': loincDatasource })

print("Finished process!")
    

    
