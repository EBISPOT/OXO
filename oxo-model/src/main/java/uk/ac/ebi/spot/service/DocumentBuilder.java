package uk.ac.ebi.spot.service;

import uk.ac.ebi.spot.index.Document;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Term;

import javax.xml.crypto.Data;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Simon Jupp
 * @date 04/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class DocumentBuilder {

    private static String COLON=":";

    static Document getDocumentFromTerm (Term term) {

        Datasource datasource = term.getDatasource();

        Set<String> identifiers = new HashSet<>();

        identifiers.add(term.getCurie());
        identifiers.add(term.getCurie().replaceAll(":", "_"));
        identifiers.add(term.getIdentifier());
        if (term.getUri() != null) {
            identifiers.add(term.getUri());
        }
        for (String s: datasource.getAlternatePrefix()) {
            identifiers.add(s+COLON+term.getIdentifier());
        }

        for (String s: datasource.getAlternateIris()) {
            identifiers.add(s+term.getIdentifier());
        }

//        identifiers.add("http://identifiers.org/"+datasource.getIdorgNamespace()+"/"+term.getCurie());

        return new Document(term.getCurie(), identifiers);

    }

}
