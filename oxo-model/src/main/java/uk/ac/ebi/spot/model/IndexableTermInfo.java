package uk.ac.ebi.spot.model;

import org.springframework.data.neo4j.annotation.QueryResult;

/**
 * @author Simon Jupp
 * @since 27/03/2017
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 *
 * This is a convenience object that is a stripped down version of a term, with only the information needed to create the Document for saving in the Document repository
 *
 */
@QueryResult
public class IndexableTermInfo {

    private String curie;
    private String id;
    private String uri;
    private String[] alternatePrefixes;

    public String getCurie() {
        return curie;
    }

    public void setCurie(String curie) {
        this.curie = curie;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String[] getAlternatePrefixes() {
        return alternatePrefixes;
    }

    public void setAlternatePrefixes(String[] alternatePrefixes) {
        this.alternatePrefixes = alternatePrefixes;
    }

    public IndexableTermInfo(String curie, String id, String uri, String[] alternatePrefixes) {

        this.curie = curie;
        this.id = id;
        this.uri = uri;
        this.alternatePrefixes = alternatePrefixes;
    }

    public IndexableTermInfo() {

    }
}
