import paxo_internals
import logging
import validation
import csv
import time
import requests
import json
import os
from ConfigParser import SafeConfigParser
import ast
import neoExporter
import sys
import listprocessing

#Compares to ontologies from the OLS. This process can take a while and procudes a csv with primary results
def scoreOntologies(sourceOntology, targetOntology, scoreParams, scoringtargetFolder, mapSmallest, useLocalOnly):
    logging.info("Start scoring "+sourceOntology+" and "+targetOntology)
    #Check for the smaller ontology
    olsURL=config.get("Basics","olsAPIURL")
    oxoURL=config.get("Basics","oxoURL")
    urls={"ols":olsURL, "oxo":oxoURL}

    try:
        r = requests.get(olsURL+"ontologies/"+sourceOntology)
        numberOfTerms=r.json()['numberOfTerms']
        #Logging some meta data
        logging.info("MetaData for "+sourceOntology+": ")
        logging.info(" OLS update date: "+str(r.json()["updated"]))
        logging.info(" OLS version field: "+str(r.json()["config"]["version"]))
        logging.info(" OLS versionIRI field: "+str(r.json()["config"]["versionIri"]))

        r = requests.get(olsURL+"ontologies/"+targetOntology)
        numberOfTerms2 = r.json()['numberOfTerms']
        #Logging some meta data
        logging.info("MetaData for "+targetOntology+": ")
        logging.info(" OLS update date: "+str(r.json()["updated"]))
        logging.info(" OLS version field: "+str(r.json()["config"]["version"]))
        logging.info(" OLS versionIRI field: "+str(r.json()["config"]["versionIri"]))

    except:
        logging.error("Error getting number of terms throw webservice call!")
        logging.error(olsURL+"ontologies/"+sourceOntology)
        logging.error(olsURL+"ontologies/"+targetOntology)
        logging.error(r)
        raise


    if mapSmallest==True:
        #In case the targetOntology is smaller than the source Ontology, switch the output
        if (numberOfTerms>numberOfTerms2):
            tmpOntology=sourceOntology
            sourceOntology=targetOntology
            targetOntology=tmpOntology

    termsUrl=olsURL+"ontologies/"+sourceOntology+"/terms?size=500&fieldList=iri,label,synonym"
    results=[]

    results.append(["sourceLabel","sourceIRI", "fuzzy", "oxo", "synFuzzy", "bridgeTerms"])
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

            #Check if the term is actually defined in that ontology. Via flag it can be changed to process all terms
            if term['is_defining_ontology'] is True or term['is_defining_ontology'] is useLocalOnly:
                pscore=paxo_internals.scoreTermOLS(term["iri"], originalLabel, targetOntology, scoreParams, urls)
                try:
                    calculatedMappings=paxo_internals.processPScore(pscore)
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
                            synPscore=paxo_internals.primaryScoreTerm('', synonym, targetOntology, scoreParams, urls)
                            synCalculatedMappings=paxo_internals.processPScore(synPscore)         #Process the primaryScore for synonyms
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

                results.append([originalLabel.encode(encoding='UTF-8'), term["iri"].encode(encoding='UTF-8'), calculatedMappings['olsFuzzyScore'], calculatedMappings['oxoScore'], synCalculatedMappings['olsFuzzyScore'], calculatedMappings['bridgeEvidence']])
        try:
            termsUrl=r.json()['_links']['next']['href']
            counter=counter+1
            if counter%2==0:
                print "Processed "+str(counter)+" pages"
                logging.info("Processed "+str(counter)+" pages")
                #break  #Uncomment this for testing the -s flag (so not the whole ontology is parsed but 2 pages)
        except:
            logging.info("Reached last page I recon")
            print "Reached last page I recon"
            break


    with open(scoringtargetFolder+'scoring_output_'+sourceOntology+'_'+targetOntology+'.csv', 'w') as f:
        writer = csv.writer(f)
        writer.writerows(results)
        f.close()

