package uk.ac.ebi.spot.repository;

import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.spot.model.Mapping;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Repository
public interface MappingRepository  extends GraphRepository<Mapping> {
    Iterable<Mapping> findAllBySourcePrefix(String sourcePrefix);
}
