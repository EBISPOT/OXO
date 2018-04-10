#!/usr/bin/env python
"""
Script for loading large amount sof data in OxO formatted CSV files into a OxO Neo4j.
You can delete the neo4j database, load datasoruces, terms and mapping files with this script
"""
__author__ = "jupp"
__license__ = "Apache 2.0"
__date__ = "03/03/2018"

from neo4j.v1 import GraphDatabase, basic_auth
from ConfigParser import SafeConfigParser
from optparse import OptionParser

class Neo4jOxOLoader:
    def __init__(self):
        # if len(sys.argv) != 2:
        #     print "\nNot enough arguments! Please pass a (path) of a config file!"
        #     raise Exception("Not enough arguments! Please pass in a config file!")
        # else:
        #     config = SafeConfigParser()
        #     config.read(sys.argv[1])

        parser = OptionParser()
        parser.add_option("-d","--datasources",  help="load the datasource file")
        parser.add_option("-t","--terms", help="load the term file")
        parser.add_option("-m","--mappings", help="load the mapping file")
        parser.add_option("-W","--wipe", action="store_true", dest="wipe", help="wipe the neo4j database")
        parser.add_option("-c","--config", help="config file", default="config.ini")

        (options, args) = parser.parse_args()

        config = SafeConfigParser()
        config.read(options.config)

        uri = config.get("Basics", "neoURL")
        neoUser = config.get("Basics", "neoUser")
        neoPass = config.get("Basics", "neoPass")


        driver = GraphDatabase.driver(uri, auth=basic_auth(neoUser, neoPass))
        self.session = driver.session()

        self.session.run("CREATE CONSTRAINT ON (i:Term) ASSERT i.curie IS UNIQUE")
        self.session.run("CREATE CONSTRAINT ON (i:Datasource) ASSERT i.prefix IS UNIQUE")

        if options.wipe:
            while self.deleteMappings() > 0:
                print "Still deleting..."
            print "Mappings deleted!"

            while self.deleteSourceRels() > 0:
                print "Still deleting..."
            print "Source rels deleted!"

            while self.deleteTerms() > 0:
                print "Still deleting..."
            print "Terms deleted!"

            while self.deleteDatasources() > 0:
                print "Still deleting..."
            print "Datasources deleted!"

        if options.datasources:
            self.loadDatasources(options.datasources)
        if options.terms:
            self.loadTerms(options.terms)
        if options.mappings:
            self.loadMappings(options.mappings)


    def deleteMappings(self):
        result = self.session.run("match (t)-[m:MAPPING]->() WITH m LIMIT 50000 DETACH DELETE m RETURN count(*) as count")
        for record in result:
            return record["count"]

    def deleteSourceRels(self):
        result = self.session.run("match (t)-[m:HAS_SOURCE]->()  WITH m LIMIT 50000 DETACH DELETE m RETURN count(*) as count")
        for record in result:
            return record["count"]

    def deleteTerms(self):
        result = self.session.run("match (t:Term) WITH t LIMIT 50000 DETACH DELETE t RETURN count(*) as count")
        for record in result:
            return record["count"]

    def deleteDatasources(self):
        result = self.session.run("match (d:Datasource) WITH d LIMIT 1000 DETACH DELETE d RETURN count(*) as count")
        for record in result:
            return record["count"]

    def loadTerms(self, terms):
        print "Loading terms from "+terms+"..."

        loadTermsCypher = "USING PERIODIC COMMIT 10000 LOAD CSV WITH HEADERS FROM 'file:///"+terms+"""' AS line
                        MATCH (d:Datasource {prefix : line.prefix})
                        WITH d, line
                        MERGE (t:Term { curie: line.curie})
                        SET t.id = line.identifier, t.label = line.label, t.uri = line.uri
                        with t,d
                        CREATE (t)-[:HAS_SOURCE]->(d)"""
        result = self.session.run(loadTermsCypher)
        print result.summary()

    def loadMappings(self, mappings):
        print "Loading mappings from "+mappings+"..."
        loadMappingsCypher = "USING PERIODIC COMMIT 10000 LOAD CSV WITH HEADERS FROM 'file:///"+mappings+"""' AS line
                        MATCH (f:Term { curie: line.fromCurie}),(t:Term { curie: line.toCurie})
                        WITH f,t,line
                        CREATE (f)-[m:MAPPING { sourcePrefix: line.datasourcePrefix, datasource: line.datasource, sourceType: line.sourceType, scope: line.scope, date: line.date}]->(t)"""

        result = self.session.run(loadMappingsCypher)
        print result.summary()

    def loadDatasources(self, datasources):
        print "Loading datasrouces from " + datasources + " ..."
        loadDatasourcesCypher = """
            LOAD CSV WITH HEADERS FROM 'file:///"""+datasources+"""' AS line
            WITH line
            MERGE (d:Datasource {prefix : line.prefix})
            WITH d, line
            SET d.preferredPrefix = line.prefix, d.name = line.title, d.description = line.description, d.versionInfo = line.versionInfo, d.idorgNamespace = line.idorgNamespace, d.licence = line.licence, d.sourceType = line.sourceType, d.alternatePrefix = split(line.alternatePrefixes,",")
            """
        result = self.session.run(loadDatasourcesCypher)
        print result.summary()

if __name__ == '__main__':
    Neo4jOxOLoader()