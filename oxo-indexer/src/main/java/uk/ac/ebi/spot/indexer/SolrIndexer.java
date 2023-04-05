package uk.ac.ebi.spot.indexer;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;
import uk.ac.ebi.spot.index.Document;
import uk.ac.ebi.spot.service.TermService;

import javax.annotation.Resource;

/**
 * @author Simon Jupp
 * @date 04/03/2018
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */

@SpringBootApplication
@EnableNeo4jRepositories(basePackages = "uk.ac.ebi.spot.repository")
@EnableSolrRepositories(basePackages = "uk.ac.ebi.spot.index", basePackageClasses = {Document.class})
@EnableConfigurationProperties
@ComponentScan({"uk.ac.ebi"})
public class SolrIndexer implements CommandLineRunner {

    @Autowired
    TermService termService;

    @Resource
    private Environment environment;

    @Bean
    SolrClient solrClient() {
        return new HttpSolrClient(environment.getProperty("spring.data.solr.host"));
    }
    @Bean
    public SolrTemplate solrTemplate() {
        return new SolrTemplate(solrClient(), "mapping");
    }
    
    @Override
    public void run(String... strings) throws Exception {
        termService.rebuildIndexes();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(SolrIndexer.class, args);
    }
}
