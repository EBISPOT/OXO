package uk.ac.ebi.spot.index;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.SolrDocument;

import java.util.Collection;

/**
 * @author Simon Jupp
 * @date 04/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@SolrDocument(solrCoreName = "mapping")
public class Document {

    @Id
    private String id;

    @Field("identifier")
    private Collection<String> identifier;

    public Collection<String> getKnownIdentifiers() {
        return identifier;
    }

    public void setKnownIdentifiers(Collection<String> knownIdentifiers) {
        this.identifier = knownIdentifiers;
    }

    public Document(String curie, Collection<String> knownIdentifiers) {
        this.id = curie;
        this.identifier = knownIdentifiers;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCurie() {
        return id;
    }

    public void setCurie(String curie) {
        this.id = curie;
    }

    public Document() {

    }
}
