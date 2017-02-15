package uk.ac.ebi.spot.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Simon Jupp
 * @date 30/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public interface MappingQueryService {

    /**
     * Get a summary mapping results by searching mappings
     * @param id the id to search
     * @param distance the distance to search for mappings, default 1, use -1 for unlimited
     * @param sourcePrefix list of mapping source prefixes to filter on
     * @param targetPrefix list of mappingf target profiuces to filter
     * @return
     */
    SearchResult getMappingResponseSearchById (String id, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix);

    /**
     * Get a summary mapping results by searching mappings
     * @param fromDatasource the id to search
     * @param distance the distance to search for mappings, default 1, use -1 for unlimited
     * @param sourcePrefix list of mapping source prefixes to filter on
     * @param targetPrefix list of mappingf target profiuces to filter
     * @return
     */
    Page<SearchResult> getMappingResponseSearchByDatasource (String fromDatasource, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix, Pageable pageable);

    /**
     * Get a graph representation of the mappings for the existing id
     * @param curie
     * @return
     */
    Object getMappingSummaryGraph(String curie);

    /**
     * Get an Object that reflects a mapping summary
     */
    Object getMappingSummary();

    /**
     * Get an Object that reflects a mapping summary by datasource
     */
    Object getMappingSummary(String sourcePrefix);

    /**
     * Get a list of curies for a given target datasource and distance. This is an optimised convenience query used by the UI
     * @param fromDatasource the from datasource
     * @param targetDatasource the target datasource for mappings
     * @param distance defaults to 3
     * @return
     */
    List<String> getMappedTermCuries(String fromDatasource, String targetDatasource, int distance);

    /**
     * Get a list of target datasources and counts for a given input source and distance. This is an optimised convenience query used by the UI
     * @param fromDatasource the from datasoource
     * @param distance defaults to 3
     * @return
     */
    Map<String,Integer> getMappedTargetCounts(String fromDatasource, int distance);

}
