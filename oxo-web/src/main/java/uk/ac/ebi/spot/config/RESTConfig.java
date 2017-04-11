package uk.ac.ebi.spot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.http.MediaType;
import uk.ac.ebi.spot.security.service.*;

/**
 * @author Simon Jupp
 * @since 14/06/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Configuration
public class RESTConfig extends RepositoryRestConfigurerAdapter {


    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
        config.getMetadataConfiguration().setAlpsEnabled(false);
        config.setBasePath("/api");
    }

}