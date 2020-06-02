package uk.ac.ebi.spot.config;

import org.neo4j.ogm.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;

/**
 * @author Simon Jupp
 * @since 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Component
public class IndexCreator implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    Neo4jOperations neo4jTemplate;

    /**
   * This event is executed as late as conceivably possible to indicate that
   * the application is ready to service requests.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {

//        try {
//            Result result = neo4jTemplate.query("CREATE INDEX ON :Term(curie)", new HashMap());
//            result.queryStatistics().toString();
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            // index already exists?
//        }
//
//        try {
//            neo4jTemplate.query("CREATE INDEX ON :Datasource(prefix)", new HashMap());
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            // index already exists?
//        }

        try {
            neo4jTemplate.query("CREATE CONSTRAINT ON (i:Term) ASSERT i.curie IS UNIQUE", new HashMap());
        } catch (Exception ex) {
            ex.printStackTrace();
            // index already exists?
        }

      try {
          neo4jTemplate.query("CREATE CONSTRAINT ON (i:Datasource) ASSERT i.prefix IS UNIQUE", new HashMap());
      } catch (Exception ex) {
          ex.printStackTrace();
          // index already exists?
      }


    return;
  }
}
