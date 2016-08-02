package uk.ac.ebi.spot.model;

import org.neo4j.ogm.annotation.*;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.annotation.typeconversion.DateString;
import uk.ac.ebi.spot.util.DatasourceConverter;

import java.util.Date;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@RelationshipEntity(type = "MAPPING")
public class Mapping {

    @GraphId
    private Long mappingId;

    @Convert(DatasourceConverter.class)
    private Datasource datasource;

    @Property
    private String sourcePrefix;

    @StartNode
    private Identifier fromIdentifier;

    @EndNode
    private Identifier toIdentifier;

    @Property
    private Scope scope;

    @DateString("yy-MM-dd")
    private Date date;


    public Mapping() {
    }

    public Identifier getFromIdentifier() {
        return fromIdentifier;
    }

    public void setFromIdentifier(Identifier fromIdentifier) {
        this.fromIdentifier = fromIdentifier;
    }

    public Identifier getToIdentifier() {
        return toIdentifier;
    }

    public void setToIdentifier(Identifier toIdentifier) {
        this.toIdentifier = toIdentifier;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
    }

    public void setSourcePrefix(String sourcePrefix) {
        this.sourcePrefix = sourcePrefix;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public String getSourcePrefix() {
        return sourcePrefix;
    }

    public Scope getScope() {
        return scope;
    }

    public Date getDate() {
        return date;
    }
}
