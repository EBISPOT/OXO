package uk.ac.ebi.spot.model;

import org.neo4j.ogm.annotation.*;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.annotation.typeconversion.DateString;
import uk.ac.ebi.spot.util.DatasourceConverter;

import java.util.Date;

/**
 * @author Simon Jupp
 * @since 11/05/2016
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

    @Property
    private SourceType sourceType = SourceType.MANUAL;

    @Property
    private SourceType predicate;

    @StartNode
    private Term fromTerm;

    @EndNode
    private Term toTerm;

    @Property
    private Scope scope = Scope.RELATED;

    @DateString("yy-MM-dd")
    private Date date;

    public Mapping() {
    }

    public Long getMappingId() {
        return mappingId;
    }

    public void setMappingId(Long mappingId) {
        this.mappingId = mappingId;
    }

    public Term getFromTerm() {
            return fromTerm;
        }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public void setFromTerm(Term fromTerm) {
        this.fromTerm = fromTerm;
    }

    public Term getToTerm() {
        return toTerm;
    }

    public void setToTerm(Term toTerm) {
        this.toTerm = toTerm;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public SourceType getPredicate() {
        return predicate;
    }

    public void setPredicate(SourceType predicate) {
        this.predicate = predicate;
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
