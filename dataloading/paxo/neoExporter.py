import csv
import logging
import flaskMapping
import time
from datetime import datetime

from neo4j.v1 import GraphDatabase, basic_auth


#Load to neo
#uri = "bolt://localhost:7687"

#uri = "bolt://localhost:7687"
#encrypted=False
#driver = GraphDatabase.driver(uri, auth=basic_auth("neo4j", "dba"))
#driver = GraphDatabase.driver(uri, auth=basic_auth(user, password))
#session = driver.session()
#print "Loading terms.csv..."
#loadTermsCypher = """USING PERIODIC COMMIT 10000
#                LOAD CSV WITH HEADERS FROM 'file:///Users/tliener/onto_map/oxo/oxo-loading/testspace/terms.csv' AS line
#                MATCH (d:Datasource {prefix : line.prefix})
#                WITH d, line
#                MERGE (t:Term { id: line.identifier, curie: line.curie, label: line.label, uri: line.uri})
#                with t,d
#                CREATE (t)-[:HAS_SOURCE]->(d)"""
#result = session.run(loadTermsCypher)
#print result.summary()
#print "Loading mappings.csv..."
#loadMappingsCypher = """USING PERIODIC COMMIT 10000
#                    LOAD CSV WITH HEADERS FROM 'file:///Users/tliener/onto_map/oxo/oxo-loading/test/spacemappings.csv' AS line
#                    MATCH (f:Term { curie: line.fromCurie}),(t:Term { curie: line.toCurie})
#                    WITH f,t,line
#                    CREATE (f)-[m:MAPPING { sourcePrefix: line.datasourcePrefix, datasource: line.datasource, sourceType: line.sourceType, scope: line.scope, date: line.date}]->(t)"""
#result = session.run(loadMappingsCypher)
#print result.summary()

#Global:
date=datetime.now().strftime('%Y-%m-%d')

def writeTermsToNeo(termsFile, session):
    loadMappingsCypher = "USING PERIODIC COMMIT 10000 LOAD CSV WITH HEADERS FROM 'file:///"+termsFile+"""' AS line
                MATCH (d:Datasource {prefix : line.prefix})
                WITH d, line
                MERGE (t:Term { id: line.identifier, curie: line.curie, label: line.label, uri: line.uri})
                with t,d
                CREATE (t)-[:HAS_SOURCE]->(d)"""

    print "Try to load terms from "+termsFile+" to database"
    print loadMappingsCypher
    result = session.run(loadMappingsCypher)
    print result.summary()

def writeMappingsToNeo(mappingsFile, session):
    loadMappingsCypher = "USING PERIODIC COMMIT 10000 LOAD CSV WITH HEADERS FROM 'file:///"+mappingsFile+"""' AS line
                    MATCH (f:Term { curie: line.fromCurie}),(t:Term { curie: line.toCurie})
                    WITH f,t,line
                    CREATE (f)-[m:MAPPING { sourcePrefix: line.datasourcePrefix, datasource: line.datasource, sourceType: line.sourceType, scope: line.scope, date: line.date}]->(t)"""

    print "Try to load mappings from "+mappingsFile+" to database"
    print loadMappingsCypher
    result = session.run(loadMappingsCypher)
    print result.summary()

def createNode(iri, ontology, olsURL):
    data={"q": iri, "ontology":ontology, "exact":True,  "type":"class", "local":True, "fieldList":"label,ontology_prefix,obo_id"}
    jsonReply=flaskMapping.apiCall(olsURL+"search", data)

    try:
        jsonReply=jsonReply.json()
    except Exception as e:
        print "Error with decoding json reply from OLS API"
        print data
        print jsonReply
        print e

    if len(jsonReply['response']['docs'])>0:
        line=[]
        label=jsonReply['response']['docs'][0]['label'].encode('utf-8').strip()
        ontology_prefix=jsonReply['response']['docs'][0]['ontology_prefix']

        #Hack for Orphanet
        if ontology_prefix=='ORDO':
            ontology_prefix='Orphanet'

        try:
            obo_id=jsonReply['response']['docs'][0]['obo_id']
        except:
            print "Try to replace the obo_id with short form!"
            try:
                #Add Ontology prefix before the short form (e.g. for MESH)

                ontology_prefix=jsonReply['response']['docs'][0]['ontology_prefix']
                #Hack for Orphanet
                if ontology_prefix=='ORDO':
                    ontology_prefix='Orphanet'

                obo_id=ontology_prefix+":"+jsonReply['response']['docs'][0]['short_form']
            except:
                print "Did not work to retrieve the obo_id nor the short_form from OLS. So I use UNKOWN:UNKNOWN instead"
                print data
                obo_id='UNKNOWN:UNKNOWN'


        identifier=obo_id.split(':')[1]



        #if we couldn't retrieve doc from OLS we return empty line so it's not added to the csv
        if obo_id=='UNKNOWN:UNKNOWN':
            return []
        else:
            line=list([identifier, obo_id, label, iri,ontology_prefix])
            return line
    else:
        print "Did not get a doc form OLS for"
        print data
        print "Therefor this node/mapping is not included in the export!"
        return []

def createMap(curie1, curie2, score):
    line=list([curie1, curie2, 'paxo', 'oxo', 'ALGORITHM', 'PREDICTED', date, score])
    return line

def exportInNeo(onto1, onto2, predictedFolder, targetFolder, olsURL, neoURL, neoUser, neoPW):
    predictedFile=predictedFolder+'calculated_output_'+onto1+"_"+onto2+".csv"

    uri=neoURL
    encrypted=False
    driver = GraphDatabase.driver(uri, auth=basic_auth(neoUser, neoPW))
    session = driver.session()


    paxo_term=[]
    paxo_mappings=[]
    line=list(['identifier','curie', 'label', 'uri', 'prefix'])
    paxo_term.append(line)
    line=list(['fromCurie','toCurie', 'datasourcePrefix', 'datasource', 'sourceType', 'scope', 'date', 'score'])
    paxo_mappings.append(line)

    print "Read in predicte mappings from "+predictedFile
    with open(predictedFile) as csvfile:
        readCSV = csv.reader(csvfile, delimiter=str(','))
        next(readCSV)
        counter=0
        for row in readCSV:
            firstRow=createNode(row[0], onto1, olsURL)
            secondRow=createNode(row[1], onto2, olsURL)

            if firstRow!=[]:
                paxo_term.append(firstRow)
            if secondRow!=[]:
                paxo_term.append(secondRow)

            if firstRow!=[] and secondRow!=[]:
                paxo_mappings.append(createMap(firstRow[1],secondRow[1], row[2]))

            #This is just for Testing, don't take more than 10
            #counter=counter+1
            #if counter>10:
            #    break

        #print paxo_term
        with open(targetFolder+onto1+"_"+onto2+'_termsNeo.csv', 'wb') as f:
            writer = csv.writer(f)
            writer.writerows(paxo_term)
            f.close()

        with open(targetFolder+onto1+"_"+onto2+'_mappingsNeo.csv', 'wb') as f2:
            writer = csv.writer(f2)
            writer.writerows(paxo_mappings)
            f.close()

        writeTermsToNeo(targetFolder+onto1+"_"+onto2+'_termsNeo.csv', session)
        writeMappingsToNeo(targetFolder+onto1+"_"+onto2+'_mappingsNeo.csv', session)


        #After Loading, update solr indexes (Might be done outside of this script so commented out for now)
        #print "updating indexes"
        #reply = urllib.urlopen(oxoUrl+"/api/search/rebuild?apikey="+apikey)
        #print "Finished process!"
