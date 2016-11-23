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
    List<Mapping> findAllByAnySource(String sourcePrefix);

    @Query("MATCH (td)-[m:MAPPING]-(ft)-[HAS_SOURCE]-(d:Datasource)\n" +
            "WHERE d.prefix = {0} or m.sourcePrefix = {0}\n" +
            "RETURN m SKIP {1} LIMIT {2}")
    List<Mapping> findAllByAnySource(String sourcePrefix, long skip, long limit);

    @Query("match (f:Term)-[r:MAPPING]-(t:Term) WHERE f.curie = {0} AND t.curie = {1} and r.sourcePrefix = {2} and r.scope = {3} return r")
    Mapping findOneByMappingBySourceAndId(String fromId, String toId, String source, String scope);

    @Query("match (f:Term)-[r:MAPPING]-(t:Term) WHERE f.curie = {0} AND t.curie = {1} return r")
    List<Mapping> findMappingsById(String fromId, String toId);

    @Query("match (f:Term)-[r:MAPPING]-(t:Term) WHERE f.curie = {0} AND t.curie = {1} return r SKIP {2} LIMIT {3}")
    List<Mapping> findMappingsById(String fromId, String toId, long skip, long limit);

    @Query("match (f:Term)-[r:MAPPING]-(t:Term) WHERE f.curie = {0} return r")
    List<Mapping> findMappingsById(String fromId);

    @Query("match (f:Term)-[r:MAPPING]-(t:Term) WHERE f.curie = {0} return r SKIP {1} LIMIT {2}")
    List<Mapping> findMappingsById(String fromId, long skip, long limit);


    @Query("match path = allShortestPaths ( (f:Term)-[r:MAPPING*1..10]-(t:Term) ) WHERE f.curie = {0} AND t.curie = {1} with rels(path) as m, length(path) as l WHERE l> 1 return m")
    List<Mapping> findInferredMappingsById(String fromId, String toId);


    @Query("MATCH (td)-[m:MAPPING]-()\n" +
            "WHERE m.sourcePrefix = {0}\n" +
            "RETURN count(m)")
    int getMappingsCountBySource(String prefix);

}
