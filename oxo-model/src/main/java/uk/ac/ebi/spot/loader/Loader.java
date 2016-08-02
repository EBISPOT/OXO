package uk.ac.ebi.spot.loader;

import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.model.MappingSource;

import java.util.Collection;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Component
public interface Loader {

    Collection<MappingSource> load();

}
