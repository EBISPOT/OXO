import flaskMapping
import logging
import validation
import csv
import time
import requests
import json
from ConfigParser import SafeConfigParser
import ast

url="https://www.ebi.ac.uk/ols/api/"


#Compares to ontologies from the OLS. This process can take a while and procudes a csv with primary results
def scoreOntologies(sourceOntology, targetOntology, scoreParams):
    logging.info("Start scoring "+sourceOntology+" and "+targetOntology)
    #Check for the smaller ontology
    r = requests.get(url+"ontologies/"+sourceOntology)
    numberOfTerms=r.json()['numberOfTerms']
    r = requests.get(url+"ontologies/"+targetOntology)
    numberOfTerms2 = r.json()['numberOfTerms']

    #In case the targetOntology is smaller than the source Ontology, switch the output
    if (numberOfTerms>numberOfTerms2):
        tmpOntology=sourceOntology
        sourceOntology=targetOntology
        targetOntology=tmpOntology

    termsUrl=url+"ontologies/"+sourceOntology+"/terms?size=100&fieldList=iri,label,synonym"
    results=[]

    results.append(["sourceLabel","sourceIRI", "fuzzy", "oxo", "synFuzzy", "synOxo", "bridgeTerms"])
    counter=0
    while True:
        try:
            r = requests.get(termsUrl)
        except Exception as e:
            print "Error with webservice call"
            print e
            logging.info("Error with webservice call")
            logging.info(r.url)
            logging.info(r.status_code)
            raise e

        for term in r.json()['_embedded']['terms']:
            originalLabel=term["label"]
            synonyms=term["synonyms"]

            #Check if the term is actually defined in that ontology
            if term['is_defining_ontology'] is True:
                    pscore=flaskMapping.scoreTermOLS(term["iri"], originalLabel, targetOntology, scoreParams)
                    try:

                        calculatedMappings=flaskMapping.processPScore(pscore)
                    except Exception as e:
                        print "Exception in primary Scoring"
                        print e
                        print term["iri"]
                        print originalLabel
                        print targetOntology
                        logging.info("Exception in primary Scoring")
                        logging.info(term["iri"]+" "+originalLabel)
                        calculatedMappings={'sourceTerm':term["iri"]+"ERROR", "olsFuzzyScore": [], "oxoScore": [], "bridgeEvidence": []}

                    #If synonyms are available, run through the same steps with synonyms to score an ontology
                    synCalculatedMappings={}
                    if synonyms!=None:
                        for synonym in synonyms:
                            try:
                                synPscore=flaskMapping.primaryScoreTerm('', synonym, targetOntology, scoreParams)
                                synCalculatedMappings=flaskMapping.processPScore(synPscore)         #Process the primaryScore for synonyms
                                synCalculatedMappings['sourceIRI']=term["iri"]
                            except Exception as e:
                                print "Exception in Synonym processPScore Term"
                                print e
                                synCalculatedMappings={'sourceTerm':term["iri"]+"ERROR", "olsFuzzyScore": [], "oxoScore": [], "bridgeEvidence": []}
                                logging.info("Exception in  Synonym processPScore Term")
                                logging.info(term["iri"]+" "+synonym+" "+targetOntology)
                                synCalculatedMappings['olsFuzzyScore']=[{'fuzzyScore': 0, 'fuzzyMapping': 'UNKNOWN - ERROR', 'fuzzyIri': 'UNKNOWN - ERROR'}]
                                synCalculatedMappings['oxoScore']=[{'distance': 0, 'oxoCurie': 'UNKNOWN', 'oxoScore': 0}]
                                synCalculatedMappings['sourceIRI']=term["iri"]

                    else:
                        synCalculatedMappings['olsFuzzyScore']=[{'fuzzyScore': 0, 'fuzzyMapping': 'UNKNOWN', 'fuzzyIri': 'UNKNOWN'}]
                        synCalculatedMappings['oxoScore']=[{'distance': 0, 'oxoCurie': 'UNKNOWN', 'oxoScore': 0}]

                    results.append([originalLabel.encode(encoding='UTF-8'), term["iri"].encode(encoding='UTF-8'), calculatedMappings['olsFuzzyScore'], calculatedMappings['oxoScore'], synCalculatedMappings['olsFuzzyScore'], synCalculatedMappings['oxoScore'], calculatedMappings['bridgeEvidence']])


        try:
            termsUrl=r.json()['_links']['next']['href']
            ###This is just temporary to not process all the stuff but abort after two pages
            counter=counter+1
            if counter%2==0:
                print "Processed "+str(counter)+" pages"
                logging.info("Processed "+str(counter)+" pages")
                break  # Not necessary if we want to parse whole ontology, just activate this for testing
        except:
            logging.info("Reached last page I recon")
            print "Reached last page I recon"
            break

    with open('pipeline_output/scoring_output_'+sourceOntology+'_'+targetOntology+'.csv', 'w') as f:
        writer = csv.writer(f)
        writer.writerows(results)
        f.close()

