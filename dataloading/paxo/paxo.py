import paxo_internals
import logging
import validation
import csv
import time
import requests
import json
from ConfigParser import SafeConfigParser
import ast
import neoExporter
import sys
import listprocessing


#Compares to ontologies from the OLS. This process can take a while and procudes a csv with primary results
def scoreOntologies(sourceOntology, targetOntology, scoreParams, scoringtargetFolder):
    logging.info("Start scoring "+sourceOntology+" and "+targetOntology)
    #Check for the smaller ontology

    olsURL=config.get("Basics","olsAPIURL")
    oxoURL=config.get("Basics","oxoURL")
    urls={"ols":olsURL, "oxo":oxoURL}


    try:
        r = requests.get(olsURL+"ontologies/"+sourceOntology)
        numberOfTerms=r.json()['numberOfTerms']
        r = requests.get(olsURL+"ontologies/"+targetOntology)
        numberOfTerms2 = r.json()['numberOfTerms']
    except:
        logging.error("Error getting number of terms throw webservice call!")
        logging.error(olsURL+"ontologies/"+sourceOntology)
        logging.error(olsURL+"ontologies/"+targetOntology)
        logging.error(r)
        raise

    #In case the targetOntology is smaller than the source Ontology, switch the output
    if (numberOfTerms>numberOfTerms2):
        tmpOntology=sourceOntology
        sourceOntology=targetOntology
        targetOntology=tmpOntology

    termsUrl=olsURL+"ontologies/"+sourceOntology+"/terms?size=500&fieldList=iri,label,synonym"
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

                    results.append([originalLabel.encode(encoding='UTF-8'), term["iri"].encode(encoding='UTF-8'), calculatedMappings['olsFuzzyScore'], calculatedMappings['oxoScore'], synCalculatedMappings['olsFuzzyScore'], synCalculatedMappings['oxoScore'], calculatedMappings['bridgeEvidence']])


        try:
            termsUrl=r.json()['_links']['next']['href']
            counter=counter+1
            if counter%2==0:
                print "Processed "+str(counter)+" pages"
                logging.info("Processed "+str(counter)+" pages")
                #break  #Uncomment this for testing (to not parse the whole ontology)
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
        #else:
        #    print "entry in results!"

    #SortScore
    tmp=sorted(tmp, key=lambda tmp:tmp[0]['finaleScore'], reverse=True)
    return tmp

#Maybe transfer to server
def scoreTermList(termList, targetOntology, scoreParams, params):
    result=[]
    for term in termList:
        result.append(paxo_internals.scoreTermLabel(term, targetOntology, scoreParams, params))
    return result

# Process an IRI list via OLS instead of a termList
# def scoreIriList(IriList, targetOntology, params):

#Process scoredMatrix to prepare for validation or save to disc
def writeOutPutScore(scoredMatrix, name, predictedTargetFolder, saveToDisc):
    result=[]

    for line in scoredMatrix:
        result.append([line[0]['sourceIRI'], line[0]['iri'], line[0]['finaleScore'], line[0]['sourceTerm']])

    if saveToDisc==True:
        with open(predictedTargetFolder+'calculated_output_'+name+'.csv', 'w') as f:
            writer = csv.writer(f)
            writer.writerows(result)
            f.close()

    return result

#Not implemented yet #Remove double entry stuff #Potentially
def curationOntologyFinalScore(scoredMatrix):
    endmap=[]
    unified=[]
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
                #replacedList.append(unified[index])
                unified[index]=scoredMatrix[counter] #Replace that line with the higher scored line!


    print "A total of "+str(doubleEntryCounter)+" processed!"
    if len(replacedList)!=0:
        print "Write file of replaced terms now"
        print "Total length of replaced is "+str(len(replacedList))
    #    with open('pipeline_output/replaced_terms.csv', 'wb') as f:
    #        writer = csv.writer(f)
    #        writer.writerows(replacedList)
    #        f.close()

    return unified



def calculatePrimaryScore(combinedOntologyName, params, scoringTargetFolder, writeToDisc, predictedTargetFolder, curationOfDoubleEntries):
    simplerMatrix=scoreOntologyPrimaryScore(combinedOntologyName, scoringTargetFolder)
    scoredMatrix=processOntologyPrimaryScore(simplerMatrix, params)
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
    for section in sections:
        sourceOntology=config.get(section, 'sourceOntology')
        targetOntology=config.get(section, 'targetOntology')
        stopwordList=config.get("Params","StopwordsList").split(',')
        #print stopwordList
        #print type(stopwordList)
        #hp_doid_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : [('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('abnormality','disease')]}
        scoreParams={"removeStopwordsList":stopwordList, "replaceTermList" : []}
        print "Score "+sourceOntology+" "+targetOntology
        scoreOntologies(sourceOntology, targetOntology, scoreParams, scoringtargetFolder)