#Read in and process the ontology primary score from a csv file
def scoreOntologyPrimaryScore(name, scorefolder):

    with open(scorefolder+"scoring_output_"+name+".csv") as csvfile:
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
            bridgeEvidence=ast.literal_eval(tmp)

            for i in fuzzy:
                #obj={"sourceTerm":originalLabel, "sourceIRI":orginaliri ,"iri": i['fuzzyIri'], "olsFuzzyScore": fuzzy, "oxoScore": oxo, "synFuzzy": synFuzzy, "synOxo":synOxo, "bridgeEvidence":bridgeEvidence}
                obj={"sourceTerm":originalLabel, "sourceIRI":orginaliri ,"iri": i['fuzzyIri'], "olsFuzzyScore": fuzzy, "oxoScore": oxo, "synFuzzy": synFuzzy, "bridgeEvidence":bridgeEvidence}
                scoreMatrix.append(obj)

            for i in oxo:
                obj={"sourceTerm":originalLabel, "sourceIRI":orginaliri, "iri": i['oxoCurie'], "olsFuzzyScore": fuzzy, "oxoScore": oxo, "synFuzzy": synFuzzy,  "bridgeEvidence":bridgeEvidence}
                scoreMatrix.append(obj)

            for i in synFuzzy:
                obj={"sourceTerm":originalLabel, "sourceIRI":orginaliri, "iri": i['fuzzyIri'], "olsFuzzyScore": fuzzy, "oxoScore": oxo, "synFuzzy": synFuzzy, "bridgeEvidence":bridgeEvidence}
                scoreMatrix.append(obj)

            for i in bridgeEvidence:
                obj={"sourceTerm":originalLabel, "sourceIRI":orginaliri, "iri": i['oxoCurie'], "olsFuzzyScore": fuzzy, "oxoScore": oxo, "synFuzzy": synFuzzy, "bridgeEvidence":bridgeEvidence}
                scoreMatrix.append(obj)


        simplerMatrix=[]
        #Calls simplifyProcessedPscore for ever line in scoreMatrix that we just read in
        for line in scoreMatrix:
            pScore=paxo_internals.simplifyProcessedPscore(line)
            if pScore not in simplerMatrix:
                simplerMatrix.append(pScore)

        return simplerMatrix

#Takes simplified input and actually calculates the finale score
def processOntologyPrimaryScore(pScore, params):
    result=[]
    for line in pScore:
        singleLineResult=paxo_internals.scoreSimple(line, params)
        result.append(singleLineResult)

    #Take the highest scored mappings
    tmp=[]
    for entry in result:
        if entry!=[]:
            tmp.append(entry)

    #SortScore
    tmp=sorted(tmp, key=lambda tmp:tmp[0]['finaleScore'], reverse=True)
    return tmp

#Maybe transfer to server
def scoreTermList(termList, targetOntology, scoreParams, params):
    result=[]
    for term in termList:
        result.append(paxo_internals.scoreTermLabel(term, targetOntology, scoreParams, params))
    return result

#Process scoredMatrix to prepare for validation or save to disc
def writeOutPutScore(scoredMatrix, name, predictedTargetFolder, saveToDisc):
    result=[]
    for line in scoredMatrix:
        sourceTerm=line[0]['sourceTerm']
        targetLabel=str(line[0]['label'].encode('ascii','ignore'))
        result.append([line[0]['sourceIRI'], line[0]['iri'], float(line[0]['finaleScore']), sourceTerm, targetLabel, float(line[0]['normalizedScore'])])

    if saveToDisc==True:
        result.insert(0,['sourceIRI','mappedIRI','score','sourceLabel', 'mappedLabel', 'NormalizedScore'])
        with open(predictedTargetFolder+'calculated_output_'+name+'.csv', 'w') as f:
            writer = csv.writer(f)
            writer.writerows(result)
            f.close()
        result.pop(0)

    return result

