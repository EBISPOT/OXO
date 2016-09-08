package uk.ac.ebi.spot.service;

import java.util.Collection;

/**
 * @author Simon Jupp
 * @date 30/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class MappingResponse {

    private String curie;
    private String label;
    private Collection<String> sourcePrefixes;
    private Collection<String> targetPrefixes;
    private int distance;

    public MappingResponse() {
    }

    public MappingResponse(String curie, String label, Collection<String> sourcePrefixes, Collection<String> targetPrefixes, int distance) {
        this.curie = curie;
        this.label = label;
        this.sourcePrefixes = sourcePrefixes;
        this.targetPrefixes = targetPrefixes;
        this.distance = distance;
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

    public Collection<String> getSourcePrefixes() {
        return sourcePrefixes;
    }

    public void setSourcePrefixes(Collection<String> sourcePrefixes) {
        this.sourcePrefixes = sourcePrefixes;
    }

    public Collection<String> getTargetPrefixes() {
        return targetPrefixes;
    }

    public void setTargetPrefixes(Collection<String> targetPrefixes) {
        this.targetPrefixes = targetPrefixes;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }
}

