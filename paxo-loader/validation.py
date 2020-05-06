import csv
import logging
import requests
import time

def validateFinaleScore(onto1, onto2, stdNamed, inputFile, TargetFile, writeToDisc, params, parseParms, validationTargetFolder, url):
    uri1Position=parseParms['uri1']
    uri2Position=parseParms['uri2']
    counterPosition=parseParms['scorePosition']
    delimiterChar=parseParms['delimiter']

    inputList=[]
    inputLongList=[]
    for row in inputFile:
        inputList.append([row[0], row[1]])
        inputLongList.append(row)

    targetList=[]
    targetLongList=[]
    with open(TargetFile) as csvfile:
        readCSV = csv.reader(csvfile, delimiter=str(delimiterChar))

        next(readCSV)
        try:
            for row in readCSV:
                targetList.append([row[uri1Position], row[uri2Position]])
                targetLongList.append(row)
        except Exception as e:
            print "Error while reading in target file"
            print row
            raise e

    missing=[]
    matches=[]

    #Now validate the computed mappings against the standard
    for counter, line in enumerate(targetList):

            if line not in inputList:
                missing.append([line[0], line[1], "NoScore", targetLongList[counter][counterPosition], targetLongList[counter][1], targetLongList[counter][3]])

            #Exact same Result for both, so this is a match. Is added to the matches List
            else:
                for c in inputLongList:
                    if c[0]==line[0] and c[1]==line[1] or c[1]==line[0] and c[1]==line[1]:
                        score=c[2]

                matches.append([line[0], line[1], score, targetLongList[counter][counterPosition],targetLongList[counter][1], targetLongList[counter][3]])
            #Add those mappings that where no in the standard but calculated to the alternatives List

    alternatives=[]
    for counter, line in enumerate(inputList):
        if line not in targetList and line[1]!="UNKNOWN":
            alternatives.append([line[0], line[1], inputLongList[counter][2], "noScore", inputLongList[counter][3], inputLongList[counter][4]])


    #Alternative Counter
    alternativeCounter=0
    unrealMiss=[]
    realMiss=[]
    for sug in alternatives:
        for miss in missing:
            if (sug[0]==miss[0] and sug[1]!=miss[1]) or (sug[0]!=miss[0] and sug[1]==miss[1]):
                alternativeCounter=alternativeCounter+1
                unrealMiss.append(sug)
                unrealMiss.append(miss)
            #Real miss
            else:
                realMiss.append(miss)


    result=matches+missing+alternatives#+discarted - we can also show the discarted terms or put them in an own file

    #print result
    #If we write to disc, I get the labels of the parts that are NOT mapped to the standard
    if writeToDisc is True:
        #print "Try to save the result"
        obsoleteScore=0
        # for row in result:
        #     #if row[2]=='NoScore' or row[3]=='noScore':
        #         #print "Need to annotate "+row[0]+" and "+row[1]
        #
        #         data={'q':row[0],'queryFields':'iri', 'fieldList': 'label', "ontology":onto1, "type":"class", "local":True}
        #
        #         try:
        #             r = requests.get(url, data)
        #         except:
        #             time.sleep(60)
        #             logging.info("API exception, try again after 5 second delay")
        #             print "API exception, try again after 5 second delay"
        #             try:
        #                 r = requests.get(url, data)
        #                 logging.info("Success")
        #                 print "Success!"
        #             except:
        #                 logging.info("Error with second try")
        #                 logging.info(r.status_code)
        #                 logging.info(r.request.url)
        #                 #raise
        #
        #
        #         try:
        #             jsonReply=r.json()
        #             row.append(jsonReply['response']['docs'][0]['label'].encode(encoding='UTF-8'))
        #         except:
        #             row.append('NoLabel Found')
        #             obsoleteScore=obsoleteScore+1
        #             print "No Label found in the first row"
        #
        #         data={'q':row[1],'queryFields':'iri', 'fieldList': 'label', "ontology":onto2, "type":"class", "local":True}
        #         try:
        #             r = requests.get(url, data)
        #         except:
        #             time.sleep(60)
        #             logging.info("API exception, try again after 5 second delay")
        #             print "API exception, try again after 5 second delay"
        #             try:
        #                 r = requests.get(url, data)
        #                 logging.info("Success")
        #                 print "Success"
        #             except:
        #                 logging.info("Error with second try")
        #                 logging.info(r.status_code)
        #                 logging.info(r.request.url)
        #
        #
        #         try:
        #             jsonReply=r.json()
        #             row.append(jsonReply['response']['docs'][0]['label'].encode(encoding='UTF-8'))
        #         except:
        #             row.append('NoLabel Found')
        #             obsoleteScore=obsoleteScore+1
        #             print "No Label found in the second row"


        with open(validationTargetFolder+onto1+"_"+onto2+'_'+stdNamed+'_validate.csv', 'wb') as f:
            writer = csv.writer(f)
            writer.writerows(result)
            f.close()

        #Logging the stats of this validation
        logging.info("ParameterSet for this validation run,  ")
        logging.info("threshold,   "+str(params["threshold"]))
        logging.info("fuzzyUpperLimit,   "+str(params["fuzzyUpperLimit"]))
        logging.info("fuzzyLowerLimit,   "+str(params["fuzzyLowerLimit"]))
        logging.info("fuzzyUpperFactor,   "+str(params["fuzzyUpperFactor"]))
        logging.info("fuzzyLowerFactor,   "+str(params["fuzzyLowerFactor"]))
        logging.info("oxoDistanceOne,   "+str(params["oxoDistanceOne"]))
        logging.info("oxoDistanceTwo,   "+str(params["oxoDistanceTwo"]))
        logging.info("oxoDistanceThree,   "+str(params["oxoDistanceThree"]))
        logging.info("synFuzzyFactor,   "+str(params["synFuzzyFactor"]))
        #logging.info("synOxoFactor,   "+str(params["synOxoFactor"]))

        msg="Stats for "+str(onto1)+"_"+str(onto2)+" validation "+stdNamed+"\n"
        msg=msg+"Number of std mappings, "+str(len(targetList))+"\n"
        msg=msg+"Total Matches, "+str(len(matches))+"\n"
        msg=msg+"Algorithm missed compared to std, "+str(len(missing))+"\n"
        msg=msg+"Suspected Obsoleted Terms, "+str(obsoleteScore)+"\n"
        msg=msg+"Algorithm missed compared to std MINUS obsoleted terms in std, "+str(len(missing)-obsoleteScore)+"\n"
        msg=msg+"Total unique terms suggested, "+str(len(alternatives))+"\n"
        msg=msg+"UniqueOverlappingWithMisses, "+str(alternativeCounter)+"\n"
        msg=msg+"Recall, "+str(round(((len(matches)/(len(targetList)-obsoleteScore*1.0))*100),2))
        logging.info(msg)
        #logging.info("NotMapped: "+str(len(discarted))+"\n")


    #return result
    return {"misses": len(missing), "alternatives": len(alternatives)}
    #Return the parameters and the result of the validation. This shall be useful in the future
    #return {"misses": len(missing), "alternativeCounter": alternativeCounter, "params":runParams}