#Not implemented yet #Remove double entry stuff #Potentially
def curationOntologyFinalScore(scoredMatrix):
    endmap=[]
    unified=[]
    doubleEntryCounter=0
    replacedList=[]
    for counter, line in enumerate(scoredMatrix):
        if line[1] not in endmap:
            endmap.append(line[1])
            unified.append(line)
        else:
            doubleEntryCounter=doubleEntryCounter+1
            index=endmap.index(line[1])

            #Found higher score, so replace the lower!
            if unified[index][2]<scoredMatrix[counter][2]:
                unified[index]=scoredMatrix[counter] #Replace that line with the higher scored line!

    #print "A total of "+str(doubleEntryCounter)+" processed!"
    #if len(replacedList)!=0:
        #print "Write file of replaced terms now"
        #print "Total length of replaced is "+str(len(replacedList))
    return unified



def calculatePrimaryScore(combinedOntologyName, params, scoringTargetFolder, writeToDisc, predictedTargetFolder, curationOfDoubleEntries):
    simplerMatrix=scoreOntologyPrimaryScore(combinedOntologyName, scoringTargetFolder)
    scoredMatrix=processOntologyPrimaryScore(simplerMatrix, params)

    #Maximum would be caluclated like that, anyway, who knows if we really want that
    #maximum=(4*params["fuzzyUpperFactor"]+params["oxoDistanceOne"]+params["oxoDistanceOne"]*params["bridgeOxoFactor"])/4.0
    for row in scoredMatrix:
        row[0]['normalizedScore']=row[0]['finaleScore']/4.0

    preparedScoredMatrix=writeOutPutScore(scoredMatrix, combinedOntologyName, predictedTargetFolder, writeToDisc)

    #Make mappings unique if the Flag is set to true, if not - we get way more mappings, miss less from the standard but are not unique
    if curationOfDoubleEntries==True:
        preparedScoredMatrix=curationOntologyFinalScore(preparedScoredMatrix)

    return preparedScoredMatrix

#Calculates a score and Validates it against a standard for a pair of ontologies
def calculateAndValidateOntologyPrimaryScore(onto1, onto2, stdName, stdFile, params, scoringTargetFolder, writeToDisc,predictedTargetFolder, parseParms, curationOfDoubleEntries, validationTargetFolder, url):
    combinedOntologyName=onto1+"_"+onto2
    preparedScoredMatrix=calculatePrimaryScore(combinedOntologyName, params, scoringTargetFolder, writeToDisc, predictedTargetFolder,curationOfDoubleEntries)
    validationResult=validation.validateFinaleScore(onto1, onto2, stdName, preparedScoredMatrix, stdFile, writeToDisc, params, parseParms, validationTargetFolder, url)
    return validationResult

#Goes through the sections and calls scoreOntologies for every section
def scoreListOntologies(sections):
    scoringtargetFolder=config.get('Params', 'scoringTargetFolder')
    if os.path.exists(scoringtargetFolder)==False:
        print "Could not find "+scoringtargetFolder+" - please make sure the folder exists!\n"
        raise Exception("Folder does not exists")

    for section in sections:
        sourceOntology=config.get(section, 'sourceOntology')
        targetOntology=config.get(section, 'targetOntology')
        stopwordList=config.get("Params","StopwordsList").split(',')

        mapSmallest=config.getboolean("Params", 'mapSmallest')
        useLocalOnly=config.getboolean("Params", 'useLocalOnly')

        scoreParams={"removeStopwordsList":stopwordList, "replaceTermList" : []}
        print "Score "+sourceOntology+" "+targetOntology
        logging.info("Score "+sourceOntology+" "+targetOntology)
        scoreOntologies(sourceOntology, targetOntology, scoreParams, scoringtargetFolder, mapSmallest, useLocalOnly)

