#!/usr/bin/env python
"""
This script pulls mappings from a UMLS database and creates a
CSV file of terms and mappings that can be loaded into OxO using the OxoNeo4jLoader.py script.
"""
__author__ = "jupp"
__license__ = "Apache 2.0"
__date__ = "03/03/2018"

import OxoClient
import MySQLdb
from ConfigParser import SafeConfigParser
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-t", "--terms", help="terms csv export file")
parser.add_option("-m", "--mappings", help="mappings csv export file")
parser.add_option("-c", "--config", help="config file", default="config.ini")

(options, args) = parser.parse_args()

config = SafeConfigParser()
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

db = MySQLdb.connect(user=user, passwd=password,
                     host=host,
                     db=sqldb, port=port)

OXO = OxoClient.OXO()

# OLS loader
# get prefix data from OLS
prefixToPreferred = {}
termToIri = {}
termToLabel = {}
idorgNamespace = {}
prefixToDatasource = {}

print "Reading datasources from OxO..."
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

terms = {}
mappings = {}
postMappings = []

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
            print "FROM UMLS label is none for "+fromCurie

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
            print "FROM UMLS - label is NONE! for"
            print toCurie

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
        print e
        print "Experienced a problem with "
        print row
        print "Catched it and try to move on"
        #Experienced a problem with  ('C1180021', 'NCI', 'C33333', None, 'Plus End of the Microtubule')
        #('C0796501', 'NCI', 'C11519', None, 'Asparaginase/Dexamethasone/Prednisone/Vincristine')

db.close()

print "Generating CSV files for neo loading..."

import OxoCsvBuilder
builder = OxoCsvBuilder.Builder()

builder.exportTermsToCsv(exportFileTerms, terms)
builder.exportMappingsToCsv(exportFileTerms, terms, prefixToDatasource)


print "Finished process!"
