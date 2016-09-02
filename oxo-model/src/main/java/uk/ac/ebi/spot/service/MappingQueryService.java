package uk.ac.ebi.spot.service;

import java.util.Collection;
import java.util.List;

/**
 * @author Simon Jupp
 * @date 30/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public interface MappingQueryService {

    /**
     * Get an Object that reflects a mapping summary
     */
    Object getMappingSummary();


    /**
     * Get a summary mapping results by searching mappings
     * @param id the id to search
     * @param distance the distance to search for mappings, default 1, use -1 for unlimited
     * @param sourcePrefix list of mapping source prefixes to filter on
     * @param targetPrefix list of mappingf target profiuces to filter
     * @return
     */
    List<MappingResponse> getMappingResponseSearch (String id, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix);

    /**
     * Get a graph representation of the mappings for the existing id
     * @param curie
     * @return
     */
    Object getMappingSummaryGraph(String curie);
}
