package uk.ac.ebi.spot.model;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@NodeEntity (label = "Datasource")
public class Datasource {

    @GraphId
    private Long id;

    @Property(name="prefix")
    private String prefix;

    @Property(name="name")
    private String name;

    @Property(name="description")
    private String description;

    @Property(name = "sourceType")
    private SourceType source;

    public Datasource(String prefix, String name, String description, SourceType source) {
        this.prefix = prefix;
        this.name = name;
        this.description = description;
        this.source = source;
    }

    public Datasource() {

    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SourceType getSource() {
        return source;
    }

    public void setSource(SourceType source) {
        this.source = source;
    }

    public Long getId() {
        return id;
    }
}
