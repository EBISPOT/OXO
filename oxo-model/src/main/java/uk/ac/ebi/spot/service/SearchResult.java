package uk.ac.ebi.spot.service;

import java.util.List;

/**
 * @author Simon Jupp
 * @date 05/09/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class SearchResult {

    String curie;
    String label;
    List<MappingResponse> mappingResponseList;

    public SearchResult() {
    }

    public SearchResult(String curie, String label, List<MappingResponse> mappingResponseList) {
        this.curie = curie;
        this.label = label;
        this.mappingResponseList = mappingResponseList;
    }

    public String getCurie() {
        return curie;
    }

    public void setCurie(String curie) {
        this.curie = curie;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<MappingResponse> getMappingResponseList() {
        return mappingResponseList;
    }

    public void setMappingResponseList(List<MappingResponse> mappingResponseList) {
        this.mappingResponseList = mappingResponseList;
    }
}