#Goes through the sections and calls calculateAndValidateOntologyPrimaryScore for every section
def calculateAndValidateListOntologies(sections, writeToDiscFlag, curationOfDoubleEntries):
    validationTargetFolder=config.get('Params', 'validationTargetFolder')
    scoringtargetFolder=config.get('Params', 'scoringTargetFolder')
    predictedTargetFolder=config.get('Params', 'predictedTargetFolder')
    for section in sections:
        sourceOntology=config.get(section, 'sourceOntology')
        targetOntology=config.get(section, 'targetOntology')

        stdNames=config.get(section, 'standard').split(',')
        #stdNames=['loom', 'silver']
        for name in stdNames:
            stdFile=config.get(section, name)
            uri1=int(config.get(section, 'uri1'+name))
            uri2=int(config.get(section, 'uri2'+name))
            scorePosition=int(config.get(section, 'scorePosition'+name))
            delimiter=config.get(section, 'delimiter'+name)
            if delimiter=='t':
                print "have to change delimiter!"
                delimiter=str('\t')

            parseParms={'uri1':uri1, 'uri2':uri2, 'scorePosition':scorePosition, 'delimiter':delimiter}

            #params={"exactFactor":1, "fuzzyFactor": 1, "oxoFactor": 1, "synExactFactor": 1, "synFuzzyFactor": 1, "synOxoFactor": 1, "threshold":0.3}
            fuzzyUpperLimit=float(config.get(section,'fuzzyUpperLimit'))
            fuzzyLowerLimit=float(config.get(section,'fuzzyLowerLimit'))
            fuzzyUpperFactor=float(config.get(section,'fuzzyUpperFactor'))
            fuzzyLowerFactor=float(config.get(section,'fuzzyLowerFactor'))
            oxoDistanceOne=float(config.get(section,'oxoDistanceOne'))
            oxoDistanceTwo=float(config.get(section,'oxoDistanceTwo'))
            oxoDistanceThree=float(config.get(section,'oxoDistanceThree'))
            synFuzzyFactor=float(config.get(section,'synFuzzyFactor'))
            synOxoFactor=float(config.get(section,'synOxoFactor'))
            bridgeOxoFactor=float(config.get(section,'bridgeOxoFactor'))
            threshold=float(config.get(section,'threshold'))
            params={"fuzzyUpperLimit": fuzzyUpperLimit, "fuzzyLowerLimit": fuzzyLowerLimit,"fuzzyUpperFactor": fuzzyUpperFactor,"fuzzyLowerFactor":fuzzyLowerFactor, "oxoDistanceOne":oxoDistanceOne, "oxoDistanceTwo":oxoDistanceTwo, "oxoDistanceThree":oxoDistanceThree, "synFuzzyFactor":synFuzzyFactor, "synOxoFactor": synOxoFactor, "bridgeOxoFactor":bridgeOxoFactor, "threshold":threshold}

            print "Validate "+sourceOntology+" "+targetOntology+" "+name
            print calculateAndValidateOntologyPrimaryScore(sourceOntology, targetOntology, name, stdFile, params, scoringtargetFolder, writeToDiscFlag, predictedTargetFolder, parseParms, curationOfDoubleEntries,validationTargetFolder,config.get('Basics','olsAPIURL')+"search")


#Goes through the sections and calls calculateOntologyPrimaryScore for every section
def calculateListOntologies(sections, writeToDisc, curationOfDoubleEntries):
    scoringTargetFolder=config.get('Params','scoringTargetFolder')
    predictedTargetFolder=config.get('Params','predictedTargetFolder')

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
        synOxoFactor=float(config.get(section,'synOxoFactor'))
        bridgeOxoFactor=float(config.get(section,'bridgeOxoFactor'))
        threshold=float(config.get(section,'threshold'))
        params={"fuzzyUpperLimit": fuzzyUpperLimit, "fuzzyLowerLimit": fuzzyLowerLimit,"fuzzyUpperFactor": fuzzyUpperFactor,"fuzzyLowerFactor":fuzzyLowerFactor, "oxoDistanceOne":oxoDistanceOne, "oxoDistanceTwo":oxoDistanceTwo, "oxoDistanceThree":oxoDistanceThree, "synFuzzyFactor":synFuzzyFactor, "synOxoFactor": synOxoFactor, "bridgeOxoFactor":bridgeOxoFactor, "threshold":threshold}


        print predictedTargetFolder
        print "Calculate "+sourceOntology+" "+targetOntology
        print scoringTargetFolder
        print calculatePrimaryScore(sourceOntology+"_"+targetOntology, params, scoringTargetFolder, writeToDisc, predictedTargetFolder, curationOfDoubleEntries)


