package uk.ac.ebi.spot.model;

import org.neo4j.ogm.annotation.RelationshipEntity;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@RelationshipEntity(type = "XREF")
public class XrefMapping extends Mapping {

    public XrefMapping() {
    }
}
