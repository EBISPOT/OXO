package uk.ac.ebi.spot.util;

import uk.ac.ebi.spot.exception.InvalidCurieException;
import uk.ac.ebi.spot.model.Curie;

import java.util.Optional;


/**
 * @author Simon Jupp
 * @date 04/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class CurieUtils {

    public static String getPrefixFromCurie (String curie) throws InvalidCurieException{

        if (curie.split(":").length == 2) {
            return curie.split(":")[0];
        }
        else {
            throw new InvalidCurieException("Id is not a valid curie, should be QNAME:<element name>");
        }
    }

    public static String getLocalFromCurie (String curie) throws InvalidCurieException{

            if (curie.split(":").length == 2) {
                return curie.split(":")[1];
            }
            else {
                throw new InvalidCurieException("Id is not a valid curie, should be QNAME:<element name>");
            }
     }

    public static Curie getCurie (String prefix, String local){
            return new Curie(prefix, local);
     }


}
