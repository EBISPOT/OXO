package uk.ac.ebi.spot.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.spot.model.Datasource;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Repository
@RepositoryRestResource(exported = false)
public interface DatasourceRepository  extends GraphRepository<Datasource> {

    @Query("match (n:Datasource) WHERE n.prefix = {0} OR {0} IN n.alternatePrefix return n")
    Datasource findByPrefix(String prefix);
}
