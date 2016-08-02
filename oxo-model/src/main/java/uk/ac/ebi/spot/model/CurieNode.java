package uk.ac.ebi.spot.model;

import org.neo4j.ogm.annotation.NodeEntity;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@NodeEntity( label = "Curie")
public class CurieNode extends Identifier {

    public CurieNode() {

    }
    public CurieNode(String fromId) {
        super(fromId);

    }
}
