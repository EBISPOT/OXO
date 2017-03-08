package uk.ac.ebi.spot.service;

import org.neo4j.cypher.CypherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.spot.exception.InvalidCurieException;
import uk.ac.ebi.spot.exception.UnknownDatasourceException;
import uk.ac.ebi.spot.index.Document;
import uk.ac.ebi.spot.index.DocumentRepository;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.repository.DatasourceRepository;
import uk.ac.ebi.spot.repository.TermGraphRepository;

import javax.xml.crypto.Data;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Simon Jupp
 * @date 12/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Service
public class DatasourceService {

    @Autowired
    private DatasourceRepository datasourceRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    private TermGraphRepository termGraphRepository;

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * This gets a datasource by prefix and also search alternate prefixes
     * @param prefix
     * @return
     */
    public Datasource getDatasource (String prefix) {
        return  datasourceRepository.findByPrefix(prefix.toLowerCase());
    }

    public Page<Datasource> getDatasources(Pageable pageable) {
        return datasourceRepository.findAll(pageable);
    }

    public List<Datasource> getDatasourceWithMappings() {
        return datasourceRepository.getDatasourcesWithMappings();
    }

    public List<Datasource> getMappingSources() {

        return datasourceRepository.getMappingDatasources();
    }

    @Transactional("transactionManager")
    public Datasource save (Datasource datasource) throws DuplicateKeyException {
        try {

            datasource.getAlternatePrefix().add(datasource.getPrefix());
            Set<String> lcStrings = datasource.getAlternatePrefix().stream().map(String::toLowerCase).collect(Collectors.toSet());
            datasource.getAlternatePrefix().addAll(lcStrings);
            return datasourceRepository.save(datasource);
        } catch (org.neo4j.ogm.exception.CypherException e) {
            if (e.getCode().contains("ConstraintValidationFailed")) {
                throw new DuplicateKeyException("Duplicate key exception, datasource already exists: " + datasource.getPrefix());
            }
            throw new RuntimeException("Error saving datasource", e);
        }
    }

    @Transactional("transactionManager")
    public Datasource update (Datasource datasource) throws InvalidCurieException, DuplicateKeyException {
        Datasource d = datasourceRepository.findByPrefix(datasource.getPrefix());

        // don't allow prefixes to be edited
        if (d == null) {
            throw new InvalidCurieException("You can't change an existing prefix for a datasource");
        }

        datasource.setId(d.getId());
        datasource.setPrefix(d.getPrefix());
        d = datasourceRepository.save(datasource);
        Collection<Term> terms = termGraphRepository.findByDatasource(d.getPrefix(), 0, Integer.MAX_VALUE);

        Collection<Document> documents = new HashSet<>();

        for (Term t : terms) {
            documentRepository.delete(t.getCurie());
            documents.add(DocumentBuilder.getDocumentFromTerm(t));
        }
        documentRepository.save(documents);

        return d;
    }




    public Datasource getOrCreateDatasource (Datasource datasource){
        Datasource existingSource = datasourceRepository.findByPrefix(datasource.getPrefix());

        if (existingSource == null) {
            return save(datasource);
        }
        return existingSource;

    }
}
