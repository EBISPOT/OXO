package uk.ac.ebi.spot.exception;

/**
 * @author Simon Jupp
 * @date 04/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class TermCreationException extends RuntimeException {
    public TermCreationException(String s) {
        super(s);
    }
}