#Goes through the sections and calls calculateAndValidateOntologyPrimaryScore for every section
def calculateAndValidateListOntologies(sections, writeToDiscFlag, curationOfDoubleEntries):
    validationTargetFolder=config.get('Params', 'validationTargetFolder')
    scoringtargetFolder=config.get('Params', 'scoringTargetFolder')
    predictedTargetFolder=config.get('Params', 'predictedTargetFolder')

    if os.path.exists(scoringtargetFolder)==False:
        print "Could not find "+scoringtargetFolder+" - please make sure the folder exists!\n"
        raise Exception("Folder does not exists")
    if os.path.exists(predictedTargetFolder)==False:
        print "Could not find "+predictedTargetFolder+" - please make sure the folder exists!\n"
        raise Exception("Folder does not exists")
    if os.path.exists(validationTargetFolder)==False:
        print "Could not find "+validationTargetFolder+" - please make sure the folder exists!\n"
        raise Exception("Folder does not exists")

    returnValue=[]
    for section in sections:
        sourceOntology=config.get(section, 'sourceOntology')
        targetOntology=config.get(section, 'targetOntology')
        stdNames=config.get(section, 'standard').split(',')

        for name in stdNames:
            stdFile=config.get(section, name)
            uri1=int(config.get(section, 'uri1'+name))
            uri2=int(config.get(section, 'uri2'+name))
            scorePosition=int(config.get(section, 'scorePosition'+name))
            delimiter=config.get(section, 'delimiter'+name)
            if delimiter=='t':
                #print "Have to change delimiter!"
                delimiter=str('\t')

            parseParms={'uri1':uri1, 'uri2':uri2, 'scorePosition':scorePosition, 'delimiter':delimiter}

            fuzzyUpperLimit=float(config.get(section,'fuzzyUpperLimit'))
            fuzzyLowerLimit=float(config.get(section,'fuzzyLowerLimit'))
            fuzzyUpperFactor=float(config.get(section,'fuzzyUpperFactor'))
            fuzzyLowerFactor=float(config.get(section,'fuzzyLowerFactor'))
            oxoDistanceOne=float(config.get(section,'oxoDistanceOne'))
            oxoDistanceTwo=float(config.get(section,'oxoDistanceTwo'))
            oxoDistanceThree=float(config.get(section,'oxoDistanceThree'))
            synFuzzyFactor=float(config.get(section,'synFuzzyFactor'))
            bridgeOxoFactor=float(config.get(section,'bridgeOxoFactor'))
            threshold=float(config.get(section,'threshold'))
            #params={"fuzzyUpperLimit": fuzzyUpperLimit, "fuzzyLowerLimit": fuzzyLowerLimit,"fuzzyUpperFactor": fuzzyUpperFactor,"fuzzyLowerFactor":fuzzyLowerFactor, "oxoDistanceOne":oxoDistanceOne, "oxoDistanceTwo":oxoDistanceTwo, "oxoDistanceThree":oxoDistanceThree, "synFuzzyFactor":synFuzzyFactor, "synOxoFactor": synOxoFactor, "bridgeOxoFactor":bridgeOxoFactor, "threshold":threshold}
            params={"fuzzyUpperLimit": fuzzyUpperLimit, "fuzzyLowerLimit": fuzzyLowerLimit,"fuzzyUpperFactor": fuzzyUpperFactor,"fuzzyLowerFactor":fuzzyLowerFactor, "oxoDistanceOne":oxoDistanceOne, "oxoDistanceTwo":oxoDistanceTwo, "oxoDistanceThree":oxoDistanceThree, "synFuzzyFactor":synFuzzyFactor, "bridgeOxoFactor":bridgeOxoFactor, "threshold":threshold}

            #print "Validate "+sourceOntology+" "+targetOntology+" "+name
            returnCV=calculateAndValidateOntologyPrimaryScore(sourceOntology, targetOntology, name, stdFile, params, scoringtargetFolder, writeToDiscFlag, predictedTargetFolder, parseParms, curationOfDoubleEntries,validationTargetFolder,config.get('Basics','olsAPIURL')+"search")
            #returnCV=[{"source":sourceOntology,"target": targetOntology}, returnCV]
            returnValue.append({"source":sourceOntology,"target": targetOntology, "misses":returnCV['misses'], "alternatives":returnCV['alternatives']})
            #returnValue={"source":sourceOntology,"target": targetOntology, "misses":returnCV['misses'], "alternatives":returnCV['alternatives']}

    return returnValue

