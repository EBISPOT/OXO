#!/usr/bin/env python
"""
Python client to the OxO REST API
"""

import urllib.request, urllib.parse, urllib.error
import requests
import json
#from ConfigParser import SafeConfigParser

class Datasource:
    def __init__(self, prefPrefix, idorgNamespace, title, description, sourceType, baseUri, alternatePrefixes, licence, versionInfo):
        self.prefPrefix = prefPrefix
        self.idorgNamespace = idorgNamespace
        self.title = title
        self.description = description
        self.sourceType = sourceType
        self.baseUri = baseUri
        self.alternatePrefixes = alternatePrefixes
        self.licence = licence
        self.versionInfo = versionInfo

#
# class Term:
#     def __init__(self):
#
# class Mapping:
#     def __init__(self):

class OXO:
    def __init__(self, ):
        self.oxoUrl = ""
        self.apikey = ""
        self.olsUrl = ""

        self.alreadyScoped = {}
        self.olsLabel = {}
        self.olsIri = {}

    def saveDatasource (self, prefix, idorgNamespace, title, description, sourceType, baseUri, alternatePrefixes, licence, versionInfo):
        #print "saving new datasource: {},{},{},{},{},{}".format(prefix, idorgNamespace, title, sourceType, baseUri, alternatePrefixes)
        if not baseUri:
            baseUri = []
        else:
            baseUri = [baseUri]

        postdata = {
                "prefix": prefix,
                "idorgNamespace": idorgNamespace,
                "alternatePrefix": alternatePrefixes,
                "alternateIris": baseUri,
                "name": title,
                "description": description,
                "licence": licence,
                "versionInfo": versionInfo,
                "source": sourceType
            }

        url  = self.oxoUrl+"/api/datasources?apikey="+self.apikey
        headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
        #print url
        r = requests.post(url, data=json.dumps(postdata), headers=headers)
        if r.status_code != requests.codes.ok:
            print(r)
            print(r.text)
            print(r.status_code)
            print(json.loads(r.text)["message"])

    def saveMapping(self, fromPrefix, fromId, fromLabel, fromUri, mappingSourceId, toPrefix, toId, sourceType):

        fromCurie = fromPrefix+":"+fromId
        toCurie = toPrefix+":"+toId

        url = self.oxoUrl+"/api/mappings?apikey="+self.apikey
        headers = {'Content-type': 'application/json', 'Accept': 'application/json'}

        postdata = {
                "fromId": fromCurie,
                "toId": toCurie,
                "datasourcePrefix": mappingSourceId,
                "sourceType": sourceType,
                "scope": "RELATED"
            }
       # print "saving new mapping: {} -> {} -> {}".format(fromCurie, mappingSourceId, toCurie)

        r = requests.post(url, data=json.dumps(postdata), headers=headers)

        if r.status_code != requests.codes.ok:
            print(json.loads(r.text)["message"])

    def saveMappings(self, mappings):

        url = self.oxoUrl+"/api/mappings?apikey="+self.apikey
        headers = {'Content-type': 'application/json', 'Accept': 'application/json'}

       # print "saving new mapping: {} -> {} -> {}".format(fromCurie, mappingSourceId, toCurie)

        r = requests.post(url, data=json.dumps(mappings), headers=headers)

        if r.status_code != requests.codes.ok:
            print(json.loads(r.text)["message"])

    def updateTerm(self, curie, iri, label):
        # if iri and label then patch
        headers = {'Content-type': 'application/json', 'Accept': 'application/json'}

        params=None
        if iri and label:
            params = "uri=" + urllib.parse.quote_plus(iri.encode('utf-8'))+"&label=" + urllib.parse.quote_plus(label.encode('utf-8'))
        if not iri and not label:
            iriMap = self.getIriAndLabelFromOls(curie)
            if iriMap:
                olsIri = iriMap["uri"]
                if olsIri:
                    params = "uri=" + urllib.parse.quote_plus(olsIri.encode('utf-8'))
                    olsLabel = iriMap["label"]
                    if olsLabel:
                        params = "uri=" + urllib.parse.quote_plus(olsIri.encode('utf-8')) + "&label=" + urllib.parse.quote_plus(olsLabel.encode('utf-8'))
        if not iri and label:
            params = "label=" + urllib.parse.quote_plus(label.encode('utf-8'))


        if params:
            url = self.oxoUrl+"/api/terms/" + curie + "?" + params + "&apikey="+self.apikey
            r = requests.patch(url, data=None, headers=headers)
            return r.status_code == requests.codes.ok
        return False


    def getIriAndLabelFromOls(self, curie):
        if curie in self.olsIri:
            return {"uri" : self.olsIri[curie], "label": self.olsLabel[curie]}
        else:
            query = self.olsurl+"/terms?obo_id="+curie
            reply = urllib.request.urlopen(query)
            if reply.getcode() == 200:
                anwser = json.load(reply)
                if "_embedded" in list(anwser.keys()):
                    terms = anwser["_embedded"]["terms"]
                    label = None
                    uri = None
                    for term in terms:
                        label = term["label"]
                        uri = term["iri"]
                        is_defining_ontology = term["is_defining_ontology"]
                        if is_defining_ontology:
                            self.olsLabel[curie] = label
                            self.olsIri[curie] = uri
                            return {'uri': uri, 'label': label}
                    self.olsLabel[curie] = label
                    self.olsIri[curie] = uri
                    return {'uri': uri, 'label': label}
        return None

    def getLabelFromOls(self, curie):

        if self.olsLabel[curie]:
            return self.olsLabel[curie]
        else:
            olsurl = self.olsurl+"/terms?obo_id="+curie
            reply = urllib.request.urlopen(olsurl)
            if reply.getcode() == 200:
                anwser = json.load(reply)
                if "_embedded" in list(anwser.keys()):
                    terms = anwser["_embedded"]["terms"]
                    label = None
                    uri = None
                    for term in terms:
                        label = term["label"]
                        uri = term["iri"]
                        is_defining_ontology = term["is_defining_ontology"]
                        if is_defining_ontology:
                            self.olsLabel[curie] = label
                            self.olsIri[curie] = uri
                            return label
                    self.olsLabel[curie] = label
                    self.olsIri[curie] = uri
                    return label
        return None

    def getScopeFromOls(self, curie, ontology, target):

        if target in self.alreadyScoped:
            return self.alreadyScoped[target]

        olsurl = self.olsurl + "/ontologies/"+ontology+"/terms?obo_id=" + curie
        reply = urllib.request.urlopen(olsurl)
        if reply.getcode() == 200:
            anwser = json.load(reply)
            if "_embedded" in list(anwser.keys()):
                terms = anwser["_embedded"]["terms"]
                for term in terms:
                    if "obo_xref" in term:
                        for xref in term["obo_xref"]:
                            if "description" in xref:
                                if xref["description"]:
                                    scope = "RELATED"
                                    if "MONDO:equivalentTo" in xref["description"]:
                                        scope = "EXACT"
                                    elif "exact mapping" in xref["description"]:
                                        scope = "EXACT"
                                    elif "BTNT" in xref["description"]:
                                        scope = "NARROWER"
                                    elif "NTBT" in xref["description"]:
                                        scope = "BROADER"
                                    self.alreadyScoped[target] = scope
                                    return scope


    def getOxODatasets(self):
        url = self.oxoUrl + "/api/datasources?size=4000"
        print("querying " + url)
        reply = urllib.request.urlopen(url)
        answer = json.load(reply)
        if "_embedded" not in answer:
            return []
        return answer["_embedded"]["datasources"]

    def getPrefixFromCui (self, id):

        if ":" in id and len(id.split(":")) == 2:
            return id.split(":")[0]

        if "_" in id and len(id.split("_")) == 2:
            return id.split("_")[0]

        return None

    def getIdFromCui (self, id):

        if ":" in id and len(id.split(":")) == 2:
            return id.split(":")[1]

        if "_" in id and len(id.split("_")) == 2:
            return id.split("_")[1]

        return None