#Read in and process the ontology primary score from a csv file
def scoreOntologyPrimaryScore(name):
    with open("pipeline_output/"+name+"/scoring_output_"+name+".csv") as csvfile:
        readCSV = csv.reader(csvfile, delimiter=',')

        scoreMatrix=[]
        next(readCSV)   #Skip csv header
        for row in readCSV:
            originalLabel=row[0]
            orginaliri=row[1]
            tmp=row[2]
            fuzzy=ast.literal_eval(tmp)
            tmp=row[3]
            oxo=ast.literal_eval(tmp)
            tmp=row[4]
            synFuzzy=ast.literal_eval(tmp)
            tmp=row[5]
            synOxo=ast.literal_eval(tmp)
            tmp=row[6]
            bridgeEvidence=ast.literal_eval(tmp)

            for i in fuzzy:
                obj={"sourceTerm":originalLabel, "sourceIRI":orginaliri ,"iri": i['fuzzyIri'], "olsFuzzyScore": fuzzy, "oxoScore": oxo, "synFuzzy": synFuzzy, "synOxo":synOxo, "bridgeEvidence":bridgeEvidence}
                scoreMatrix.append(obj)

            for i in oxo:
                obj={"sourceTerm":originalLabel, "sourceIRI":orginaliri, "iri": i['oxoCurie'], "olsFuzzyScore": fuzzy, "oxoScore": oxo, "synFuzzy": synFuzzy, "synOxo":synOxo, "bridgeEvidence":bridgeEvidence}
                scoreMatrix.append(obj)

            for i in synFuzzy:
                obj={"sourceTerm":originalLabel, "sourceIRI":orginaliri, "iri": i['fuzzyIri'], "olsFuzzyScore": fuzzy, "oxoScore": oxo, "synFuzzy": synFuzzy, "synOxo":synOxo, "bridgeEvidence":bridgeEvidence}
                scoreMatrix.append(obj)

            for i in synOxo:
                obj={"sourceTerm":originalLabel, "sourceIRI":orginaliri, "iri": i['oxoCurie'], "olsFuzzyScore": fuzzy, "oxoScore": oxo, "synFuzzy": synFuzzy, "synOxo":synOxo, "bridgeEvidence":bridgeEvidence}
                scoreMatrix.append(obj)

            for i in bridgeEvidence:
                obj={"sourceTerm":originalLabel, "sourceIRI":orginaliri, "iri": i['oxoCurie'], "olsFuzzyScore": fuzzy, "oxoScore": oxo, "synFuzzy": synFuzzy, "synOxo":synOxo, "bridgeEvidence":bridgeEvidence}
                scoreMatrix.append(obj)


        simplerMatrix=[]
        #Calls simplifyProcessedPscore for ever line in scoreMatrix that we just read in
        for line in scoreMatrix:
            pScore=flaskMapping.simplifyProcessedPscore(line)
            if pScore not in simplerMatrix:
                simplerMatrix.append(pScore)

        return simplerMatrix