#Goes through the sections and calls calculateOntologyPrimaryScore for every section
def calculateListOntologies(sections, writeToDisc, curationOfDoubleEntries):
    scoringTargetFolder=config.get('Params','scoringTargetFolder')
    predictedTargetFolder=config.get('Params','predictedTargetFolder')

    if os.path.exists(scoringTargetFolder)==False and writeToDisc==True:
        print "Could not find "+scoringTargetFolder+" - please make sure the folder exists!\n"
        raise Exception("Folder does not exists")

    if os.path.exists(predictedTargetFolder)==False and writeToDisc==True:
        print "Could not find "+predictedTargetFolder+" - please make sure the folder exists!\n"
        raise Exception("Folder does not exists")

    for section in sections:
        sourceOntology=config.get(section, 'sourceOntology')
        targetOntology=config.get(section, 'targetOntology')

        fuzzyUpperLimit=float(config.get(section,'fuzzyUpperLimit'))
        fuzzyLowerLimit=float(config.get(section,'fuzzyLowerLimit'))
        fuzzyUpperFactor=float(config.get(section,'fuzzyUpperFactor'))
        fuzzyLowerFactor=float(config.get(section,'fuzzyLowerFactor'))
        oxoDistanceOne=float(config.get(section,'oxoDistanceOne'))
        oxoDistanceTwo=float(config.get(section,'oxoDistanceTwo'))
        oxoDistanceThree=float(config.get(section,'oxoDistanceThree'))
        synFuzzyFactor=float(config.get(section,'synFuzzyFactor'))
        #synOxoFactor=float(config.get(section,'synOxoFactor'))
        bridgeOxoFactor=float(config.get(section,'bridgeOxoFactor'))
        threshold=float(config.get(section,'threshold'))
        #params={"fuzzyUpperLimit": fuzzyUpperLimit, "fuzzyLowerLimit": fuzzyLowerLimit,"fuzzyUpperFactor": fuzzyUpperFactor,"fuzzyLowerFactor":fuzzyLowerFactor, "oxoDistanceOne":oxoDistanceOne, "oxoDistanceTwo":oxoDistanceTwo, "oxoDistanceThree":oxoDistanceThree, "synFuzzyFactor":synFuzzyFactor, "synOxoFactor": synOxoFactor, "bridgeOxoFactor":bridgeOxoFactor, "threshold":threshold}
        params={"fuzzyUpperLimit": fuzzyUpperLimit, "fuzzyLowerLimit": fuzzyLowerLimit,"fuzzyUpperFactor": fuzzyUpperFactor,"fuzzyLowerFactor":fuzzyLowerFactor, "oxoDistanceOne":oxoDistanceOne, "oxoDistanceTwo":oxoDistanceTwo, "oxoDistanceThree":oxoDistanceThree, "synFuzzyFactor":synFuzzyFactor, "bridgeOxoFactor":bridgeOxoFactor, "threshold":threshold}
        print "Calculate "+sourceOntology+" "+targetOntology
        logging.info("Calculate "+sourceOntology+" "+targetOntology)
        calculatePrimaryScore(sourceOntology+"_"+targetOntology, params, scoringTargetFolder, writeToDisc, predictedTargetFolder, curationOfDoubleEntries)


