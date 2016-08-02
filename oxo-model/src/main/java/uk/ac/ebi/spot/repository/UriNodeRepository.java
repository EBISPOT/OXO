package uk.ac.ebi.spot.repository;

import org.springframework.data.neo4j.repository.GraphRepository;
import uk.ac.ebi.spot.model.URINode;

/**
 * @author Simon Jupp
 * @date 12/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public interface UriNodeRepository extends GraphRepository<URINode> {

    URINode findByIdentifier(String identifier);

}
