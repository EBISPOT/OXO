import pymysql
import OxoClient as OXO
from pip._vendor.requests.packages.urllib3.connection import port_by_scheme
import urllib.request, urllib.parse, urllib.error
import json
import xml.etree.ElementTree as ET
import yaml
import csv
import sys
import datetime
from neo4j.v1 import GraphDatabase, basic_auth
from configparser import SafeConfigParser


#Parse the input parameters. A config file and a flag is expected
if len(sys.argv)!=2:
    print("\nNot enough arguments! Please pass a (path) of a config file!")
    raise Exception("Not enough arguments! Please pass in a config file!")
else:
    config = SafeConfigParser()
    config.read(sys.argv[1])



#config = SafeConfigParser()
#config.read("../config/oxo_dataRelease_config.ini")

OXO.oxoUrl=config.get("Basics","oxoUrl")
OXO.apikey=config.get("Basics","oxoAPIkey")
#OXO.olsurl=config.get("Basics","olsurl")
olsurl=config.get("Basics","olsurl")

solrBaseUrl=config.get("Basics","solrBaseUrl")
getEfoAnnotationsUrl = solrBaseUrl+"/ontology/select?q=*%3A*&fq=ontology_name%3Aefo&rows=0&wt=csv&indent=true"
efoSolrQueryUrl = solrBaseUrl+"/ontology/select?fq=ontology_name%3Aefo&q=*&wt=json"
olsDbxerfSolrQuery = solrBaseUrl+"/ontology/select?q=hasDbXref_annotation%3A*+OR%0Adatabase_cross_reference_annotation%3A*+OR%0Ahas_alternative_id_annotation%3A*+OR%0Adefinition_citation_annotation%3A*&fl=iri%2Contology_name%2Clabel%2Cshort_form%2Cobo_id%2Cdatabase_cross_reference_annotation%2ChasDbXref_annotation%2C+definition_citation_annotation%2C+has_alternative_id_annotation+&wt=json&fq=!ontology_name%3Ancbitaxon&fq=!ontology_name%3Apr&fq=!ontology_name%3Avto&fq=!ontology_name%3Aogg"

solrChunks=config.getint("Basics","solrChunks")
uri=config.get("Basics","neoURL")

exportFileTerms=config.get("Paths","exportFileTerms")
exportFileMappings=config.get("Paths","exportFileMappings")

user=config.get("SQLumls","user")
password=config.get("SQLumls","password")
host=config.get("SQLumls","host")
sqldb=config.get("SQLumls","db")
port=config.getint("SQLumls","port")


driver = GraphDatabase.driver(uri, auth=basic_auth("neo4j", "dba"))
session = driver.session()
print("neo success no sql")
db = pymysql.connect(user=user, passwd=password,
                     host=host,
                     db=sqldb, port=port)


# OLS loader
# get prefix data from OLS
prefixToPreferred = {}
termToIri = {}
termToLabel = {}
idorgNamespace = {}
prefixToDatasource = {}

print("Reading datasources from OxO...")
for data in OXO.getOxODatasets():
    del data['_links']
    del data['description']
    prefix = data["prefix"]
    prefixToDatasource[prefix] = data
    prefixToPreferred[prefix] = prefix
    for altPrefix in data["alternatePrefix"]:
        prefixToPreferred[altPrefix] = prefix
        if "idorgNamespace" in data:
            idorgNamespace[altPrefix.lower()] = data["idorgNamespace"]
            idorgNamespace[prefix.lower()] = data["idorgNamespace"]
# get total number of results

knownAnnotations = [
    "database_cross_reference_annotation",
    "hasDbXref_annotation"
]

print("Reading datasources from OxO done")
# hack to get EFO xref annotations

response = urllib.request.urlopen(getEfoAnnotationsUrl)
cr = csv.reader(response)
for row in cr:
    for p in row:
        if 'definition_citation' in p:
            knownAnnotations.append(p)

print("\n knownAnnotations")
print(knownAnnotations)


unknownSource = {}

terms = {}
mappings = {}
postMappings = []

