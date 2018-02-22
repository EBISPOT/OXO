#import clientOperations as paxo
import paxo_internals
import csv
import requests
import time

def runListProcessing(options, params, scoreParams):

    inputFile=options["inputFile"]
    resultFile=options["resultFile"]
    delimiter=options["delimiter"]
    targetOntology=options["targetOntology"]
    detailLevel=options["detailLevel"]

    #Open the input file
    with open(inputFile) as csvfile:
        readCSV = csv.reader(csvfile, delimiter=str(delimiter))
        next(readCSV)   #Skip the headers

        try:
            replyList=[]

            #Adding Headers to the result file
            replyList.append(['inputID','inputLabel','mappedId',"mappedLabel","Score"])
            counter=0
            #tmpReadCSV=readCSV
            #totalLength=len(list(tmpReadCSV))
            print "Enumerate over csv now"
            for index,row in enumerate(readCSV):
                print row
                potentialReply=[]
                #Execute label in the first row
                prefLabel=row[1].encode(encoding='UTF-8')

                if len(row)>2:
                    synList=row[2].split("|")
                else:
                    synList=[]

                #print prefLabel
                tmpReply=paxo_internals.scoreTermLabel(prefLabel, targetOntology, scoreParams, params)

                if tmpReply!=[]:
                    potentialReply.append(tmpReply)

                for syn in synList:
                    #print " --> "+syn
                    tmpReply=paxo_internals.scoreTermLabel(syn.strip().encode(encoding='UTF-8'), targetOntology, scoreParams, params)
                    if tmpReply!=[]:
                        potentialReply.append(tmpReply)

                #Sort all potential replies via the score, so the highest score is first
                try:
                    potentialReply=sorted(potentialReply, key=lambda potentialReply:potentialReply[0]['finaleScore'], reverse=True)[0]
                    if detailLevel<1:
                        replyList.append([row[0].encode(encoding='UTF-8'),prefLabel,potentialReply[0]['iri'].encode(encoding='UTF-8'),"", potentialReply[0]['fuzzyScore']])
                    elif detailLevel==1:
                        detail=[]
                        for tmpReply in potentialReply:
                            detail.append({"mappedIRI":tmpReply['iri'], "score":tmpReply['fuzzyScore']})
                        replyList.append([row[0].encode(encoding='UTF-8'),prefLabel,potentialReply[0]['iri'].encode(encoding='UTF-8'),"", potentialReply[0]['fuzzyScore'], detail])
                    elif detailLevel>1:
                        replyList.append([row[0].encode(encoding='UTF-8'),prefLabel,potentialReply[0]['iri'].encode(encoding='UTF-8'),"", potentialReply[0]['fuzzyScore'], potentialReply])
                except Exception as e:
                    #If the exception just arises because of and empty reply, we simply did not find a match and can move on
                    if potentialReply==[]:
                        replyList.append([row[0].encode(encoding='UTF-8'),prefLabel, "no match found", "", 0])
                    #Another error occured, this is something to look into!
                    else:
                        print e
                        print "Problem getting results for "+prefLabel+" - the reply was "+potentialReply
                        print potentialReply
                        raise

                #This is just to print feedback - if we work on a large list
                counter=counter+1
                if counter%20==0:
                    print "Processed "+str(counter)+" entries"

    ### annotating file
            print "Done processing input list, now annotate the result"
            olsurl=params['ols']+"search"
            for row in replyList[1:]:
                if row[2]=="no match found":
                    row[3]="no label found"
                else:
                    data={'q':row[2],'queryFields':'iri', 'fieldList': 'label', "ontology":targetOntology, "type":"class", "local":True}
                    r = requests.get(olsurl, data)
                    jsonReply=r.json()
                    try:
                        row[3]=jsonReply['response']['docs'][0]['label'].encode(encoding='UTF-8')
                    except Exception as e:
                        print "No label found"
                        row[3]="no label found"
                        print e

    ### write to file
            print "Done annotating file, now write result to file"
            #Writing result to output file
            with open(resultFile, 'wb') as f:
                writer = csv.writer(f)
                writer.writerows(replyList)
                f.close()

        #In case there is an error, print the exception
        except Exception as e:
            print "Error while processing file"
            print e
