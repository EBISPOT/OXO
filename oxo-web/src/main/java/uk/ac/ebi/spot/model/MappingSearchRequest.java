package uk.ac.ebi.spot.model;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Simon Jupp
 * @date 30/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class MappingSearchRequest {

    private String identifiers;
    private Set<String> inputSource = new HashSet<>();
    private Set<String> mappingTarget = new HashSet<>();
    private Set<String> mappingSource = new HashSet<>();
    private int distance = 3;

    public MappingSearchRequest(String identifiers, Set<String> mappingSource, Set<String> mappingTarget) {
        this.identifiers = identifiers;
        this.mappingSource = mappingSource;
        this.mappingTarget = mappingTarget;
    }

    public MappingSearchRequest(String identifiers, Set<String> inputSource, Set<String> mappingSource, Set<String> mappingTarget) {
        this.identifiers = identifiers;
        this.inputSource = inputSource;
        this.mappingSource = mappingSource;
        this.mappingTarget = mappingTarget;
    }

    public Set<String> getInputSource() {
        return inputSource;
    }

    public void setInputSource(Set<String> inputSource) {
        this.inputSource = inputSource;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public String getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(String identifiers) {
        this.identifiers = identifiers;
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
