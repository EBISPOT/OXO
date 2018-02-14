import time
import logging
import requests
import Levenshtein
#from ConfigParser import SafeConfigParser

from flask import Flask
from flask import request
from flask import jsonify
app = Flask(__name__)

@app.route("/")
def hello():
    return "Hello World!"

#Offers an endpoint for scoreTermLabel
@app.route("/scoreTermLabel/")
def jsonScoreTermLabel():
    label = request.args.get('termLabel', default = 1, type = str)
    targetOntology = request.args.get('targetOntology', default = 1, type = str)
    scoredTerm=scoreTermLabel(label, targetOntology, {})
    return jsonify(scoredTerm[0])

#General function to do webservice calls via requests. Retries 3 times in case of failure before it fails for good
def apiCall(url, data):
    try:
        r = requests.get(url, data)
    except:
        time.sleep(10)
        logging.info("API exception, try again after 10 second delay")
        try:
            r = requests.get(url, data)
            logging.info("Success after 10 seconds")
        except:
            logging.info("API exception failed, again - try, now after addtional 120 seconds delay!")
            time.sleep(120)
            try:
                r = requests.get(url, data)
                logging.info("Success after 120 seconds")
            except:
                logging.info("API exception failed, again - try, now after addtional 120 seconds delay!")
                time.sleep(300)
                try:
                    r = requests.get(url, data)
                    logging.info("Success after 300 seconds")
                except:
                    logging.info("Last try failed as well, abort. Total of 4 tries failed, so I let the whole process fail")
                    logging.info(url)
                    logging.info(data)
                    logging.info(r.status_code)
                    logging.info(r.request.url)
                    raise
    return r

#Takes an input label and executes the oxo call
def oxoMatch(termLabel, targetOntology, url):
    data={"ids":termLabel, "mappingTarget":targetOntology, "distance":3} #Maybe include also 'querySource='parameters
    oxoUrl=url+"search"
    jsonReply=apiCall(oxoUrl, data)
    try:
        jsonReply=jsonReply.json()['_embedded']['searchResults'][0]
        tmpList=[]
        sourceCurie=jsonReply['curie']
        if len(jsonReply['mappingResponseList'])>0:
            for row in jsonReply['mappingResponseList']:

                ##Additional webservice call to get the stupid long IRI out of oxo
                #oxoMapURL="https://www.ebi.ac.uk/spot/oxo/api/mappings"
                data={"fromId":row['curie']}
                longId=apiCall(url+"mappings", data)
                longId=longId.json()['_embedded']['mappings'][0]['fromTerm']['uri']
                tmpList.append({"curie":longId, "distance":row['distance']})
                #tmpList.append({"curie":row['curie'], "distance":row['distance']})
                sortedCurie=sorted(tmpList, key=lambda tmpList: tmpList['distance'], reverse=False)
        else:
            sortedCurie=[{"curie":"UNKNOWN", "distance": 0}]
        return sortedCurie
    except Exception as e:
        #In case there is NO oxo result, we find outselfs in this loop
        #print "Problem with oxo:"
        #print e
        #print "Termlabel: "+termLabel
        #print "TargetOntolgy: "+targetOntology
        #print "Tried to reach "+oxoUrl+" with parameters "+str(data)
        return [{"curie":"UNKNOWN", "distance": 0}]



############################################################
##List of terms that should be cut out of label before fuzzy match # Shall come from config file later
#cutList=["abnormalityof", "syndrome", "disease", "cancer", "tumor", "abnormal"]

# def stringProcess(term):
#     processedTerm=term.lower()                      #Make sting lower case
#     processedTerm=processedTerm.replace(" ", "")    #Remove all spaces
#
#     #Simply cut some things from the label before calculating the levenstein distance
#     for cut in cutList:
#         tmpArray=term.split(cut)                    #Remove problematic terms
#         if len(tmpArray[0])!=0:
#             processedTerm=tmpArray[0]
#             break
#         elif len(tmpArray[1])!=0:
#             processedTerm=tmpArray[1]
#             break
#         else:
#             print "Something is wrong"
#             break
#
#     return processedTerm

############################################################
#removeStopwordsList=['of', 'the']
#replaceTermList=[('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('abnormality','disease')]

def sortWords(term):
    term=term.lower()
    StringList=term.split(' ')
    StringList.sort()
    term=' '.join(StringList)
    return term

