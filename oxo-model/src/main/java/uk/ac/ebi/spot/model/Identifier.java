package uk.ac.ebi.spot.model;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import uk.ac.ebi.spot.util.IdentifierType;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@NodeEntity(label="Identifier")
public class Identifier {


    @GraphId
    private Long id;

    @Property(name="identifier")
    private String identifier;


    public Identifier() {

    }

    public Identifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
