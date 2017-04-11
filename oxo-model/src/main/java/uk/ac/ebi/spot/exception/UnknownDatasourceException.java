package uk.ac.ebi.spot.exception;

/**
 * @author Simon Jupp
 * @since 04/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class UnknownDatasourceException extends RuntimeException {

    public UnknownDatasourceException (String msg) {
        super(msg);
    }
}