#Takes simplified input and actually calculates the finale score
def processOntologyPrimaryScore(pScore, params):
    result=[]
    for line in pScore:
        singleLineResult=flaskMapping.scoreSimple(line, params)
        result.append(singleLineResult)

    #Take the highest scored mappings
    tmp=[]
    for entry in result:
        if entry!=[]:
            tmp.append(entry)
        #else:
        #    print "entry in results!"

    #SortScore
    tmp=sorted(tmp, key=lambda tmp:tmp[0]['finaleScore'], reverse=True)
    return tmp

#Maybe transfer to server
def scoreTermList(termList, targetOntology, params):
    result=[]
    for term in termList:
        result.append(flaskMapping.scoreTermLabel(term, targetOntology, params))
    return result

# Process an IRI list via OLS instead of a termList
# def scoreIriList(IriList, targetOntology, params):

#Process scoredMatrix to prepare for validation or save to disc
def writeOutPutScore(scoredMatrix, name, saveToDisc):
    result=[]
    for line in scoredMatrix:
        result.append([line[0]['sourceIRI'], line[0]['iri'], line[0]['finaleScore'], line[0]['sourceTerm']])

    if saveToDisc==True:
        with open('pipeline_output/calculated_output_'+name+'.csv', 'w') as f:
            writer = csv.writer(f)
            writer.writerows(result)
            f.close()

    return result

#Not implemented yet #Remove double entry stuff #Potentially
def curationOntologyFinalScore(scoredMatrix):
    endmap=[]
    unified=[]
    #print scoredMatrix
    doubleEntryCounter=0
    replacedList=[]
    for counter, line in enumerate(scoredMatrix):
        #print line
        if line[1] not in endmap:
            endmap.append(line[1])
            unified.append(line)
        else:
            #print "Double entry Found!!!"
            doubleEntryCounter=doubleEntryCounter+1
            index=endmap.index(line[1])
            #print unified[index]
            #print scoredMatrix[counter]
            if unified[index][2]<scoredMatrix[counter][2]:
                print "Found higher score, so will replace now! "
                replacedList.append(unified[index])
                unified[index]=scoredMatrix[counter] #Replace that line with the higher score!


    print "A total of "+str(doubleEntryCounter)+" processed!"
    if len(replacedList)!=0:
        print "Write file of replaced terms now"
        print "Total length of replaced is "+str(len(replacedList))
        with open('pipeline_output/replaced_terms.csv', 'wb') as f:
            writer = csv.writer(f)
            writer.writerows(replacedList)
            f.close()

    return unified



def calculatePrimaryScore(combinedOntologyName, params, writeToDisc, curationOfDoubleEntries):
    simplerMatrix=scoreOntologyPrimaryScore(combinedOntologyName)
    scoredMatrix=processOntologyPrimaryScore(simplerMatrix, params)
    preparedScoredMatrix=writeOutPutScore(scoredMatrix, combinedOntologyName, writeToDisc)

    #Make mappings unique if the Flag is set to true, if not - we get way more mappings, miss less from the standard but are not unique
    if curationOfDoubleEntries==True:
        preparedScoredMatrix=curationOntologyFinalScore(preparedScoredMatrix)

    return preparedScoredMatrix

#Calculates a score and Validates it against a standard for a pair of ontologies
def calculateAndValidateOntologyPrimaryScore(onto1, onto2, stdName, stdFile, params, writeToDisc, parseParms, curationOfDoubleEntries):
    combinedOntologyName=onto1+"_"+onto2
    preparedScoredMatrix=calculatePrimaryScore(combinedOntologyName, params, writeToDisc, curationOfDoubleEntries)
    validationResult=validation.validateFinaleScore(onto1, onto2, stdName, preparedScoredMatrix, stdFile, writeToDisc, params, parseParms)
    return validationResult

#Goes through the sections and calls scoreOntologies for every section
def scoreListOntologies(sections):
    for section in sections:
        sourceOntology=config.get(section, 'sourceOntology')
        targetOntology=config.get(section, 'targetOntology')
        print "Score "+sourceOntology+" "+targetOntology
        scoreOntologies(sourceOntology, targetOntology, scoreParams)

