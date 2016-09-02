package uk.ac.ebi.spot.model;

/**
 * @author Simon Jupp
 * @date 04/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class Curie {

    private String prefix;
    private String local;

    public String toString () {
        return prefix+":"+local;
    }
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public Curie(String prefix, String local) {

        this.prefix = prefix;
        this.local = local;
    }
}
