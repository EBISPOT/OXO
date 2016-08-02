package uk.ac.ebi.spot.model;

import org.neo4j.ogm.annotation.NodeEntity;

/**
 * @author Simon Jupp
 * @date 12/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@NodeEntity( label = "PrefixedCurie")
public class PrefixedCurie extends Identifier {

    public PrefixedCurie() {

    }
    public PrefixedCurie(String fromId) {
        super(fromId);
    }
}
