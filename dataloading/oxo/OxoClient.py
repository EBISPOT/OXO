import urllib
import requests
import json
#from ConfigParser import SafeConfigParser

def saveDatasource (prefix, idorgNamespace, title, description, sourceType, baseUri, alternatePrefixes, licence, versionInfo):
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

    url  = oxoUrl+"/api/datasources?apikey="+apikey
    headers = {'Content-type': 'application/json', 'Accept': 'application/json'}
    #print url
    r = requests.post(url, data=json.dumps(postdata), headers=headers)
    if r.status_code != requests.codes.ok:
        print r
        print r.text
        print r.status_code
        print json.loads(r.text)["message"]

def saveMapping(fromPrefix, fromId, fromLabel, fromUri, mappingSourceId, toPrefix, toId, sourceType):

    fromCurie = fromPrefix+":"+fromId
    toCurie = toPrefix+":"+toId

    url = oxoUrl+"/api/mappings?apikey="+apikey
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
        print json.loads(r.text)["message"]

def saveMappings(mappings):

    url = oxoUrl+"/api/mappings?apikey="+apikey
    headers = {'Content-type': 'application/json', 'Accept': 'application/json'}

   # print "saving new mapping: {} -> {} -> {}".format(fromCurie, mappingSourceId, toCurie)

    r = requests.post(url, data=json.dumps(mappings), headers=headers)

    if r.status_code != requests.codes.ok:
        print json.loads(r.text)["message"]

def updateTerm(curie, iri, label):
    # if iri and label then patch
    headers = {'Content-type': 'application/json', 'Accept': 'application/json'}

    params=None
    if iri and label:
        params = "uri=" + urllib.quote_plus(iri.encode('utf-8'))+"&label=" + urllib.quote_plus(label.encode('utf-8'))
    if not iri and not label:
        iriMap = getIriAndLabelFromOls(curie)
        if iriMap:
            olsIri = iriMap["uri"]
            if olsIri:
                params = "uri=" + urllib.quote_plus(olsIri.encode('utf-8'))
                olsLabel = iriMap["label"]
                if olsLabel:
                    params = "uri=" + urllib.quote_plus(olsIri.encode('utf-8')) + "&label=" + urllib.quote_plus(olsLabel.encode('utf-8'))
    if not iri and label:
        params = "label=" + urllib.quote_plus(label.encode('utf-8'))


    if params:
        url = oxoUrl+"/api/terms/" + curie + "?" + params + "&apikey="+apikey
        r = requests.patch(url, data=None, headers=headers)
        return r.status_code == requests.codes.ok
    return False


olsLabel = {}
olsIri = {}
def getIriAndLabelFromOls(curie, olsurl):
    if curie in olsIri:
        return {"uri" : olsIri[curie], "label": olsLabel[curie]}
    else:
        olsurl = olsurl+"/terms?obo_id="+curie
        reply = urllib.urlopen(olsurl)
        if reply.getcode() == 200:
            anwser = json.load(reply)
            if "_embedded" in anwser.keys():
                terms = anwser["_embedded"]["terms"]
                label = None
                uri = None
                for term in terms:
                    label = term["label"]
                    uri = term["iri"]
                    is_defining_ontology = term["is_defining_ontology"]
                    if is_defining_ontology:
                        olsLabel[curie] = label
                        olsIri[curie] = uri
                        return {'uri': uri, 'label': label}
                olsLabel[curie] = label
                olsIri[curie] = uri
                return {'uri': uri, 'label': label}
    return None

def getLabelFromOls(curie):

    if olsLabel[curie]:
        return olsLabel[curie]
    else:
        olsurl = olsurl+"/terms?obo_id="+curie
        reply = urllib.urlopen(olsurl)
        if reply.getcode() == 200:
            anwser = json.load(reply)
            if "_embedded" in anwser.keys():
                terms = anwser["_embedded"]["terms"]
                label = None
                uri = None
                for term in terms:
                    label = term["label"]
                    uri = term["iri"]
                    is_defining_ontology = term["is_defining_ontology"]
                    if is_defining_ontology:
                        olsLabel[curie] = label
                        olsIri[curie] = uri
                        return label
                olsLabel[curie] = label
                olsIri[curie] = uri
                return label
    return None

def getOxODatasets():
    url = oxoUrl + "/api/datasources?size=4000"
    reply = urllib.urlopen(url)
    anwser = json.load(reply)
    return anwser["_embedded"]["datasources"]

def getPrefixFromCui (id):

    if ":" in id and len(id.split(":")) == 2:
        return id.split(":")[0]

    if "_" in id and len(id.split("_")) == 2:
        return id.split("_")[0]

    return None

def getIdFromCui (id):

    if ":" in id and len(id.split(":")) == 2:
        return id.split(":")[1]

    if "_" in id and len(id.split("_")) == 2:
        return id.split("_")[1]

    return None
