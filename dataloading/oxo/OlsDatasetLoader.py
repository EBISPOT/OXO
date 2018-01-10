import urllib
import json
import xml.etree.ElementTree as ET
import yaml
import OxoClient as OXO
import csv
from ConfigParser import SafeConfigParser

prefixToPreferred = {}
idorgNamespace = {}

unprocessedIds = {}
termToIri = {}
termToLabel = {}

#config.read(sys.argv[1])
config = SafeConfigParser()
config.read("../config/oxo_dataRelease_config.ini")

OXO.oxoUrl = config.get("Basics","oxoUrl")
OXO.apikey = config.get("Basics", "oxoAPIkey")
oboDbxrefUrl= config.get("Basics", "oboDbxrefUrl")

olsurl=config.get("Basics", "olsurl")
olsurl=olsurl+"/ontologies?size=1000"

idorgDataLocation = config.get("Paths", "idorgDataLocation")

reply = urllib.urlopen(olsurl)
anwser = json.load(reply)

ontologies  = anwser["_embedded"]["ontologies"]

for ontology in ontologies:
    namespace = ontology["config"]["namespace"]
    version = ontology["updated"]

    if namespace == 'ordo':
        prefPrefix = 'Orphanet'
    else:
        prefPrefix = ontology["config"]["preferredPrefix"]

    title = ontology["config"]["title"]
    desc = ontology["config"]["description"]
    prefixToPreferred[prefPrefix.lower()] = prefPrefix
    prefixToPreferred[namespace.lower()] = prefPrefix

    OXO.saveDatasource(prefPrefix, None, title, desc, "ONTOLOGY", None, [namespace], "https://creativecommons.org/licenses/by/4.0/",  "Last updated in the ontology lookup service on "+version )
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
        print "Ignoring "+namespace+" from idorg as it is already registered as a datasource"
    elif namespace.lower() in prefixToPreferred:
        print "Ignoring " + namespace + " from idorg as it is already registered as a datasource"
    else:
        idorgNamespace[prefPrefix.lower()] = prefPrefix
        idorgNamespace[namespace.lower()] = prefPrefix
        idorgNamespace[title.lower()] = prefPrefix
        prefixToPreferred[prefPrefix.lower()] = prefPrefix
        prefixToPreferred[namespace.lower()] = prefPrefix
        prefixToPreferred[title.lower()] = prefPrefix
        OXO.saveDatasource(prefPrefix, namespace, title, desc, "DATABASE", None, altPrefixes, licence, versionInfo)


#oboDbxrefUrl = 'https://raw.githubusercontent.com/geneontology/go-site/master/metadata/db-xrefs.yaml'
# Read from OBO db-xrefs
yamlData = yaml.load(urllib.urlopen(oboDbxrefUrl))

for database in yamlData:
    namespace= database["database"]
    title = database["name"]
    prefPrefix = namespace

    altPrefixes = [namespace]
    if namespace.lower() in prefixToPreferred:
        print "Ignoring " + namespace + " from OBO as it is already registered as a datasource"
    else:
        urlSyntax = None
        if "entity_types" in database:
            if "url_syntax" in  database["entity_types"][0]:
                urlSyntax = database["entity_types"][0]["url_syntax"].replace("[example_id]", "")
        prefixToPreferred[namespace.lower()] = prefPrefix

        OXO.saveDatasource(prefPrefix, None, title, None, "DATABASE",urlSyntax, altPrefixes, None, None)


# Create Paxo as datasources
print "Save paxo as datasource"
prefPrefix="paxo"
namespace=None
title="Paxo"
desc=None
sourceType="DATABASE"
urlSyntax=None
altPrefixes=["paxo"]
licence=None
versionInfo=0.1
OXO.saveDatasource(prefPrefix, namespace, title, desc, sourceType, urlSyntax, altPrefixes, licence, versionInfo)
