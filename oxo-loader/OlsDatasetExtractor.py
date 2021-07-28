#!/usr/bin/env python
"""
This script pulls data about ontologies and databases registeted in EBI's OLS, identifiers.org and OBO xrefs. This creates a
CSV file of datasources that can be loaded into OxO using the OxoNeo4jLoader.py script. Datasources must be loaded into OxO
before any mappings can be loaded.
"""
__author__ = "jupp"
__license__ = "Apache 2.0"
__date__ = "03/03/2018"


import urllib.request, urllib.parse, urllib.error
import json
import xml.etree.ElementTree as ET
import yaml
import OxoClient
from configparser import ConfigParser
from optparse import OptionParser


prefixToPreferred = {}
idorgNamespace = {}

unprocessedIds = {}
termToIri = {}
termToLabel = {}

parser = OptionParser()
parser.add_option("-d", "--datasources", help="datasources csv export file")
parser.add_option("-i", "--idorg", help="id.org config file")
parser.add_option("-c", "--config", help="config file", default="config.ini")

(options, args) = parser.parse_args()

config = ConfigParser()
config.read(options.config)
OXO = OxoClient.OXO()

OXO.oxoUrl = config.get("Basics","oxoUrl")
oboDbxrefUrl= config.get("Basics", "oboDbxrefUrl")

olsurl=config.get("Basics", "olsurl")
olsurl=olsurl+"/ontologies?size=1000"

idorgDataLocation = config.get("Paths", "idorgDataLocation")
if options.idorg:
    idorgDataLocation = options.idorg

exportFileDatasources=config.get("Paths","exportFileDatasources")
if options.datasources:
    exportFileDatasources = options.datasources

reply = urllib.request.urlopen(olsurl)
anwser = json.load(reply)

ontologies  = anwser["_embedded"]["ontologies"]

datasources = {}

for ontology in ontologies:
    namespace = ontology["config"]["namespace"]
    version = ontology["updated"]

    altPrefixes = [namespace]
    if namespace == 'ordo':
        prefPrefix = 'Orphanet'
    elif namespace == 'hp':
        prefPrefix = 'HP'
        altPrefixes = [namespace, "hpo"]
        prefixToPreferred["HPO"] = prefPrefix
        prefixToPreferred["hpo"] = prefPrefix
    elif namespace == "ncit":
        prefPrefix = "NCIT"
        altPrefixes = [namespace, "NCI_Thesaurus", "NCI", "ncithesaurus", "NCI2009_04D"]
    else:
        prefPrefix = ontology["config"]["preferredPrefix"]

    title = ontology["config"]["title"]
    desc = ontology["config"]["description"]
    prefixToPreferred[prefPrefix.lower()] = prefPrefix
    prefixToPreferred[namespace.lower()] = prefPrefix

    datasources[prefPrefix] = OxoClient.Datasource(prefPrefix, None, title, desc, "ONTOLOGY", None, altPrefixes, "https://creativecommons.org/licenses/by/4.0/",  "Last updated in the ontology lookup service on "+version )

altPrefixes = []
# get namespaces from identifiers.org

#urllib.urlopen('http://www.ebi.ac.uk/miriam/main/export/xml/')
tree = ET.ElementTree(file=idorgDataLocation)

# from id.org default to namespace
# if no spaces in title, this is usally a better option
# unless a preferred prefix is provided, then always use that

rootElem = tree.getroot()
for datatype in rootElem.findall('{http://www.biomodels.net/MIRIAM/}datatype'):
    namespace =  datatype.find('{http://www.biomodels.net/MIRIAM/}namespace').text
    prefPrefix = namespace

    title =  datatype.find('{http://www.biomodels.net/MIRIAM/}name').text
    desc =  datatype.find('{http://www.biomodels.net/MIRIAM/}definition').text
    licence = None
    versionInfo = None

    altPrefixes = [namespace]

    if datatype.find('{http://www.biomodels.net/MIRIAM/}licence') is not None:
        licence =  datatype.find('{http://www.biomodels.net/MIRIAM/}licence').text
    if datatype.find('{http://www.biomodels.net/MIRIAM/}versionInfo') is not None:
        versionInfo =  datatype.find('{http://www.biomodels.net/MIRIAM/}versionInfo').text

    if datatype.find('{http://www.biomodels.net/MIRIAM/}preferredPrefix') is not None:
        prefPrefix = datatype.find('{http://www.biomodels.net/MIRIAM/}preferredPrefix').text
    elif ' ' not in title:
        prefPrefix = title

    # add titles to alt prefix if
    if ' ' not in title:
        altPrefixes.append(title)

    if datatype.find('{http://www.biomodels.net/MIRIAM/}alternatePrefixes') is not None:
        for altPrefixs in datatype.find('{http://www.biomodels.net/MIRIAM/}alternatePrefixes'):
            altPrefixes.append(altPrefixs.text)

    if prefPrefix.lower() in prefixToPreferred:
        print(("Ignoring "+namespace+" from idorg as it is already registered as a datasource"))
    elif namespace.lower() in prefixToPreferred:
        print(("Ignoring " + namespace + " from idorg as it is already registered as a datasource"))
    else:
        idorgNamespace[prefPrefix.lower()] = prefPrefix
        idorgNamespace[namespace.lower()] = prefPrefix
        idorgNamespace[title.lower()] = prefPrefix
        prefixToPreferred[prefPrefix.lower()] = prefPrefix
        prefixToPreferred[namespace.lower()] = prefPrefix
        prefixToPreferred[title.lower()] = prefPrefix

        if prefPrefix not in datasources:
            datasources[prefPrefix] = OxoClient.Datasource (prefPrefix, namespace, title, desc, "DATABASE", None, altPrefixes, licence, versionInfo)


