
#!/usr/bin/env python
"""
This script pulls mappings from OLS and creates a
CSV file of terms and mappings that can be loaded into OxO using the OxoNeo4jLoader.py script.
"""
__author__ = "jupp"
__license__ = "Apache 2.0"
__date__ = "03/03/2018"


import OxoClient
import urllib
import json
import csv
from ConfigParser import SafeConfigParser
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-t", "--terms", help="terms csv export file")
parser.add_option("-m", "--mappings", help="mappings csv export file")
parser.add_option("-c", "--config", help="config file", default="config.ini")

(options, args) = parser.parse_args()

config = SafeConfigParser()
config.read(options.config)

OXO = OxoClient.OXO()

OXO.oxoUrl=config.get("Basics","oxoUrl")
OXO.olsurl=config.get("Basics","olsurl")

solrBaseUrl=config.get("Basics","solrBaseUrl")

exportFileTerms= config.get("Paths","exportFileTerms")
if options.terms:
    exportFileTerms = options.terms

exportFileMappings=config.get("Paths","exportFileMappings")
if options.mappings:
    exportFileMappings = options.mappings

efoAnnotationsParams = {
    'q' :  '*:*',
    'fq' : 'ontology_name:efo',
    'rows' : 0,
    'wt' : 'csv'
}
getEfoAnnotationsUrl = solrBaseUrl+"/ontology/select?"+urllib.urlencode(efoAnnotationsParams)

efoSolrQueryParams = {
    'fq': 'ontology_name:"efo" AND type:"class" AND is_obsolete:false',
    'q':'*',
    'wt':'json',
}
efoSolrQueryUrl = solrBaseUrl+"/ontology/select?"+urllib.urlencode(efoSolrQueryParams)

olsDbxrefSolrParams = {
    'q':'hasDbXref_annotation:* OR database_cross_reference_annotation:* OR has_alternative_id_annotation:* OR definition_citation_annotation:*',
    'fl':'iri,ontology_name,label,short_form,obo_id,database_cross_reference_annotation,hasDbXref_annotation, definition_citation_annotation, has_alternative_id_annotation',
    'wt':'json',
    'fq':'type:"class" AND !ontology_name: (ncbitaxon OR pr OR vto OR ogg) AND is_obsolete:false',
}
olsDbxrefSolrQuery = solrBaseUrl + "/ontology/select?" + urllib.urlencode(olsDbxrefSolrParams)

solrChunks=config.getint("Basics","solrChunks")



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
        if "idorgNamespace" in data and  data["idorgNamespace"] != '':
            idorgNamespace[altPrefix.lower()] = data["idorgNamespace"]
            idorgNamespace[prefix.lower()] = data["idorgNamespace"]
print "Reading datasources from OxO done"

# these are the annotation properties where we look for xrefs
knownAnnotations = [
    "database_cross_reference_annotation",
    "hasDbXref_annotation"
]

# find all the EFO xref annotation propertied
# note EFO does xrefs in a different way to all the other OBO ontologies so
# give it special consideration
response = urllib.urlopen(getEfoAnnotationsUrl)
cr = csv.reader(response)
for row in cr:
    for p in row:
        if 'definition_citation' in p:
            knownAnnotations.append(p)

unknownSource = {}

terms = {}
mappings = {}
postMappings = []

# main function that gets crawls the OLS Solr documents for xrefs
# We use the Solr endpoint directly instead of the OLS API as we can restrict the query
# to only terms that have an xref. In the future we may add this functionality to the OLS API
def processSolrDocs(url):
    rows = solrChunks
    initUrl = url + "&start=0&rows=" + str(rows)
    reply = urllib.urlopen(initUrl)
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

            if not fromPrefix and not fromId:
                fromPrefix = OXO.getPrefixFromCui(fromShortForm)
                fromId = OXO.getIdFromCui(fromShortForm)

            if not fromPrefix:
                print "Can't determine prefix for " + fromShortForm + " so skipping"
                continue

            if not fromId:
                print "Can't determine id for " + fromShortForm + " so skipping"
                continue
            # do we know the source term from the prefix?

            if fromPrefix not in prefixToPreferred:
                print "unknown prefix " + fromPrefix + " so skipping"
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
                                print "Can't get prefix or id for " + xref.encode('utf-8')
                                continue

                            if not toPrefix:
                                print "Can't extract prefix for " + xref.encode('utf-8')
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
                                print "mapping from unknown source " + fromOntology
                                continue

                            # we know some ontologies have information about BT/NT or exact mappings. This needs to be better exposed in
                            # the OLS API. For now we know that for MONDO and Orphanet we should be using these qualifiers
                            scope = "RELATED"
                            if fromOntology == "mondo" or (fromOntology == "ordo" and toPrefix == "OMIM"):
                                scope = OXO.getScopeFromOls(fromCurie, fromOntology, toCurie)

                            mapping = {
                                "fromId": fromCurie,
                                "toId": toCurie,
                                "datasourcePrefix": prefixToPreferred[fromOntology],
                                "sourceType": "ONTOLOGY",
                                "scope": scope
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
                            if toPrefix.lower() in idorgNamespace:
                                idorgUri = "http://identifiers.org/" + idorgNamespace[toPrefix.lower()] + "/" + toId
                                terms[toCurie]["uri"] = idorgUri

        print str(x)
        initUrl = url + "&start=" + str(x) + "&rows=" + str(rows)
        reply = urllib.urlopen(initUrl)
        anwser = json.load(reply)


# do the query to get docs from solr and process

processSolrDocs(efoSolrQueryUrl)
print "Done processing EFO, starting to query OLS"
processSolrDocs(olsDbxrefSolrQuery)
print "Done processing OLS"

print "Looking for OLS terms with no labels..."
for key, term in terms.iteritems():
    if term["label"] is None:
        prefix = OXO.getPrefixFromCui(key)
        if prefix == "NCIT":
            continue
        if prefixToDatasource[prefixToPreferred[prefix]]["source"] == "ONTOLOGY":
            object = OXO.getIriAndLabelFromOls(term["curie"])
            if object is not None:
                if term["uri"] is None:
                    terms[key]["uri"] = object["uri"]
                if term["label"] is None:
                    terms[key]["label"] = object["label"]
            else:
                print "No label found for" + terms[key]

# dump out the list of unkonwn sources
print "Finished, here are all the unknown sources"
for key, value in unknownSource.iteritems() :
    # see if we can match prefix to db
    print key.encode('utf-8', 'ignore')

print "Generating CSV files for neo loading..."


import OxoCsvBuilder
builder = OxoCsvBuilder.Builder()

builder.exportTermsToCsv(exportFileTerms, terms)
builder.exportMappingsToCsv(exportFileMappings, postMappings, prefixToDatasource)

print "Finished process!"