def exportNeoList(sections):
    for section in sections:
        sourceOntology=config.get(section, 'sourceOntology')
        targetOntology=config.get(section, 'targetOntology')
        predictedFolder=config.get('Params','predictedTargetFolder')
        targetFolder=config.get('Params','neoFolder')

        if os.path.exists(predictedFolder)==False:
            print "Could not find "+predictedFolder+" - please make sure input file exists!\n"
            raise Exception("File does not exists")
        if os.path.exists(targetFolder)==False:
            print "Could not find "+targetFolder+" - please make sure input file exists!\n"
            raise Exception("File does not exists")

        olsURL=config.get('Basics', 'olsAPIURL')
        neoURL=config.get('Basics','neoURL')
        neoUser=config.get('Basics','neoUser')
        neoPW=config.get('Basics','neoPW')

        neoExporter.exportInNeo(sourceOntology, targetOntology, predictedFolder, targetFolder, olsURL, neoURL, neoUser, neoPW)

    print "Completed neo4J export"


def runListAnnotation():
    inputFile=config.get("Basics","inputFile")
    resultFile=config.get("Basics","resultFile")
    if os.path.exists(inputFile)==False:
        print "Could not find "+inputFile+" - please make sure input file exists!\n"
        raise Exception("File does not exists")

    targetOntology=config.get("Basics", 'targetOntology')
    delimiter=config.get("Basics", 'delimiter')
    olsURL=config.get("Basics","olsAPIURL")
    oxoURL=config.get("Basics","oxoURL")
    detailLevel=int(config.get("Basics","detailLevel"))


    if delimiter=='t':
        print "Change delimiter to \t!"
        delimiter=str('\t')


    fuzzyUpperLimit=float(config.get("Basics",'fuzzyUpperLimit'))
    fuzzyLowerLimit=float(config.get("Basics",'fuzzyLowerLimit'))
    fuzzyUpperFactor=float(config.get("Basics",'fuzzyUpperFactor'))
    fuzzyLowerFactor=float(config.get("Basics",'fuzzyLowerFactor'))
    oxoDistanceOne=float(config.get("Basics",'oxoDistanceOne'))
    oxoDistanceTwo=float(config.get("Basics",'oxoDistanceTwo'))
    oxoDistanceThree=float(config.get("Basics",'oxoDistanceThree'))
    synFuzzyFactor=float(config.get("Basics",'synFuzzyFactor'))
    #synOxoFactor=float(config.get("Basics",'synOxoFactor'))
    bridgeOxoFactor=float(config.get("Basics",'bridgeOxoFactor'))
    threshold=float(config.get("Basics",'threshold'))
    stopwordList=config.get("Params","StopwordsList").split(',')
    synonymSplitChar=config.get("Basics","synonymSplitChar")

    #params={"fuzzyUpperLimit": fuzzyUpperLimit, "fuzzyLowerLimit": fuzzyLowerLimit,"fuzzyUpperFactor": fuzzyUpperFactor,"fuzzyLowerFactor":fuzzyLowerFactor, "oxoDistanceOne":oxoDistanceOne, "oxoDistanceTwo":oxoDistanceTwo, "oxoDistanceThree":oxoDistanceThree, "synFuzzyFactor":synFuzzyFactor, "synOxoFactor": synOxoFactor, "bridgeOxoFactor":bridgeOxoFactor, "threshold":threshold, "ols": olsURL, "oxo":oxoURL}
    params={"fuzzyUpperLimit": fuzzyUpperLimit, "fuzzyLowerLimit": fuzzyLowerLimit,"fuzzyUpperFactor": fuzzyUpperFactor,"fuzzyLowerFactor":fuzzyLowerFactor, "oxoDistanceOne":oxoDistanceOne, "oxoDistanceTwo":oxoDistanceTwo, "oxoDistanceThree":oxoDistanceThree, "synFuzzyFactor":synFuzzyFactor, "bridgeOxoFactor":bridgeOxoFactor, "threshold":threshold, "ols": olsURL, "oxo":oxoURL}
    options={"inputFile":inputFile, "resultFile":resultFile, "delimiter":delimiter, "targetOntology":targetOntology, "detailLevel": detailLevel, "synonymSplitChar":synonymSplitChar}
    #ScoreParameters define stopwords
    scoreParams={"removeStopwordsList": stopwordList, "replaceTermList" : replacementTerms}

    listprocessing.runListProcessing(options, params, scoreParams)