def processSolrDocs(url):
    rows = solrChunks
    initUrl = url + "&start=0&rows=" + str(rows)
    reply = urllib.request.urlopen(initUrl)
    anwser = json.load(reply)

    size = anwser["response"]["numFound"]


    for x in range(rows, size, rows):
        for docs in anwser["response"]["docs"]:
            fromPrefix = None
            fromId = None

            fromIri = docs["iri"]
            fromShortForm = docs["short_form"]
            fromOntology = docs["ontology_name"]
            fromLabel = docs["label"]

            if "obo_id" in docs:
                fromOboId = docs["obo_id"]
                fromPrefix = OXO.getPrefixFromCui(fromOboId)
                fromId = OXO.getIdFromCui(fromOboId)

            #if fromPrefix=="orphanet":
            #   than use OLS API to check if it is an exact Match

            if not fromPrefix and not fromId:
                fromPrefix = OXO.getPrefixFromCui(fromShortForm)
                fromId = OXO.getIdFromCui(fromShortForm)

            if not fromPrefix:
                print("Can't determine prefix for " + fromShortForm + " so skipping")
                continue

            if not fromId:
                print("Can't determine id for " + fromShortForm + " so skipping")
                continue
            # do we know the source term from the prefix?

            if fromPrefix not in prefixToPreferred:
                print("unknown prefix " + fromPrefix + " so skipping")
                continue

            fromPrefix = prefixToPreferred[fromPrefix]
            fromCurie = fromPrefix + ":" + fromId

            if fromCurie not in terms:
                terms[fromCurie] = {
                    "prefix": fromPrefix,
                    "id": fromId,
                    "curie": fromCurie,
                    "uri": fromIri,
                    "label": fromLabel
                }
            else:
                terms[fromCurie]["uri"] = fromIri
                terms[fromCurie]["label"] = fromLabel

            for anno in knownAnnotations:
                if anno in docs:
                    for xref in docs[anno]:
                        if ":" in xref or "_" in xref:
                            toPrefix = OXO.getPrefixFromCui(xref)
                            toId = OXO.getIdFromCui(xref)

                            if not toPrefix or not toId:
                                print("Can't get prefix or id for " + xref.encode('utf-8'))
                                continue

                            if not toPrefix:
                                print("Can't extract prefix for " + xref.encode('utf-8'))
                                continue
                            if toPrefix.lower() not in prefixToPreferred:
                                unknownSource[toPrefix] = 1
                                # print "Unknown prefix source for "+toPrefix+" so skipping"
                                continue


                            toPrefix = prefixToPreferred[toPrefix.lower()]
                            toCurie = toPrefix + ":" + toId

                            if toCurie not in terms:
                                terms[toCurie] = {
                                    "prefix": toPrefix,
                                    "id": toId,
                                    "curie": toCurie,
                                    "uri": None,
                                    "label":None
                                }

                            if fromCurie == toCurie:
                                continue


                            if fromOntology not in  prefixToPreferred:
                                print("mapping from unknown source " + fromOntology)
                                continue
                            mapping = {
                                "fromId": fromCurie,
                                "toId": toCurie,
                                "datasourcePrefix": prefixToPreferred[fromOntology],
                                "sourceType": "ONTOLOGY",
                                "scope": "RELATED"
                            }

                            postMappings.append(mapping)

                            # if fromCurie not in termToIri:
                            #     termToIri[fromCurie] = None
                            # if fromCurie not in termToLabel:
                            #     termToLabel[fromCurie] = None
                            # if toCurie not in termToIri:
                            #     termToIri[toCurie] = None
                            # if toCurie not in termToLabel:
                            #     termToLabel[toCurie] = None

                            # if to id is idorg, then mint the Uri
                            if idorgNamespace[toPrefix.lower()] is not None:
                                idorgUri = "http://identifiers.org/" + idorgNamespace[toPrefix.lower()] + "/" + toId
                                terms[toCurie]["uri"] = idorgUri

        print(str(x))
        # OXO.saveMappings(postMappings)
        # postMappings = []
        initUrl = url + "&start=" + str(x) + "&rows=" + str(rows)
        reply = urllib.request.urlopen(initUrl)
        anwser = json.load(reply)


# do the query to get docs from solr and process

processSolrDocs(efoSolrQueryUrl)
print("Done processing EFO, starting to query OLS")
processSolrDocs(olsDbxerfSolrQuery)
print("Done processing OLS")

#terms={ "DOID:0080184" :{"prefix": "DOID",
#        "id": "0080184",
#        "curie": "DOID:0080184",
#        "uri": None,
#        "label":None}
#    }


