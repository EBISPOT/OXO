package uk.ac.ebi.spot.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.Resource;
import java.net.MalformedURLException;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Configuration
@EnableAsync
@EnableAutoConfiguration
@EnableNeo4jRepositories(basePackages = "uk.ac.ebi.spot.repository")
//@EnableSolrRepositories(basePackages = "uk.ac.ebi.spot.index")
@EnableTransactionManagement
public class OxoNeo4jConfiguration extends Neo4jConfiguration {

//    private static final String PROPERTY_NAME_SOLR_SERVER_URL = "spring.data.solr.host";
//
//    @Resource
//    private Environment environment;
//
//    @Bean
//    public SolrClient solrClient() {
//      return new HttpSolrClient(environment.getRequiredProperty(PROPERTY_NAME_SOLR_SERVER_URL));
//    }
//
//    @Bean
//    public SolrTemplate solrTemplate() throws Exception {
//        SolrTemplate solrTemplate = new SolrTemplate(solrClient());
//        solrTemplate.setSolrCore("mapping");
//        return solrTemplate;
//    }

    @Bean
    public SessionFactory getSessionFactory() {
        // with domain entity base package(s)
        return new SessionFactory("uk.ac.ebi.spot.model");
    }


//    // needed for session in view in web-applications
//    @Bean
//    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
//    public Session getSession() throws Exception {
//        return super.getSession();
//    }

}