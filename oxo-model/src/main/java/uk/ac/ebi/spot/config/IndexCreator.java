package uk.ac.ebi.spot.config;

import org.neo4j.ogm.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
//@Component
public class IndexCreator {

//    @Autowired
//    Neo4jOperations neo4jTemplate;
//
//    public IndexCreator() {
//    }
//
//    @PostConstruct
//    public void createIndexes() {
//        try {
//            Result result = neo4jTemplate.query("CREATE INDEX ON :Identifier(identifier)", null);
//            result.queryStatistics().toString();
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            // index already exists?
//        }
//
//        try {
//            neo4jTemplate.query("CREATE INDEX ON :Identifier(type)", null);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            // index already exists?
//        }
//
//        try {
//            neo4jTemplate.query("CREATE CONSTRAINT ON (i:Identifier) ASSERT i.identifier IS UNIQUE", null);
//        } catch (Exception ex) {
//            // index already exists?
//        }
//
//
//        try {
//            neo4jTemplate.query("CREATE INDEX ON :Mapping(scope)", null);
//        } catch (Exception ex) {
//            // index already exists?
//        }
//
//
//        try {
//            neo4jTemplate.query("CREATE INDEX ON :Mapping(type)", null);
//        } catch (Exception ex) {
//            // index already exists?
//        }
//
//
//        try {
//            neo4jTemplate.query("CREATE INDEX ON :Mapping(source)", null);
//        } catch (Exception ex) {
//            // index already exists?
//        }
//
//    }
}