print("Looking for OLS terms with no labels...")
for key, term in terms.items():
    if term["label"] is None:
        prefix = OXO.getPrefixFromCui(key)
        if prefixToDatasource[prefixToPreferred[prefix]]["source"] == "ONTOLOGY":
            object = OXO.getIriAndLabelFromOls(term["curie"], olsurl)
            if object is not None:
                if term["uri"] is None:
                    terms[key]["uri"] = object["uri"]
                if term["label"] is None:
                    terms[key]["label"] = object["label"]
            else:
                print("Object None!")
                print(object)
                print(terms[key])




#url = "http://www.ebi.ac.uk/ols/api/search?q=*&fieldList=iri,short_form,obo_id,database_cross_reference_annotation"
#print "Updating term labels"
# update URIs and labels for any terms we have seen
#for id in termToIri:
#    if id not in termToIri and id not in termToLabel:
#         print "Can't determine iri or label for "+id
#    else:
#         OXO.updateTerm(id, termToIri[id], termToLabel[id])


# dump out the list of unkonwn sources
print("Finished, here are all the unknown sources")
for key, value in unknownSource.items() :
    # see if we can match prefix to db
    print(key.encode('utf-8', 'ignore'))


# print all the first cell of all the rows
idToLabel = {}
def getUMLSMappingFromRow(row):
    cui =  row[0]
    source = row[1]
    toid = row[2]
    descId =  row[3]
    label = row[4]

    if descId is not None:
        toid = descId

    if toid is None:
        return None

    if source == "HPO":
        source = OXO.getPrefixFromCui(toid)
        toid = OXO.getIdFromCui(toid)

    fromCurie = "UMLS:" + cui

    toCurie = prefixToPreferred[source] + ":" + toid


#### Do the if-else things here prevent empty labels??
    if fromCurie not in terms:
        terms[fromCurie] = {
            "prefix": "UMLS",
            "id": cui,
            "curie": fromCurie,
            "uri": "http://identifiers.org/umls/"+cui,
            "label": label
        }
    else:
        if label!="":
            terms[fromCurie]["label"] = label
        else:
            print("FROM UMLS label is none for ")
            print(fromCurie)

    if toCurie not in terms:
        terms[toCurie] = {
            "prefix": prefixToPreferred[source],
            "id": toid,
            "curie": toCurie,
            "label": label,
            "uri": None
        }
    else:
        if label!="":
            terms[toCurie]["label"] = label
        else:
            print("FROM UMLS - label is NONE! for")
            print(toCurie)
#### End empty labels

    if idorgNamespace[source.lower()]:
        terms[toCurie]["uri"] = "http://identifiers.org/"+idorgNamespace[source.lower()]+"/"+toid

    mapping = {
        "fromId": fromCurie,
        "toId": toCurie,
        "datasourcePrefix": "UMLS",
        "sourceType": "DATABASE",
        "scope": "RELATED"
    }
    # idToLabel[source+":"+toid] = label
    return mapping



# umls loader
cur = db.cursor()
# Use all the SQL you like

#cur.execute("select distinct cui,sab, scui, sdui, str from MRCONSO where stt = 'PF' and (ts = 'P' or tty='PT') and sab != 'src'")
# --> missing Snomed labels 6613 (down from )

# https://www.ncbi.nlm.nih.gov/books/NBK9685/
# STT	String type
# TS	Term status
# SAB	Abbreviated source name (SAB).

cur.execute("select distinct cui,sab, scui, sdui, str from MRCONSO where stt = 'PF' and (ts = 'P' or ts='S') and sab != 'src'")

fetched=cur.fetchall()

# Previously, 'old sql query'
#cur.execute("select distinct cui,sab, scui, sdui, str from MRCONSO where stt = 'PF' and tty = 'PT' and sab != 'src'")
#fetched=cur.fetchall()

#if len(fetched)==0:
#     cur.execute("select distinct cui,sab, scui, sdui, str from MRCONSO where stt = 'PF' and tty = 'PT' and sab != 'src'")
#     fetched=cur.fetchall()

