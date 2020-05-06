#!/usr/bin/env python
"""
Generates a CSV file from sets of OxO datasrouces, terms or mappings
"""
__author__ = "jupp"
__license__ = "Apache 2.0"
__date__ = "04/03/2018"


import csv
import datetime
import json

class Builder:

    def generateAllAltPrefixes(self, alternatePrefixes):
        terms = {}
        for ap in alternatePrefixes:
            terms[ap] = 1
            terms[ap.lower()] = 1
            terms[ap.upper()] = 1

        return list(terms.keys())

    def exportDatasourceToCsv(self, file, datasources):
        with open(file, 'w') as csvfile:
            spamwriter = csv.writer(csvfile, delimiter=',',
                                    quoting=csv.QUOTE_ALL, escapechar='\\', doublequote=False)
            spamwriter.writerow(
                ['prefix', "idorgNamespace", "title", "description", "sourceType", "baseUri", "alternatePrefixes",
                 "licence", "versionInfo"])
            for key, value in datasources.items():
                value.alternatePrefixes.append(key)
                alternatePrefixes = self.generateAllAltPrefixes(value.alternatePrefixes)

                spamwriter.writerow([value.prefPrefix, value.idorgNamespace, str(value.title),
                                     str(value.description), value.sourceType, value.baseUri,
                                     ",".join(alternatePrefixes), str(value.licence),
                                     str(value.versionInfo)])

    def exportTermsToCsv(self, file, terms):
        with open(file, 'w') as csvfile:
            spamwriter = csv.writer(csvfile, delimiter=',',
                                    quoting=csv.QUOTE_ALL, escapechar='\\', doublequote=False)
            spamwriter.writerow(['identifier', "curie", "label", "uri", "prefix"])
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

                spamwriter.writerow([term["id"], term["curie"], label, uri, term["prefix"]])

    def exportMappingsToCsv(self, file, postMappings, prefixToDatasource):

        with open(file, 'w') as csvfile:
            spamwriter = csv.writer(csvfile, delimiter=',',
                                    quoting=csv.QUOTE_ALL, escapechar='\\', doublequote=False)
            spamwriter.writerow(
                ['fromCurie', "toCurie", "datasourcePrefix", "datasource", "sourceType", "scope", "date"])
            for mapping in postMappings:
                datasource = prefixToDatasource[mapping["datasourcePrefix"]]
                spamwriter.writerow(
                    [mapping["fromId"], mapping["toId"], mapping["datasourcePrefix"], json.dumps(datasource),
                     mapping["sourceType"], mapping["scope"], datetime.datetime.now().strftime("%y-%m-%d")])
