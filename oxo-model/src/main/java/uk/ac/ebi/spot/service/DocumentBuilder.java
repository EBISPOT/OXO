package uk.ac.ebi.spot.service;

import uk.ac.ebi.spot.index.Document;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.IndexableTermInfo;
import uk.ac.ebi.spot.model.Term;

import javax.xml.crypto.Data;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Simon Jupp
 * @since 04/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class DocumentBuilder {

    private static String COLON=":";

    static Document getDocumentFromTerm (Term term) {

        Datasource datasource = term.getDatasource();

        return getDocumentFromTerm(new IndexableTermInfo(term.getCurie(), term.getIdentifier(), term.getUri(), datasource.getAlternatePrefix().toArray(new String [datasource.getAlternatePrefix().size()])));

    }

    public static Document getDocumentFromTerm(IndexableTermInfo term) {
        Set<String> identifiers = new HashSet<>();

        identifiers.add(term.getCurie());
        identifiers.add(term.getCurie().replaceAll(":", "_"));
        identifiers.add(term.getId());
        if (term.getUri() != null) {
            identifiers.add(term.getUri());
        }
        for (String s: term.getAlternatePrefixes()) {
            identifiers.add(s+COLON+term.getId());
        }
        return new Document(term.getCurie(), identifiers);
    }
}
