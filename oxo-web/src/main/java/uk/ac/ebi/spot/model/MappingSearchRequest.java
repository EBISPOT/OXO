package uk.ac.ebi.spot.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Simon Jupp
 * @date 30/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class MappingSearchRequest {

    private List<String> ids = new ArrayList<>();;
    private String inputSource;
    private Set<String> mappingTarget = new HashSet<>();
    private Set<String> mappingSource = new HashSet<>();
    private int distance = 3;

    public MappingSearchRequest(List<String> ids, Set<String> mappingSource, Set<String> mappingTarget) {
        this.ids = ids;
        this.mappingSource = mappingSource;
        this.mappingTarget = mappingTarget;
    }

    public MappingSearchRequest(List<String> ids, String inputSource, Set<String> mappingSource, Set<String> mappingTarget) {
        this.ids = ids;
        this.inputSource = inputSource;
        this.mappingSource = mappingSource;
        this.mappingTarget = mappingTarget;
    }

    public String getInputSource() {
        return inputSource;
    }

    public void setInputSource(String inputSource) {
        this.inputSource = inputSource;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public Set<String> getMappingSource() {
        return mappingSource;
    }

    public void setMappingSource(Set<String> mappingSource) {
        this.mappingSource = mappingSource;
    }

    public Set<String> getMappingTarget() {
        return mappingTarget;
    }

    public void setMappingTarget(Set<String> mappingTarget) {
        this.mappingTarget = mappingTarget;
    }

    public MappingSearchRequest() {

    }
}
