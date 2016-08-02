package uk.ac.ebi.spot.model;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@NodeEntity(label = "Uri")
public class URINode extends Identifier {

    @Relationship(type = "DATASOURCE", direction = Relationship.OUTGOING)
    private Datasource datasource;

    public URINode() {
    }

    // todo add is obsollete
    public URINode(String identifier, Datasource datasource) {
        super(identifier);
        this.datasource = datasource;
    }

    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
    }

    public Datasource getDatasource() {

        return datasource;
    }
}
