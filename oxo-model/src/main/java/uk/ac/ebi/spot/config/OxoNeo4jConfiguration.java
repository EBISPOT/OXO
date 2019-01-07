package uk.ac.ebi.spot.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Mapping;
import uk.ac.ebi.spot.model.Term;

import javax.annotation.Resource;
import java.net.MalformedURLException;

/**
 * @author Simon Jupp
 * @since 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Configuration
@EnableAsync
@EnableAutoConfiguration
@EnableNeo4jRepositories(basePackages = "uk.ac.ebi.spot.repository", basePackageClasses = {Datasource.class, Term.class, Mapping.class})
//@EnableSolrRepositories(basePackages = "uk.ac.ebi.spot.index")
@EnableTransactionManagement
public class OxoNeo4jConfiguration extends Neo4jConfiguration {

    @Value("${oxo.neo.driver:org.neo4j.ogm.drivers.http.driver.HttpDriver}")
    String driver;
    @Value("${oxo.neo.uri:http://localhost:7474}")
    String uri;
    @Bean
    public SessionFactory getSessionFactory() {
        // with domain entity base package(s)
        return new SessionFactory(getConfiguration(), "uk.ac.ebi.spot.model");
    }


    public org.neo4j.ogm.config.Configuration getConfiguration() {
        org.neo4j.ogm.config.Configuration config = new org.neo4j.ogm.config.Configuration();
       config
           .driverConfiguration()
           .setDriverClassName(driver)
           .setURI(uri);
       return config;
    }

    @Override
    public PlatformTransactionManager transactionManager() throws Exception {
        return super.transactionManager();
    }
}