def stringMatcher(sourceTerm, targetTerm, replaceTermList, removeStopwordsList):
    #First calculate Lev without changes
    lev=round(Levenshtein.ratio(sourceTerm, targetTerm), 5)
    #print "Straight Lev: "+sourceTerm+" - "+targetTerm+" --> "+str(lev)

    sourceTerm=sortWords(sourceTerm)
    targetTerm=sortWords(targetTerm)


    replacementLev=round(Levenshtein.ratio(sourceTerm, targetTerm), 5)
    #print "Sorted Lev: "+sourceTerm+" - "+targetTerm+" --> "+str(replacementLev)
    if replacementLev>lev:
        lev=replacementLev
        #print " Score Higher, so replaced"

    #Remove stop words
    for stop in removeStopwordsList:
        sourceTerm=sourceTerm.replace(stop,'').strip().replace('  ', ' ')
        targetTerm=targetTerm.replace(stop, '').strip().replace('  ', ' ')

    #print "Removed Stopwords Lev: "+sourceTerm+" - "+targetTerm+" --> "+str(round(Levenshtein.ratio(sourceTerm, targetTerm), 5))

    #print "SourceReplacements:"
    #Replace terms in source to trying to find higher score
    for replacement in replaceTermList:
        tmpSource=sourceTerm.replace(replacement[0], replacement[1])
        tmpSource=sortWords(tmpSource)
        replacementLev=round(Levenshtein.ratio(tmpSource, targetTerm), 5)
        #print "  Replacements: "+tmpSource+" - "+targetTerm+" --> "+str(replacementLev)
        if replacementLev>lev:
            lev=replacementLev
            #print " Score Higher, so replaced"

#        tmpSource=tmpSource.split(' ')
#        tmpSource.sort()
#        tmpSource=' '.join(tmpSource)
#        replacementLev=round(Levenshtein.ratio(tmpSource, targetTerm), 5)
#        print "  Replacements Source: "+tmpSource+" - "+targetTerm+" --> "+str(replacementLev)
#        if replacementLev>lev:
#            lev=replacementLev

    #print "TargetReplacements:"
    #Replace terms in target to trying to find higher score
    for replacement in replaceTermList:
        tmpTarget=targetTerm.replace(replacement[0], replacement[1])
        tmpTarget=sortWords(tmpTarget)
        replacementLev=round(Levenshtein.ratio(sourceTerm, tmpTarget), 5)
        #print "  Replacements: "+sourceTerm+" - "+tmpTarget+" --> "+str(replacementLev)
        if replacementLev>lev:
            lev=replacementLev
            #print " Score Higher, so replaced"

    return lev
############################################################




