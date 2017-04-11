package uk.ac.ebi.spot.service;

import uk.ac.ebi.spot.model.*;

import java.util.Date;

/**
 * @author Simon Jupp
 * @since 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class MappingBuilder {

    private Term fromTerm;

    private Term toTerm;

    private Scope scope;
    private SourceType source;
    private Date date;

    private Datasource mappingSource;

    public MappingBuilder(

            Term fromTerm,
            Term toTerm,
            Datasource mappingSource) {
        this.date = new Date();
        this.scope = Scope.RELATED;
        this.source = SourceType.MANUAL;
        this.mappingSource = mappingSource;
        this.fromTerm = fromTerm;
        this.toTerm = toTerm;

    }

    public MappingBuilder setFromId(Term fromTerm) {
        this.fromTerm = fromTerm;
        return this;
    }


    public MappingBuilder setToId(Term toTerm) {
        this.toTerm = toTerm;
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

        mapping = new Mapping();


        mapping.setDatasource(this.mappingSource);
        mapping.setSourcePrefix(this.mappingSource.getPrefix());
        mapping.setFromTerm(fromTerm);
        mapping.setToTerm(toTerm);
        mapping.setDate(this.date);
        mapping.setScope(this.scope);
        mapping.setSourceType(this.source);


        return mapping;
    }



}
