package uk.ac.ebi.spot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import uk.ac.ebi.spot.model.IndexableTermInfo;
import uk.ac.ebi.spot.model.Term;

import java.util.Collection;

/**
 * @author Simon Jupp
 * @since 12/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@RepositoryRestResource(exported = false)
public interface TermGraphRepository extends GraphRepository<Term> {

    Term findByCurie(String curie);

    @Query(value = "MATCH (n:Term)-[HAS_SOURCE]->(d:Datasource) WHERE d.prefix = {0} RETURN n SKIP {1} LIMIT {2}")
    Collection<Term> findByDatasource(String prefix, long skip, long limit);

    @Query(value = "MATCH (n:Term)-[HAS_SOURCE]->(d:Datasource) WHERE d.prefix = {0} RETURN count(n)")
    int getTermCountBySource(String prefix);

    @Query(value = "MATCH (n:Term)-[HAS_SOURCE]->(d:Datasource) RETURN count(DISTINCT n)")
    int getIndexableTermCount();

    @Query(value = "Match (t:Term)-[:HAS_SOURCE]->(d:Datasource) RETURN DISTINCT t.curie as curie, t.id as id, t.uri as uri, d.alternatePrefix as alternatePrefixes")
    Iterable<IndexableTermInfo> getAllIndexableTerms();

    @Query(value = "Match (t:Term)-[:HAS_SOURCE]->(d:Datasource) RETURN DISTINCT t.curie as curie, t.id as id, t.uri as uri, d.alternatePrefix as alternatePrefixes ORDER BY curie SKIP {0} LIMIT {1}")
    Iterable<IndexableTermInfo> getAllIndexableTerms(long skip, long limit);
}
