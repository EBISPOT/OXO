package uk.ac.ebi.spot.exception;

/**
 * @author Simon Jupp
 * @since 09/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class UnknownTermException extends RuntimeException {

    public UnknownTermException(String msg) {
        super(msg);
    }
}