for row in fetched:
    try:
        mappingRow = getUMLSMappingFromRow(row)
        if mappingRow is not None:
            postMappings.append(mappingRow)
    except Exception as e:
        print(e)
        print("Experienced a problem with ")
        print(row)
        print("Catched it and try to move on")
        #Experienced a problem with  ('C1180021', 'NCI', 'C33333', None, 'Plus End of the Microtubule')
        #('C0796501', 'NCI', 'C11519', None, 'Asparaginase/Dexamethasone/Prednisone/Vincristine')

db.close()



print()
print("Generating CSV files for neo loading...")

with open(exportFileTerms, 'w') as csvfile:
    spamwriter = csv.writer(csvfile, delimiter=',',
                            quoting=csv.QUOTE_ALL, escapechar='\\',doublequote=False)
    spamwriter.writerow(['identifier', "curie", "label","uri", "prefix" ])
    for key, term in terms.items():
        label = None
        uri = None

        try:
            if term["label"] is not None:
                label = term["label"].encode('utf-8', errors="ignore")
        except:
            pass

        if term["uri"] is not None:
            uri = term["uri"]

        spamwriter.writerow( [term["id"], term["curie"], label, uri, term["prefix"] ])

with open(exportFileMappings, 'w') as csvfile:
    spamwriter = csv.writer(csvfile, delimiter=',',
                            quoting=csv.QUOTE_ALL, escapechar='\\',doublequote=False)
    spamwriter.writerow(['fromCurie', "toCurie","datasourcePrefix","datasource","sourceType","scope","date" ])
    for mapping in postMappings:
        datasource = prefixToDatasource[mapping["datasourcePrefix"]]
        spamwriter.writerow( [mapping["fromId"],mapping["toId"],mapping["datasourcePrefix"],json.dumps(datasource),mapping["sourceType"],mapping["scope"],  datetime.datetime.now().strftime("%y-%m-%d")])

print("Generating CSV files for neo loading done, now loading them...")

# CREATE CONSTRAINT ON (i:Term) ASSERT i.curie IS UNIQUE
# CREATE CONSTRAINT ON (i:Datasource) ASSERT i.prefix IS UNIQUE

#
def deleteMappings():
    result = session.run("match (t)-[m:MAPPING]->() WITH m LIMIT 50000 DETACH DELETE m RETURN count(*) as count")
    for record in result:
        return record["count"]
print("Deleting mappings...")
while deleteMappings() > 0:
    print("Still deleting...")
print("Mappings deleted!")

print("Deleting previous has_source")
def deleteSourceRels():
    result = session.run("match (t)-[m:HAS_SOURCE]->()  WITH m LIMIT 50000 DETACH DELETE m RETURN count(*) as count")
    for record in result:
        return record["count"]
while deleteSourceRels() > 0:
    print("Still deleting...")
print("Source rels deleted!")

print("Deleting previous terms")
def deleteTerms():
    result = session.run("match (t:Term) WITH t LIMIT 50000 DETACH DELETE t RETURN count(*) as count")
    for record in result:
        return record["count"]
while deleteTerms() > 0:
    print("Still deleting...")
print("Terms deleted!")

print("Loading terms.csv...")
loadTermsCypher = "USING PERIODIC COMMIT 10000 LOAD CSV WITH HEADERS FROM 'file:///"+exportFileTerms+"""' AS line
                MATCH (d:Datasource {prefix : line.prefix})
                WITH d, line
                MERGE (t:Term { id: line.identifier, curie: line.curie, label: line.label, uri: line.uri})
                with t,d
                CREATE (t)-[:HAS_SOURCE]->(d)"""
result = session.run(loadTermsCypher)
print(result.summary())

print("Loading mappings.csv...")
loadMappingsCypher = "USING PERIODIC COMMIT 10000 LOAD CSV WITH HEADERS FROM 'file:///"+exportFileMappings+"""' AS line
                    MATCH (f:Term { curie: line.fromCurie}),(t:Term { curie: line.toCurie})
                    WITH f,t,line
                    CREATE (f)-[m:MAPPING { sourcePrefix: line.datasourcePrefix, datasource: line.datasource, sourceType: line.sourceType, scope: line.scope, date: line.date}]->(t)"""

result = session.run(loadMappingsCypher)
print(result.summary())

#After Loading, update indexes
print("updating indexes")
reply = urllib.request.urlopen(OXO.oxoUrl+"/api/search/rebuild?apikey="+OXO.apikey)
print("Finished process!")
