package uk.ac.ebi.spot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.repository.DatasourceRepository;
import uk.ac.ebi.spot.repository.TermGraphRepository;

import java.io.Serializable;

/**
 * @author Simon Jupp
 * @since 05/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Component
public class CustomBackendIdConverter implements BackendIdConverter {

    @Autowired
    TermGraphRepository termGraphRepository;

    @Autowired
    DatasourceRepository datasourceRepository;


    @Override
    public Serializable fromRequestId(String id, Class<?> entityType) {

        if(entityType.equals(Term.class)) {
            Term term = termGraphRepository.findByCurie(id);
            if (term != null) {
                return term.getId();
            }
        }

        if(entityType.equals(Datasource.class)) {
            Datasource datasource = datasourceRepository.findByPrefix(id);
            if (datasource != null) {
                return datasource.getId();
            }
        }
        throw new IllegalArgumentException("Unrecognized class " + entityType);
    }

    @Override
    public String toRequestId(Serializable id, Class<?> entityType) {
        if(entityType.equals(Term.class)) {
            Term term = termGraphRepository.findOne((Long) id);
            if (term != null) {
                return term.getCurie();
            }
        }
        if(entityType.equals(Datasource.class)) {
            Datasource datasource = datasourceRepository.findOne((Long) id);
            if (datasource != null) {
                return datasource.getPrefix();
            }
        }
        throw new IllegalArgumentException("Unrecognized class " + entityType);
    }

    @Override
    public boolean supports(Class<?> delimiter) {
        return true;
    }
}