def exportNeoList(sections):
    for section in sections:
        sourceOntology=config.get(section, 'sourceOntology')
        targetOntology=config.get(section, 'targetOntology')
        predictedFolder=config.get('Params','predictedTargetFolder')
        targetFolder=config.get('Params','neoFolder')

        olsURL=config.get('Basics', 'olsAPIURL')
        neoURL=config.get('Basics','neoURL')
        neoUser=config.get('Basics','neoUser')
        neoPW=config.get('Basics','neoPW')

        neoExporter.exportInNeo(sourceOntology, targetOntology, predictedFolder, targetFolder, olsURL, neoURL, neoUser, neoPW)

    print "Completed neo4J export"


def runListAnnotation():
    print "In list processing, let's get config"
    inputFile=config.get("Basics","inputFile")
    resultFile=config.get("Basics","resultFile")
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
    synOxoFactor=float(config.get("Basics",'synOxoFactor'))
    bridgeOxoFactor=float(config.get("Basics",'bridgeOxoFactor'))
    threshold=float(config.get("Basics",'threshold'))
    stopwordList=config.get("Params","StopwordsList").split(',')

    params={"fuzzyUpperLimit": fuzzyUpperLimit, "fuzzyLowerLimit": fuzzyLowerLimit,"fuzzyUpperFactor": fuzzyUpperFactor,"fuzzyLowerFactor":fuzzyLowerFactor, "oxoDistanceOne":oxoDistanceOne, "oxoDistanceTwo":oxoDistanceTwo, "oxoDistanceThree":oxoDistanceThree, "synFuzzyFactor":synFuzzyFactor, "synOxoFactor": synOxoFactor, "bridgeOxoFactor":bridgeOxoFactor, "threshold":threshold, "ols": olsURL, "oxo":oxoURL}
    options={"inputFile":inputFile, "resultFile":resultFile, "delimiter":delimiter, "targetOntology":targetOntology, "detailLevel": detailLevel}
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
elif len(sys.argv)>3:
    print helptext
    print "\nToo many arguments! Take exactly two, "+str(len(sys.argv)-1)+" given!"
