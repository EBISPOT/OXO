package uk.ac.ebi.spot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Mapping;

import java.util.List;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Repository
@RepositoryRestResource(exported = false)
public interface MappingRepository  extends GraphRepository<Mapping> {
    Iterable<Mapping> findAllBySourcePrefix(String sourcePrefix);


    @Query("MATCH (td)-[m:MAPPING]-(ft)-[HAS_SOURCE]-(d:Datasource)\n" +
            "WHERE d.prefix = {0} or m.sourcePrefix = {0}\n" +
            "RETURN m")
    Page<Mapping> findAllByAnySource(String sourcePrefix, Pageable pageable);

    @Query("match (f:Term)-[r:MAPPING]-(t:Term) WHERE f.curie = {0} AND t.curie = {1} and r.sourcePrefix = {2} and r.scope = {3} return r")
    Mapping findOneByMappingBySourceAndId(String fromId, String toId, String source, String scope);




}
