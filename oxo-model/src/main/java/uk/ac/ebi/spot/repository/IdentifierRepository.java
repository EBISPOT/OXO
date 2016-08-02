package uk.ac.ebi.spot.repository;

import org.springframework.data.neo4j.repository.GraphRepository;
import uk.ac.ebi.spot.model.Identifier;

/**
 * @author Simon Jupp
 * @date 12/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public interface IdentifierRepository extends GraphRepository<Identifier> {
}