#Takes an input label and executes the fuzzyOLS call
def olsFuzzyMatch(termLabel, targetOntology, replaceTermList, removeStopwordsList, url):
    url=url+"search"
    data={"q":termLabel, "ontology":targetOntology, "type":"class", "local":True, "fieldList":"label,iri,synonym"}
    jsonReply=apiCall(url, data)
    termLabel=termLabel.encode(encoding='UTF-8')

    #WE found at least 1 hit
    try:
        jsonReply=jsonReply.json()['response']
    except:
        print "Error with deoding jsonReply from OLS api call!"
        logging.error("Error with deoding jsonReply from OLS api call!")
        print data
        print url
        logging.error(data)
        logging.error(jsonReply)
        print jsonReply

    if  jsonReply['numFound']>0:
        levList=[]
        for reply in jsonReply['docs']:
            try:
                #answerTerm=stringProcess(reply['label'].encode(encoding='UTF-8'))
                answerTerm=reply['label'].encode(encoding='UTF-8')

                #lev=round(Levenshtein.ratio(termLabel, answerTerm), 5)
                lev=stringMatcher(termLabel, answerTerm, replaceTermList, removeStopwordsList)

                #Compare the inputLabel with all synonym Labels as well.
                #If lev score is higher for a synonym, replace lev score --> boost synonym label hits
                if "synonym" in reply.keys():
                    for synonym in reply["synonym"]:
                        #answerTerm=stringProcess(synonym.encode(encoding='UTF-8'))
                        answerTerm=synonym.encode(encoding='UTF-8')
                        #tmpLev=round(Levenshtein.ratio(termLabel, answerTerm), 5)
                        tmpLev=stringMatcher(termLabel, answerTerm, replaceTermList, removeStopwordsList)
                        if tmpLev>lev:
                            lev=tmpLev

                levList.append({"SourceLabel": termLabel, "SourceIRI": termLabel , "TargetIRI": reply['iri'], "TargetLabel": reply['label'], "lev":lev})

            except Exception as e:
                print e
                print "ERROR WITH LEV Distance, score 0 for now for these two"
                print reply
                print termLabel
                levList.append({"SourceLabel": termLabel, "SourceIRI": termLabel , "TargetIRI": reply['iri'], "TargetLabel": reply['label']+"_ERROR", "lev":0})

        sortedLev=sorted(levList, key=lambda levList:levList['lev'], reverse=True)

    else:
        #print "No hits, therefore Add empty placeholder"
        sortedLev=[{"SourceLabel": termLabel, "SourceIRI": termLabel , "TargetIRI": "UNKNOWN", "TargetLabel": "UNKNOWN", "lev": 0}]


    ##Now let's relax The fuzzy search and aim for other (all) ontologies
    data={"q":termLabel, "type":"class", "local":True, "limit":5}
    jsonReply=apiCall(url, data)
    try:
        jsonReply=jsonReply.json()['response']
    except Exception as e:
        print "Error with decoding jsonReply from RELAXED OLS api call!"
        print jsonReply
        print e
        logging.error("Error with decoding jsonReply from RELAXED OLS api call!")
        logging.error(jsonReply)
        logging.error(data)
        logging.error(e)


    #jsonReply=jsonReply.json()['response']
    try:
        oxoTargetList=[]
        if  jsonReply['numFound']>0:
            for reply in jsonReply['docs']:
                if reply['ontology_name']!=targetOntology:
                    oxoTargetList.append({"short_form": reply['short_form'],"bridgeOntology":reply['ontology_name']})

        return {"fuzzyTerms": sortedLev, "bridgeTerms": oxoTargetList}
    except Exception as e:
        print "Error processing jsonsReply"
        print jsonReply
        print e
        logging.error(e)
        logging.error(jsonReply)
        return {"fuzzyTerms": sortedLev, "bridgeTerms": oxoTargetList}

#Executes the basic calls, delievers primary score (raw scoring)
def primaryScoreTerm(termIRI, termLabel, targetOntology, scoreParams, urls):
    replaceTermList=scoreParams["replaceTermList"]
    removeStopwordsList=scoreParams["removeStopwordsList"]

    olsFuzzyResult=olsFuzzyMatch(termLabel, targetOntology, replaceTermList, removeStopwordsList, urls["ols"])

    if termIRI!='':
        oxoResults=oxoMatch(termIRI, targetOntology, urls["oxo"])
    else:
        oxoResults=[{"curie":"UNKNOWN", "distance": 0}]

    bridgeTerms=olsFuzzyResult['bridgeTerms']
    olsFuzzyResult=olsFuzzyResult['fuzzyTerms']

    #if bridgeTerms!=[]:
        #print "Found bridge terms, it is incredible!"

    bridgeOxo=[]
    if len(bridgeTerms)>0:
        for bridgeTerm in bridgeTerms:
            tmp=oxoMatch(bridgeTerm['short_form'],targetOntology, urls["oxo"])
            for line in tmp:
                if line['curie']!='UNKNOWN':
                    bridgeOxo.append(tmp)
                else:
                    bridgeOxo=[[{"curie":"UNKNOWN", "distance": 0}]]
    else:
        bridgeOxo=[[{"curie":"UNKNOWN", "distance": 0}]]


    try:
        bridgeOxo=bridgeOxo[0]
    except e as Exception:
        print "Error with that stupid list in bridgeOxo"
        print termIRI
        print termLabel
        print bridgeOxo
        bridgeOxo=[[{"curie":"UNKNOWN", "distance": 0}]]

    scoreTerm={"sourceTerm": termLabel, "targetOntology":targetOntology,"olsFuzzyScore":olsFuzzyResult, "oxoScore":oxoResults, "bridgeEvidence":bridgeOxo}
    return scoreTerm

