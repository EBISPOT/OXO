package uk.ac.ebi.spot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Simon Jupp
 * @date 14/06/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan
public class OxoWebApp  extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(OxoWebApp.class, args);

    }
}
