package uk.ac.ebi.spot.model;

import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Mapping;

import java.util.Collection;

/**
 * @author Simon Jupp
 * @since 13/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class MappingSource {

    Datasource datasource;
    Collection<Mapping> mappings;

    public Datasource getDatasource() {
        return datasource;
    }

    public Collection<Mapping> getMappings() {
        return mappings;
    }

    public MappingSource(Datasource datasource, Collection<Mapping> mappings) {

        this.datasource = datasource;
        this.mappings = mappings;
    }
}
