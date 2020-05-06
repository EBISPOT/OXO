#!/usr/bin/env python
"""
This script pulls mappings from a UMLS database and creates a
CSV file of terms and mappings that can be loaded into OxO using the OxoNeo4jLoader.py script.
"""
__author__ = "jupp"
__license__ = "Apache 2.0"
__date__ = "03/03/2018"

import OxoClient
import pymysql
from configparser import ConfigParser
from optparse import OptionParser

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

user=config.get("SQLumls","user")
password=config.get("SQLumls","password")
host=config.get("SQLumls","host")
sqldb=config.get("SQLumls","db")
port=config.getint("SQLumls","port")

db = pymysql.connect(user=user, passwd=password,
                     host=host,
                     db=sqldb, port=port)

OXO = OxoClient.OXO()
OXO.oxoUrl=config.get("Basics","oxoUrl")
OXO.olsurl=config.get("Basics","olsurl")


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

print("Reading datasources from OxO done!")

terms = {}
umlsMapping = {}
mappings = {}
postMappings = []

# print all the first cell of all the rows
idToLabel = {}
def getUMLSMappingFromRow(row, terms, umlsMapping):
    cui = row[0]
    source = row[1]
    toid = row[2]
    descId = row[3]
    label = row[4]
    sourcePreferred = row[5]
    ts = row[6]
    stt = row[7]
    isPref = row[8]

    if descId is not None:
        toid = descId

    if toid is None:
        return None


    if source == "HPO":
        source = OXO.getPrefixFromCui(toid)
        toid = OXO.getIdFromCui(toid)

    fromCurie = "UMLS:" + cui
    toCurie = prefixToPreferred[source] + ":" + toid

    if fromCurie not in terms:
        terms[fromCurie] = {
            "prefix": "UMLS",
            "id": cui,
            "curie": fromCurie,
            "uri": "http://identifiers.org/" + fromCurie,
        }

    if ts == 'P' and stt == 'PF' and isPref == "Y":
        terms[fromCurie]["label"] = label

    if fromCurie not in umlsMapping:
        umlsMapping[fromCurie] = {}
    umlsMapping[fromCurie][toCurie] = 1

    toUri = "http://identifiers.org/" + toCurie
    if source == "HP":
        toUri = "http://purl.obolibrary.org/obo/HP_" + toid

    if toCurie not in terms:
        terms[toCurie] = {
            "prefix": prefixToPreferred[source],
            "id": toid,
            "curie": toCurie,
            "uri": toUri
        }

    if sourcePreferred in ['PT' ,'MH', 'OAP', "NM"]:
        terms[toCurie]["label"] = label


# umls loader
cur = db.cursor()

print("Fetching all mappings from UMLS...")

# get all pref labels for UMLS concepts
getUmlsLabelsSqlQuery = "select distinct cui,sab, scui, sdui, str, tty, ts, stt, ispref from MRCONSO where ts ='P' and stt = 'PF' and ispref = 'Y' and sab != 'src'"
cur.execute(getUmlsLabelsSqlQuery)
fetched=cur.fetchall()

# first get all the UMLS concepts
for row in fetched:
    try:
        getUMLSMappingFromRow(row, terms, umlsMapping)
    except Exception as e:
        print(e)
        print("Experienced a problem with ")
        print(row)
        print("Catched it and try to move on")

print("Fetching all source terms info from from UMLS...")

# now get source term labels
getPreferredLabelFromSource = "select distinct cui,sab, scui, sdui, str, tty, ts, stt, ispref from MRCONSO where  (tty = 'PT' or tty = 'MH' or tty = 'OAP' or tty = 'NM') and  sab != 'src'"
cur.execute(getPreferredLabelFromSource)
fetched=cur.fetchall()

for row in fetched:
    try:
        getUMLSMappingFromRow(row, terms, umlsMapping)
    except Exception as e:
        print(e)
        print("Experienced a problem with ")
        print(row)
        print("Catched it and try to move on")

for formIdKey, toIdValues in umlsMapping.items():
    for toIdKey in toIdValues:
        postMappings.append({
        "fromId": formIdKey,
        "toId": toIdKey,
        "datasourcePrefix": "UMLS",
        "sourceType": "DATABASE",
        "scope": "RELATED"
        }
        )


db.close()
print("Fetching all mappings from UMLS done!")

print("Generating CSV files for neo loading...")

import OxoCsvBuilder
builder = OxoCsvBuilder.Builder()

builder.exportTermsToCsv(exportFileTerms, terms)
builder.exportMappingsToCsv(exportFileMappings, postMappings, prefixToDatasource)


print("Finished process!")
