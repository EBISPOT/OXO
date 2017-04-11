package uk.ac.ebi.spot.model;

import uk.ac.ebi.spot.util.MappingDistance;

/**
 * @author Simon Jupp
 * @since 14/06/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class MappingQuery {

    private String id;
    private String sourcePrefix = null;
    private int distance = MappingDistance.DEFAULT_MAPPING_DISTANCE;

    public MappingQuery() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourcePrefix() {
        return sourcePrefix;
    }

    public void setSourcePrefix(String sourcePrefix) {
        this.sourcePrefix = sourcePrefix;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }
}