## Here main starts ##

####First definition of two global variables
replacementTerms={
"ordo_hp" : [('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('tumor', 'neoplasm'), ('tumor','cancer'), ('abnormality', 'disease'), ('decreased', 'reduced'), ('morphology', '')],
"doid_mp" : [],
"doid_ordo" :[],
"hp_doid" : [('cancer', 'neoplasm'), ('cancer','carcinoma'), ('abnormality','disease'), 'abnormality','disease'],
"hp_mp" : [('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('abnormality','disease'), ('abnormal','Abnormality')],
"ordo_mp" : []
}

helptext="""Start the client with exactly two input parameters: The path to the config file and one of the following flags:
            -l: Create mappings for a list of terms with an ontology
            -s: Create the primary raw score for two ontologies or a set of two ontologies.
            -c: Calculate from the primary raw score a predicted score
            -cv: Calculate a predicted score but also validate it against given standard files.
            -n: Reads in a predicted score file and exports it to a neo4j compatible format

            example: python paxo.py config.ini -s
            """
#Parse the input parameters. A config file and a flag is expected
if len(sys.argv)<3:
    print helptext
    print "\nNot enough arguments! Take exactly two, "+str(len(sys.argv)-1)+" given!"
    #raise
elif len(sys.argv)>3:
    print helptext
    print "\nToo many arguments! Take exactly two, "+str(len(sys.argv)-1)+" given!"
    raise
elif len(sys.argv)==3:
    config = SafeConfigParser()
    config.read(sys.argv[1])
    logFile=config.get("Basics","logFile")
    logging.basicConfig(filename=logFile, level=logging.INFO, format='%(asctime)s - %(message)s')
    writeToDiscFlag=config.getboolean("Params","writeToDiscFlag")
    uniqueMaps=config.getboolean("Params","uniqueMaps")

    #Throw away the first 2 sections and take only the actual mapping part of the config into account
    sections=config.sections()[2:]
    if sys.argv[2]=="-l":
        runListAnnotation()
    elif sys.argv[2]=="-s":
        scoreListOntologies(sections)
    elif sys.argv[2]=="-c":
        calculateListOntologies(sections, writeToDiscFlag, uniqueMaps)
    elif sys.argv[2]=="-cv":
        print calculateAndValidateListOntologies(sections, writeToDiscFlag, uniqueMaps)
    elif sys.argv[2]=="-n":
        exportNeoList(sections)
    else:
        print "Could not recognize option. So I execute what's uncommented in the else branch. This should just be during development"
        #Could/Should be changed so parameters come from the config file
        params={"fuzzyUpperLimit": 0.8, "fuzzyLowerLimit": 0.6,"fuzzyUpperFactor": 1,"fuzzyLowerFactor":0.6, "oxoDistanceOne":1, "oxoDistanceTwo":0.3, "oxoDistanceThree":0.1, "synFuzzyFactor":0.6, "synOxoFactor": 0.4, "bridgeOxoFactor":1, "threshold":0.6}

        ###Execute functions for terms
        scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : []}
        params={"fuzzyUpperLimit": 0.6, "fuzzyLowerLimit": 0.6,"fuzzyUpperFactor": 1,"fuzzyLowerFactor":0.6, "oxoDistanceOne":1, "oxoDistanceTwo":0.3, "oxoDistanceThree":0.1, "synFuzzyFactor":0.6, "synOxoFactor": 0.4, "bridgeOxoFactor":1, "threshold":0.6, "ols":"https://www.ebi.ac.uk/ols/api/", "oxo":"https://www.ebi.ac.uk/ols/api/"}
        print paxo_internals.scoreTermLabel("Nuclear cataract", "doid", scoreParams, params)
