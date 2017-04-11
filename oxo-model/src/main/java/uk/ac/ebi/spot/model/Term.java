package uk.ac.ebi.spot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * @author Simon Jupp
 * @since 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@NodeEntity(label="Term")
public class Term implements Serializable {


    @GraphId
    @JsonIgnore
    private Long id;

    @Property(name="curie")
    private String curie;

    @Property(name="identifier")
    private String identifier;

    @Property(name="uri")
    private String uri;

    @Property(name="label")
    private String label;

    @Relationship(type="HAS_SOURCE", direction=Relationship.OUTGOING)
    private Datasource datasource;

    public Term() {

    }

    public Term(String curie, String identifier, String uri, String label, Datasource datasource) {
        this.curie = curie;
        this.identifier = identifier;
        this.uri = uri;
        this.label = label;
        this.datasource = datasource;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
    }

    public String getCurie() {
        return curie;
    }

    public void setCurie(String curie) {
        this.curie = curie;
    }

    public Long getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