#The fuction takes a primary score (raw) and calculates a corresponding score for each subresult
def processPScore(pScore):
    mapping={'sourceTerm':pScore['sourceTerm'], "olsFuzzyScore": [], "oxoScore": [], "bridgeEvidence": []}

    for fuzzy in pScore['olsFuzzyScore']:
        mapping['olsFuzzyScore'].append({'fuzzyMapping': fuzzy['TargetLabel'], 'fuzzyIri':fuzzy['TargetIRI'],'fuzzyScore': fuzzy['lev']})

    for oxo in pScore['oxoScore']:
        tmpCurie=oxo['curie']
        oxoScore=int(oxo['distance'])

        if int(oxo['distance'])==0:
            tmpCurie="UNKNOWN"

        mapping['oxoScore'].append({'oxoCurie':tmpCurie, "distance": oxo['distance'] ,"oxoScore":oxoScore})

    for oxo in pScore['bridgeEvidence']:
        tmpCurie=oxo['curie']
        bridgeOxoScore=int(oxo['distance'])

        if int(oxo['distance'])==0:
            tmpCurie="UNKNOWN"

        mapping['bridgeEvidence'].append({'oxoCurie':tmpCurie, "distance": oxo['distance'], "oxoScore":bridgeOxoScore})

    return mapping

#Combines the subresults of the processPScore function. Results are combined line by line #So we combine the same results and add a new line for new entries
def simplifyProcessedPscore(mapping):
    scoreMatrix=[]
    sourceIRI=mapping['sourceIRI']

    flag=False
    for line in mapping['olsFuzzyScore']:
        if line['fuzzyScore']==[]:
            line['fuzzyScore']=0
        obj={"sourceTerm":mapping['sourceTerm'], "sourceIRI":sourceIRI,"iri":line['fuzzyIri'], "fuzzyScore": line['fuzzyScore'], "oxoScore": 0, "synFuzzy":0, "synOxo": 0, "bridgeOxoScore":0}
        scoreMatrix.append(obj)

    flag=False
    for line in mapping['oxoScore']:
         for s in scoreMatrix:
             if line["oxoCurie"]==s["iri"]:
                 s['oxoScore']=line['oxoScore']
                 flag=True

         if flag==False:
             obj={"sourceTerm":mapping['sourceTerm'], "sourceIRI":sourceIRI, "iri":line['oxoCurie'], "fuzzyScore": 0, "oxoScore": line['oxoScore'], "synFuzzy":0, "synOxo": 0, "bridgeOxoScore":0}
             scoreMatrix.append(obj)

    # Starting here we try to take care of synonyms!
    if 'synFuzzy' in mapping and 'synOxo' in mapping:
    # Fuzzy Synonyms Score
        flag=False
        for line in mapping['synFuzzy']:
            for s in scoreMatrix:
                if line["fuzzyIri"]==s["iri"]:
                    #s['fuzzyScore']=line['fuzzyScore']
                    s['synFuzzy']=line['fuzzyScore']
                    flag=True

            if flag==False:
                obj={"sourceTerm":mapping['sourceTerm'], "sourceIRI":sourceIRI,"iri":line['fuzzyIri'], "fuzzyScore":0, "oxoScore": 0, "synFuzzy": line['fuzzyScore'], "synOxo":0, "bridgeOxoScore":0}
                scoreMatrix.append(obj)

    # Oxo Synonyms Score
        flag=False
        for line in mapping['synOxo']:
            for s in scoreMatrix:
                if line["oxoCurie"]==s["iri"]:
                    #s['oxoScore']=line['oxoScore']
                    s['synOxo']=line['oxoScore']
                    flag=True

            if flag==False:
                obj={"sourceTerm":mapping['sourceTerm'], "sourceIRI":sourceIRI, "iri":line['oxoCurie'], "fuzzyScore": 0, "oxoScore": 0, "synFuzzy":0, "synOxo": line['oxoScore'], "bridgeOxoScore":0}
                scoreMatrix.append(obj)

    #else:
    #    print "No Synonyms here"



    #Getting into bridge evidence
    flag=False
    for line in mapping['bridgeEvidence']:
         for s in scoreMatrix:
             if line["oxoCurie"]==s["iri"]:
                 s['oxoScore']=line['oxoScore']
                 flag=True

         if flag==False:
             #obj={"sourceTerm":mapping['sourceTerm'], "sourceIRI":sourceIRI, "iri":line['bridgeOxoCurie'], "fuzzyScore": 0, "oxoScore": 0, "synFuzzy":0, "synOxo": 0, "bridgeOxoScore": line['oxoScore']}
             obj={"sourceTerm":mapping['sourceTerm'], "sourceIRI":sourceIRI, "iri":line['oxoCurie'], "fuzzyScore": 0, "oxoScore": 0, "synFuzzy":0, "synOxo": 0, "bridgeOxoScore": line['oxoScore']}
             #oxoCurie
             scoreMatrix.append(obj)


    #print "made it to the end of simplifyProcessedPscore"1
    return scoreMatrix