else:

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
        calculateAndValidateListOntologies(sections, writeToDiscFlag, uniqueMaps)
    elif sys.argv[2]=="-n":
        exportNeoList(sections)
    else:
        print "Could not recognize option. So I execute what's uncommented in the else branch. This should just be during development"
        #removeStopwordsList=['of', 'the']
        #replaceTermList=[('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('abnormality','disease')]
        #scoreParams={"removeStopwordsList": removeStopwordsList, "replaceTermList" :replaceTermList}
        #hp_doid_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : [('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('abnormality','disease')]}


        ### Primary score ontologies
        #ordo_hp_scoreParams={"removeStopwordsList": ['of', 'the', 'Rare'], "replaceTermList" : [('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('tumor', 'neoplasm'), ('tumor','cancer'), ('abnormality', 'disease'), ('decreased', 'reduced'), ('morphology', '')]}
        #scoreOntologies("ordo","hp", ordo_hp_scoreParams, 'final_dec/scoring/')
        #
        #doid_mp_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : []}
        #scoreOntologies("doid","mp", doid_mp_scoreParams, 'final_dec/scoring/')
        # #
        # doid_ordo_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : []}
        # scoreOntologies("doid","ordo", doid_ordo_scoreParams, 'final_dec/scoring/')
        # #
        # hp_doid_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : [('cancer', 'neoplasm'), ('cancer','carcinoma'), ('abnormality','disease'), 'abnormality','disease']}
        # scoreOntologies("hp","doid",hp_doid_scoreParams, 'final_dec/scoring/')
        # #
        # hp_mp_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : [('cancer', 'carcinom'), ('cancer', 'neoplasm'), ('cancer','carcinoma'),('abnormality','disease'), ('abnormal','Abnormality')]}
        # scoreOntologies("hp","mp", hp_mp_scoreParams, 'final_dec/scoring/')
        # #
        # ordo_mp_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : []}
        # scoreOntologies("ordo","mp", ordo_mp_scoreParams, 'final_dec/scoring/')


        #mesh_scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : []}
        #scoreOntologies("mesh","hp", mesh_scoreParams, 'final_dec/scoring/')
        #scoreOntologies("mesh","doid", mesh_scoreParams, 'final_dec/scoring/')
        #scoreOntologies("mesh","ordo", mesh_scoreParams, 'final_dec/scoring/')
        #scoreOntologies("mesh","mp", mesh_scoreParams, 'final_dec/scoring/')



        #Could/Should be changed so parameters come from the config file
        params={"fuzzyUpperLimit": 0.8, "fuzzyLowerLimit": 0.6,"fuzzyUpperFactor": 1,"fuzzyLowerFactor":0.6, "oxoDistanceOne":1, "oxoDistanceTwo":0.3, "oxoDistanceThree":0.1, "synFuzzyFactor":0.6, "synOxoFactor": 0.4, "bridgeOxoFactor":1, "threshold":0.6}
        #params={"fuzzyUpperLimit": 0.8, "fuzzyLowerLimit": 0.6,"fuzzyUpperFactor": 1,"fuzzyLowerFactor":0.6, "oxoDistanceOne":1, "oxoDistanceTwo":0.3, "oxoDistanceThree":0.1, "synFuzzyFactor":0.6, "synOxoFactor": 0.4, "bridgeOxoFactor":1, "threshold":0.8}

        ### Execute Calculate and validate for a certain file
        #print calculateAndValidateOntologyPrimaryScore('hp', 'doid', 'loom', 'Loom/DOID_HP_loom.csv', params, 'final_dec/scoring/', writeToDiscFlag, 'final_dec/predicted/', {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps, 'final_dec/validation/')
        #print calculateAndValidateOntologyPrimaryScore('hp','doid', 'silver','silver_nov/Consensus-3-hp-doid.tsv', params, 'final_dec/scoring/', writeToDiscFlag, 'final_dec/predicted/',{'uri1':0, 'uri2':2, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps, 'final_dec/validation/')
        #print calculateAndValidateOntologyPrimaryScore('ordo', 'hp', 'loom', 'Loom/ordo_hp_loom.csv', params,'final_dec/scoring/',  writeToDisc, final_dec/predicted/', {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps, 'final_dec/validation/')


        #params={"fuzzyUpperLimit": 0, "fuzzyLowerLimit": 0,"fuzzyUpperFactor": 0.65, "fuzzyLowerFactor":0, "oxoDistanceOne":0.00029, "oxoDistanceTwo":0.57, "oxoDistanceThree":0.027, "synFuzzyFactor":0.247, "synOxoFactor": 0.62, "bridgeOxoFactor":0.829, "threshold":0.6}

        #print calculateAndValidateOntologyPrimaryScore('ordo', 'hp', 'silver','silver_nov/Consensus-3-hp-ordo.tsv', params,'final_dec/scoring/', writeToDiscFlag, 'final_dec/predicted/',{'uri1':2, 'uri2':0, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps, 'final_dec/validation/')
        # {'misses': 210, 'alternatives': 350}

        # #
        # print calculateAndValidateOntologyPrimaryScore('mp','hp', 'loom','Loom/MP_HP_loom.csv', params,'final_dec/scoring/', writeToDiscFag, 'final_dec/predicted/', {'uri1':0, 'uri2':1, 'scorePosition':2 , 'delimiter':','}, uniqueMaps, 'final_dec/evaluation/')
        #print calculateAndValidateOntologyPrimaryScore('mp','hp', 'silver','silver_nov/Consensus-3-hp-mp.tsv', params, 'final_dec/scoring/',writeToDiscFlag, 'final_dec/predicted/', {'uri1':2, 'uri2':0, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps, 'final_dec/evaluation/')
        # print calculateAndValidateOntologyPrimaryScore('ordo','doid', 'loom' ,'Loom/DOID_ORDO_loom.csv', params, 'final_dec/scoring/',writeToDisc, 'final_dec/predicted/', {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps, 'final_dec/evaluation/')
        #print calculateAndValidateOntologyPrimaryScore('ordo','doid', 'silver','silver_nov/Consensus-3-doid-ordo.tsv', params, 'final_dec/scoring/', writeToDiscFlag,'final_dec/predicted/', {'uri1':2, 'uri2':0, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps, 'final_dec/evaluation/')
        # print calculateAndValidateOntologyPrimaryScore('ordo','mp', 'loom', 'Loom/mp_ordo_loom.csv', params,'final_dec/scoring/', writeToDiscFlag, 'final_dec/predicted/', {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps, 'final_dec/evaluation/')
        #print calculateAndValidateOntologyPrimaryScore('ordo','mp', 'silver','silver_nov/Consensus-3-mp-ordo.tsv', params,'final_dec/scoring/', writeToDiscFlag, 'final_dec/predicted/', {'uri1':2, 'uri2':0, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps, 'final_dec/evaluation/')
        #print calculateAndValidateOntologyPrimaryScore('mp','doid', 'loom', 'Loom/DOID_MP_loom.csv', params, 'final_dec/scoring/',writeToDiscFlag, 'final_dec/predicted/', {'uri1':1, 'uri2':0, 'scorePosition':2, 'delimiter':','}, uniqueMaps, 'final_dec/evaluation/')
        #print calculateAndValidateOntologyPrimaryScore('mp','doid', 'silver','silver_nov/Consensus-3-mp-doid.tsv', params, 'final_dec/scoring/',writeToDiscFlag,'final_dec/predicted/',  {'uri1':0, 'uri2':2, 'scorePosition':4 , 'delimiter':'\t'}, uniqueMaps, 'final_dec/evaluation/')
        # #
        #

        # print calculateAndValidateOntologyPrimaryScore('mesh','doid', 'loom', 'Loom/DOID_MESH_loom_new.csv', params, 'final_dec/scoring/',writeToDisc,'final_dec/predicted/',  {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps, 'final_dec/validation/')
        # print calculateAndValidateOntologyPrimaryScore('mesh','doid', 'silver', 'silver_nov/Consensus-3-doid-mesh3.tsv', 'final_dec/scoring/',params, writeToDisc, 'final_dec/predicted/', {'uri1':2, 'uri2':0, 'scorePosition':2, 'delimiter':'\t'}, uniqueMaps, 'final_dec/validation/')
        # print calculateAndValidateOntologyPrimaryScore('mesh','hp', 'loom', 'Loom/mesh_hp_loom_new.csv', params, 'final_dec/scoring/',writeToDisc,'final_dec/predicted/',  {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps, 'final_dec/validation/')
        # print calculateAndValidateOntologyPrimaryScore('mesh','hp', 'silver', 'silver_nov/Consensus-3-hp-mesh3.tsv', params, 'final_dec/scoring/',writeToDisc,'final_dec/predicted/',  {'uri1':1, 'uri2':0, 'scorePosition':2, 'delimiter':'\t'}, uniqueMaps, 'final_dec/validation/')
        # print calculateAndValidateOntologyPrimaryScore('mesh','mp', 'loom', 'Loom/mesh_mp_loom_new.csv', params, 'final_dec/scoring/',writeToDisc,'final_dec/predicted/',  {'uri1':0, 'uri2':1, 'scorePosition':2, 'delimiter':','}, uniqueMaps, 'final_dec/validation/')
        # print calculateAndValidateOntologyPrimaryScore('mesh','mp', 'silver', 'silver_nov/Consensus-3-mp-mesh3.tsv', params,'final_dec/scoring/', writeToDisc, 'final_dec/predicted/', {'uri1':1, 'uri2':0, 'scorePosition':2, 'delimiter':'\t'}, uniqueMaps, 'final_dec/validation/')


        #Just run calculate without validation
        #calculatePrimaryScore('ordo'+"_"+'doid', params, 'final_dec/scoring/', writeToDiscFlag, 'final_dec/predicted/', uniqueMaps)


        ###Execute functions for terms
        scoreParams={"removeStopwordsList": ['of', 'the'], "replaceTermList" : []}
        params={"fuzzyUpperLimit": 0.6, "fuzzyLowerLimit": 0.6,"fuzzyUpperFactor": 1,"fuzzyLowerFactor":0.6, "oxoDistanceOne":1, "oxoDistanceTwo":0.3, "oxoDistanceThree":0.1, "synFuzzyFactor":0.6, "synOxoFactor": 0.4, "bridgeOxoFactor":1, "threshold":0.6, "ols":"https://www.ebi.ac.uk/ols/api/", "oxo":"https://www.ebi.ac.uk/ols/api/"}
        print paxo_internals.scoreTermLabel("Nuclear cataract", "doid", scoreParams, params)
        #print scoreTermList(["Asthma", "Dermatitis atopic"], "doid", scoreParams, params)
