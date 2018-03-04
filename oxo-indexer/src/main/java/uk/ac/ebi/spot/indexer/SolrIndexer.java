package uk.ac.ebi.spot.indexer;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;
import org.springframework.data.solr.server.support.EmbeddedSolrServerFactoryBean;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.index.Document;
import uk.ac.ebi.spot.index.DocumentRepository;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.service.DatasourceService;
import uk.ac.ebi.spot.service.TermService;

import javax.annotation.Resource;
import java.net.MalformedURLException;

/**
 * @author Simon Jupp
 * @date 04/03/2018
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */

@SpringBootApplication
@EnableNeo4jRepositories(basePackages = "uk.ac.ebi.spot.repository")
@EnableSolrRepositories(basePackages = "uk.ac.ebi.spot.index", basePackageClasses = {Document.class})
@EnableAutoConfiguration
@EnableConfigurationProperties
@ComponentScan({"uk.ac.ebi" })
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