#Simple Score mechanism for all subscores, returns a sorted list. Is Called after simplifyProcessedPscore
def scoreSimple(scoreMatrix, params):
    threshold=params['threshold']

    oxoDistanceOne=params['oxoDistanceOne']
    oxoDistanceTwo=params['oxoDistanceTwo']
    oxoDistanceThree=params['oxoDistanceThree']

    fuzzyUpperLimit=params['fuzzyUpperLimit']
    fuzzyLowerLimit=params['fuzzyLowerLimit']
    fuzzyUpperFactor=params['fuzzyUpperFactor']
    fuzzyLowerFactor=params['fuzzyLowerFactor']

    synFuzzyFactor=params['synFuzzyFactor']
    synOxoFactor=params['synOxoFactor']

    synFuzzyFactor=params['synFuzzyFactor']
    synOxoFactor=params['synOxoFactor']
    bridgeOxoFactor=params['bridgeOxoFactor']

    resultMatrix=[]
    for i,score in enumerate(scoreMatrix):
        fFactor=0
        if score['fuzzyScore']==1:  #Exact match, we shall boost this by all means, so we take UpperFactor*2 for now
            fFactor=2
        elif score['fuzzyScore']>=fuzzyUpperLimit:
            fFactor=fuzzyUpperFactor
        elif score['fuzzyScore']<fuzzyUpperLimit and score['fuzzyScore']>=fuzzyLowerLimit:
            fFactor=fuzzyLowerFactor
        elif score['fuzzyScore']<fuzzyLowerLimit:
            fFactor=0


        if score['oxoScore']==1:
            score['oxoScore']=oxoDistanceOne
        if score['oxoScore']==2:
            score['oxoScore']=oxoDistanceTwo
        if score['oxoScore']==3:
            score['oxoScore']=oxoDistanceThree


        if score['bridgeOxoScore']>0:
            print "FOUND an incredible bridge Term, uhauha!"
            print scoreMatrix[i]


        score['finaleScore']=score['fuzzyScore']*fFactor+score['oxoScore']+score['synFuzzy']*synFuzzyFactor+score['synOxo']*synOxoFactor+score['bridgeOxoScore']*bridgeOxoFactor

        ### Do we want unknown to be printed
        if score['finaleScore']>threshold:          #This removes "unknow" from the results and weak results
            resultMatrix.append(scoreMatrix[i])
#        else:
            #print "Failed to pass the threshold unfortunatley!"
            #print score['finaleScore']

    #Sort the thing so the best score is top
    resultMatrix=sorted(resultMatrix, key=lambda resultMatrix:resultMatrix['finaleScore'], reverse=True)
    return resultMatrix

#Simple Score mechanism for all subscores, returns a sorted list. Is Called after simplifyProcessedPscore
#def scoreComplex(scoreMatrix):

#Calls all necessary steps to get a result for a termLabel
def scoreTermLabel(termLabel, targetOntology, scoreParams, params):
    pscore=primaryScoreTerm('', termLabel, targetOntology, scoreParams, params)  #Executes the basic calls to OLS and OXO, delievers primary score
    pscore['sourceIRI']="UNKNOWN"
    calculatedMappings=processPScore(pscore)    #Process the primaryScore, weighting the primary results
    calculatedMappings['sourceIRI']="UNKNOWN"
    simplerMatrix=simplifyProcessedPscore(calculatedMappings) #Takes the processed input and combines the results line by line
    singleLineResult=scoreSimple(simplerMatrix, params) #Takes simplified input and actually calculates the finale score
    return singleLineResult


# Synonymsearch for comparing Ontologies in OLS, should be called instead score Simple for these cases
def scoreTermOLS(termIRI, termLabel, targetOntology, params, urls):
    pscore=primaryScoreTerm(termIRI, termLabel, targetOntology, params, urls)
    pscore['sourceIri']=termIRI
    return pscore