#oboDbxrefUrl = 'https://raw.githubusercontent.com/geneontology/go-site/master/metadata/db-xrefs.yaml'
# Read from OBO db-xrefs
yamlData = yaml.load(urllib.request.urlopen(oboDbxrefUrl))

for database in yamlData:
    namespace= database["database"]
    title = database["name"]
    prefPrefix = namespace

    altPrefixes = [namespace]
    if namespace.lower() in prefixToPreferred:
        print(("Ignoring " + namespace + " from OBO as it is already registered as a datasource"))
    else:
        urlSyntax = None
        if "entity_types" in database:
            if "url_syntax" in  database["entity_types"][0]:
                urlSyntax = database["entity_types"][0]["url_syntax"].replace("[example_id]", "")
        prefixToPreferred[namespace.lower()] = prefPrefix

        if prefPrefix not in datasources:
            print("New datasource " + namespace + " from GO db-xrefs file")

            datasources[prefPrefix] = OxoClient.Datasource (prefPrefix, None, title, None, "DATABASE",urlSyntax, altPrefixes, None, None)



# Create Paxo as datasources
print("Adding paxo as datasource")
prefPrefix="paxo"
namespace=None
title="paxo"
desc=None
sourceType="DATABASE"
urlSyntax=None
altPrefixes=["paxo"]
licence=None
versionInfo=1
datasources[prefPrefix] = OxoClient.Datasource(prefPrefix, namespace, title, desc, sourceType, urlSyntax, altPrefixes, licence, versionInfo)


print("Adding loinc as datasource")
prefPrefix="LNC"
namespace=None
title="LOINC"
desc="Logical Observation Identifiers Names and Codes"
sourceType="DATABASE"
urlSyntax=None
altPrefixes=["loinc", "LOINC"]
licence=None
versionInfo=1
datasources[prefPrefix] = OxoClient.Datasource(prefPrefix, namespace, title, desc, sourceType, urlSyntax, altPrefixes, licence, versionInfo)


print("Adding ctv3 as datasource")
prefPrefix="CTV3"
namespace=None
title="CTV3"
desc="CTV3"
sourceType="DATABASE"
urlSyntax=None
altPrefixes=["ctv3", "CTV3"]
licence=None
versionInfo=1
datasources[prefPrefix] = OxoClient.Datasource(prefPrefix, namespace, title, desc, sourceType, urlSyntax, altPrefixes, licence, versionInfo)


print("Adding ICD10CM as datasource")
prefPrefix="ICD10CM"
namespace=None
title="ICD10CM"
desc="ICD10CM"
sourceType="DATABASE"
urlSyntax=None
altPrefixes=["icd10cm", "ICD10CM"]
licence=None
versionInfo=1
datasources[prefPrefix] = OxoClient.Datasource(prefPrefix, namespace, title, desc, sourceType, urlSyntax, altPrefixes, licence, versionInfo)


print("Adding ICDO as datasource")
prefPrefix="ICDO"
namespace=None
title="ICDO"
desc="ICDO"
sourceType="DATABASE"
urlSyntax=None
altPrefixes=["icdo", "ICDO"]
licence=None
versionInfo=1
datasources[prefPrefix] = OxoClient.Datasource(prefPrefix, namespace, title, desc, sourceType, urlSyntax, altPrefixes, licence, versionInfo)




# print OxO loading csv file
import OxoCsvBuilder

buider = OxoCsvBuilder.Builder()
buider.exportDatasourceToCsv(exportFileDatasources, datasources)