#Goes through the sections and calls calculateAndValidateOntologyPrimaryScore for every section
def calculateAndValidateListOntologies(sections):
    for section in sections:
        sourceOntology=config.get(section, 'sourceOntology')
        targetOntology=config.get(section, 'targetOntology')
        stdFile=config.get(section, 'loom')
        print "Score "+sourceOntology+" "+targetOntology
        #params={"exactFactor":1, "fuzzyFactor": 1, "oxoFactor": 1, "synExactFactor": 1, "synFuzzyFactor": 1, "synOxoFactor": 1, "threshold":0.3}
        calculateAndValidateOntologyPrimaryScore(sourceOntology+"_"+targetOntology, stdFile, params,True)


config = SafeConfigParser()
config.read("config.ini")
sections=config.sections()[2:]
#Could/Should be changed so parameters come from the config file
params={"fuzzyUpperLimit": 0.8, "fuzzyLowerLimit": 0.6,"fuzzyUpperFactor": 1,"fuzzyLowerFactor":0.6, "oxoDistanceOne":1, "oxoDistanceTwo":0.3, "oxoDistanceThree":0.1, "synFuzzyFactor":0.6, "synOxoFactor": 0.4, "threshold":0.6}

#ordo_hp {'misses': 532, 'alternatives': 60}
#params={"fuzzyUpperLimit": 0.66, "fuzzyLowerLimit": 0.89,"fuzzyUpperFactor": 0.2,"fuzzyLowerFactor":0.0149, "oxoDistanceOne":0.45, "oxoDistanceTwo":0.5, "oxoDistanceThree":0.414, "synFuzzyFactor":0.357, "synOxoFactor": 0.38, "threshold":0.6}

# For 'ordo','hp',silver --> -175?
#Best run at 39 of 50 (best like=-175) with parameter set:
#[  5.17002538e-05   9.97186358e-01   5.14350766e-03   5.08109418e-01
#   9.98062574e-01   9.11663359e-02   4.59066160e-01   6.72066822e-01
#   4.07275455e-01]

#params={"fuzzyUpperLimit": 0.00005, "fuzzyLowerLimit": 0.99,"fuzzyUpperFactor": 0.5,"fuzzyLowerFactor":0.99, "oxoDistanceOne":0.09, "oxoDistanceTwo":0.459, "oxoDistanceThree":0.67, "synFuzzyFactor":0.41, "synOxoFactor": 0.38, "threshold":0.6}


#params={"fuzzyUpperLimit": 0.8, "fuzzyLowerLimit": 0.6,"fuzzyUpperFactor": 1,"fuzzyLowerFactor":0.6, "oxoDistanceOne":1, "oxoDistanceTwo":0.3, "oxoDistanceThree":0.1, "synFuzzyFactor":1, "synOxoFactor": 0.4, "threshold":0.6}


###Score all Ontologies in the config file
#scoreListOntologies(sections)
#config.get()
#oxoURL=config.get("replacements","oxoURL")
#print list(config.items('replacements'))

