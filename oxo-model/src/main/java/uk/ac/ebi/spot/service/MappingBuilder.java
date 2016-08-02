package uk.ac.ebi.spot.service;

import uk.ac.ebi.spot.model.*;
import uk.ac.ebi.spot.util.IdentifierType;
import uk.ac.ebi.spot.util.MappingType;

import java.util.Date;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class MappingBuilder {

    private String fromId;
    private IdentifierType fromType;
    private Datasource fromDatasource;

    private String toId;
    private IdentifierType toType;
    private Datasource toDatasource;

    private MappingType mappingType;
    private Scope scope;
    private SourceType source;
    private Date date;

    private Datasource mappingSource;

    public MappingBuilder(

            String fromId,
            IdentifierType fromType,
            Datasource fromDatasoure,
            String toId,
            IdentifierType toType,
            Datasource toDatasoure,
            MappingType mappingType,
            Datasource mappingSource) {
        this.date = new Date();
        this.scope = Scope.RELATED;
        this.source = SourceType.MANUAL;
        this.mappingType = mappingType;
        this.fromDatasource = fromDatasoure;
        this.toDatasource = toDatasoure;
        this.toDatasource = toDatasoure;
        this.mappingSource = mappingSource;
        this.fromId = fromId;
        this.fromType = fromType;
        this.toId = toId;
        this.toType = toType;

    }

    public MappingBuilder setFromId(String fromId) {
        this.fromId = fromId;
        return this;
    }

    public MappingBuilder setFromType(IdentifierType fromType) {
        this.fromType = fromType;
        return this;
    }

    public MappingBuilder setToId(String toId) {
        this.toId = toId;
        return this;
    }

    public MappingBuilder setToType(IdentifierType toType) {
        this.toType = toType;
        return this;
    }

    public MappingBuilder setMappingType(MappingType mappingType) {
        this.mappingType = mappingType;
        return this;
    }

    public MappingBuilder setScope(Scope scope) {
        this.scope = scope;
        return this;
    }

    public MappingBuilder setSource(SourceType source) {
        this.source = source;
        return this;
    }

    public MappingBuilder setDate(Date date) {
        this.date = date;
        return this;
    }

    public MappingBuilder setMappingSource(Datasource mappingSource) {
        this.mappingSource = mappingSource;
        return this;
    }

    public Mapping build() {

        Mapping mapping;

        Identifier fromId;

        if (this.fromType.equals(IdentifierType.URI)) {
            fromId = new URINode(this.fromId, this.fromDatasource);
        }
        else if (this.fromType.equals(IdentifierType.PREFIXED)) {
            fromId = new PrefixedCurie(this.fromId);
        }
        else {
            fromId = new CurieNode(this.fromId);
        }

        Identifier toId;

        if (this.toType.equals(IdentifierType.URI)){
            toId = new URINode(this.toId, this.fromDatasource);
        }
        else if (this.toType.equals(IdentifierType.PREFIXED)) {
            toId = new PrefixedCurie(this.toId);
        }
        else {
            toId = new CurieNode(this.toId);
        }

        if (mappingType.equals(MappingType.XREF)) {
            mapping = new XrefMapping();
            mapping.setScope(Scope.RELATED);
        }
        else {
            mapping = new AlternateIdMapping();
            mapping.setScope(Scope.EXACT);

            mapping.setScope(this.scope);
        }

        mapping.setDatasource(this.mappingSource);
        mapping.setSourcePrefix(this.mappingSource.getPrefix());
        mapping.setFromIdentifier(fromId);
        mapping.setToIdentifier(toId);
        mapping.setDate(this.date);


        return mapping;
    }



}