removeStopwordsList=['of', 'the']
replaceTermList=[('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('abnormality','disease')]
scoreParams={"removeStopwordsList": removeStopwordsList, "replaceTermList" :replaceTermList}

hp_doid_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : [('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('abnormality','disease')]}


### Primary score ontologies
ordo_hp_scoreParams={"removeStopwordsList": ['of', 'the', 'Rare'], "replaceTermList" : [('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('tumor', 'neoplasm'), ('tumor','cancer'), ('abnormality', 'disease'), ('decreased', 'reduced'), ('morphology', '')]}
scoreOntologies("ordo","hp", ordo_hp_scoreParams)

doid_ordo_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : []}
scoreOntologies("doid","mp", doid_ordo_scoreParams)
#
doid_ordo_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : []}
scoreOntologies("doid","ordo", doid_ordo_scoreParams)
#
hp_doid_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : [('cancer', 'neoplasm'), ('cancer','carcinoma'), ('abnormality','disease'), 'abnormality','disease']}
scoreOntologies("hp","doid",hp_doid_scoreParams)
#
hp_mp_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : [('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('abnormality','disease'), ('abnormal','Abnormality')]}
scoreOntologies("hp","mp", hp_mp_scoreParams)
#
ordo_mp_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : []}
scoreOntologies("ordo","mp", ordo_mp_scoreParams)



#scoreOntologies("mesh","hp")
#scoreOntologies("mesh","doid")
#scoreOntologies("mesh","ordo")
#scoreOntologies("mesh","mp")


writeToDisc=False
uniqueMaps=False
### Execute Calculate and validate for a certain file
# print calculateAndValidateOntologyPrimaryScore('hp', 'doid', 'loom', 'Loom/DOID_HP_loom.csv', params, writeToDisc, {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('hp','doid', 'silver','silver_nov/Consensus-3-hp-doid.tsv', params, writeToDisc, {'uri1':0, 'uri2':2, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('ordo', 'hp', 'loom', 'Loom/ordo_hp_loom.csv', params, writeToDisc, {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('ordo', 'hp', 'silver','silver_nov/Consensus-3-hp-ordo.tsv', params, writeToDisc, {'uri1':2, 'uri2':0, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps)
# #
# print calculateAndValidateOntologyPrimaryScore('mp','hp', 'loom','Loom/MP_HP_loom.csv', params, writeToDisc, {'uri1':0, 'uri2':1, 'scorePosition':2 , 'delimiter':','}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('mp','hp', 'silver','silver_nov/Consensus-3-hp-mp.tsv', params, writeToDisc, {'uri1':2, 'uri2':0, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('ordo','doid', 'loom' ,'Loom/DOID_ORDO_loom.csv', params, writeToDisc, {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('ordo','doid', 'silver','silver_nov/Consensus-3-doid-ordo.tsv', params, writeToDisc, {'uri1':2, 'uri2':0, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('ordo','mp', 'loom', 'Loom/mp_ordo_loom.csv', params, writeToDisc, {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('ordo','mp', 'silver','silver_nov/Consensus-3-mp-ordo.tsv', params, writeToDisc, {'uri1':2, 'uri2':0, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('mp','doid', 'loom', 'Loom/DOID_MP_loom.csv', params, writeToDisc, {'uri1':1, 'uri2':0, 'scorePosition':2, 'delimiter':','}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('mp','doid', 'silver','silver_nov/Consensus-3-mp-doid.tsv', params, writeToDisc, {'uri1':0, 'uri2':2, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps)
# #
#
# print calculateAndValidateOntologyPrimaryScore('mesh','doid', 'loom', 'Loom/DOID_MESH_loom_new.csv', params, writeToDisc, {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('mesh','doid', 'silver', 'silver_nov/Consensus-3-doid-mesh3.tsv', params, writeToDisc, {'uri1':2, 'uri2':0, 'scorePosition':2, 'delimiter':'\t'}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('mesh','hp', 'loom', 'Loom/mesh_hp_loom_new.csv', params, writeToDisc, {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('mesh','doid', 'silver', 'silver_nov/Consensus-3-doid-mesh3.tsv', params, writeToDisc, {'uri1':1, 'uri2':0, 'scorePosition':2, 'delimiter':'\t'}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('mesh','mp', 'loom', 'Loom/mesh_mp_loom_new.csv', params, writeToDisc, {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps)
# print calculateAndValidateOntologyPrimaryScore('mesh','doid', 'silver', 'silver_nov/Consensus-3-doid-mesh3.tsv', params, writeToDisc, {'uri1':1, 'uri2':0, 'scorePosition':2, 'delimiter':'\t'}, uniqueMaps)

###Execute functions for terms (#Broken since last change?)
# --> needs to be invesitgate!
#print flaskMapping.scoreTermLabel("Nuclear cataract", "doid", params)

###Calculate the score from the primary file and run it against the validation files. For all Ontologies in the config file
#calculateAndValidateListOntologies(sections)
