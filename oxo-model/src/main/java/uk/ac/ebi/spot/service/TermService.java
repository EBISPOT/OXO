package uk.ac.ebi.spot.service;

import org.neo4j.ogm.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.spot.exception.InvalidCurieException;
import uk.ac.ebi.spot.exception.TermCreationException;
import uk.ac.ebi.spot.exception.UnknownDatasourceException;
import uk.ac.ebi.spot.exception.UnknownTermException;
import uk.ac.ebi.spot.index.Document;
import uk.ac.ebi.spot.index.DocumentRepository;
import uk.ac.ebi.spot.model.*;
import uk.ac.ebi.spot.repository.TermGraphRepository;
import uk.ac.ebi.spot.util.CurieUtils;

import java.util.*;

/**
 * @author Simon Jupp
 * @since 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Service
public class TermService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    DatasourceService datasourceService;

    @Autowired
    TermGraphRepository termGraphRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    MappingQueryService mappingQueryService;

    @Autowired
    MappingQueryService cypherQueryService;

    @Autowired
    Session session;
    private Object summaryGraphJson;

    @Value("${oxo.indexer.chunks}")
    int chunks = 10000;

    /**
     *
     * @param curie
     * @return Term or null
     */
    public Term getTerm(String curie) {
        Document document = documentRepository.findOneByIdentifier(curie);
        if (document == null) {
            return termGraphRepository.findByCurie(curie);
        }
        return termGraphRepository.findByCurie(document.getId());
    }
              git
    /**
     * This method saves a term. The term must have a valid datasource that we know about. If the datasource is valid, but the prefix
     * isn't we will change the prefix to match the datasource.
     * @param term
     * @return
     * @throws UnknownDatasourceException
     */
    @Transactional
    public Term save (Term term) throws UnknownDatasourceException, InvalidCurieException {

        Document document = documentRepository.findOneByIdentifier(term.getCurie());
        if (document != null) {
            // we may have seen this before, see if stored in main repo
            if (termGraphRepository.findByCurie(document.getCurie()) != null) {
                throw new DuplicateKeyException("Resource with id "+ document.getId() + " already exists");
            }
        }

        String prefix = CurieUtils.getPrefixFromCurie(term.getCurie());
        String local = CurieUtils.getLocalFromCurie(term.getCurie());

        if (term.getIdentifier() == null) {
            term.setIdentifier(local);
        }

        if (!term.getIdentifier().equals(local)) {
            throw new TermCreationException("Curie local " + local + " does not match supplied id " + term.getIdentifier());
        }

        Datasource datasource = term.getDatasource();
        if (datasource == null) {
            datasource = datasourceService.getDatasource(prefix);
        } else {
            datasource = datasourceService.getDatasource(datasource.getPrefix());
        }

        // if still can't determine datsource, then throw exception
        if (datasource == null) {
            log.error("Unknown datasource " + term.getCurie());
            throw new UnknownDatasourceException("Can't determine datasource for " + term.getCurie());
        }

        term.setDatasource(datasource);

        // check prefix db matches datasource
        if (!prefix.equals(datasource.getPrefix()) && !datasource.getAlternatePrefix().contains(prefix.toLowerCase())) {
            throw new TermCreationException("Can't create term as prefix does not match a known prefix for this datasource");
        }

        // todo think need to remove this for the EDAM case if the source is valid, but the prefix isn't, then change the prefix to match the preferred prefix for that source
        if (!prefix.equals(datasource.getPrefix())) {
            Curie newCurie = CurieUtils.getCurie(datasource.getPrefix(), term.getIdentifier());
            term.setCurie(newCurie.toString());
        }

        try {
            term = termGraphRepository.save(term);
            // if successful, also update the Solr Index

            Document newDocument = DocumentBuilder.getDocumentFromTerm(term);
            documentRepository.save(newDocument);

        }  catch (org.neo4j.ogm.exception.CypherException e) {
            if (e.getCode().contains("ConstraintValidationFailed")) {
                throw new TermCreationException("Duplicate key exception, term already exists");
            }
            throw new RuntimeException("Erro saving term", e);
        }

        return term;
    }

    /**
     * Given a term id this method will look if term already exists, otherwise it will use the prefix to determine if its from
     * a datasource that already exists and create it. The Prefix must be valid i.e. unique to a datasource we know about.
     * @param id
     * @param label
     * @param uri
     * @return
     * @throws UnknownDatasourceException
     * @throws InvalidCurieException
     */
    @Transactional("transactionManager")
    public Term getOrCreateTerm(String id, String label, String uri) throws UnknownDatasourceException, InvalidCurieException {

        Document document = documentRepository.findOneByIdentifier(id);

        if (document != null) {
            Term r = termGraphRepository.findByCurie(document.getId());
            if (r != null) {
                return r;
            }
        }
        Term t = new Term();
        t.setCurie(id);
        t.setLabel(label);
        t.setUri(uri);
        return save(t);
    }

    public Page<Term> getTermsBySource(String prefix, Pageable pageable) {

        Collection<Term> terms =  termGraphRepository.findByDatasource(prefix, pageable.getOffset(), pageable.getPageSize());
        return new PageImpl<Term>(new ArrayList<Term>(terms));
    }


    public Page<Term> getTerms(Pageable pageable) {
        return termGraphRepository.findAll(pageable);
    }

    /**
     * Update fields on an existing term. Note you can't change the term curie, or identifier. If a term is wrong you should delete the
     * old term and add a new one
     * @param term
     * @return
     * @throws UnknownDatasourceException
     * @throws UnknownTermException
     */
    @Transactional("transactionManager")
    public Term update(Term term) throws UnknownDatasourceException, UnknownTermException {

        Term t = termGraphRepository.findByCurie(term.getCurie());

        if (t == null) {
            Document document = documentRepository.findOneByIdentifier(term.getCurie());
            if (document == null) {
                throw new UnknownTermException("Can't update as term doesn't exist:" + term.getCurie());
            }
            t = termGraphRepository.findByCurie(document.getCurie());
        }

        if (term.getLabel() != null) {
            t.setLabel(term.getLabel());
        }

        if (term.getUri() != null) {
            t.setUri(term.getUri());
        }

        t = termGraphRepository.save(t, 0);
        Document document = DocumentBuilder.getDocumentFromTerm(t);
        documentRepository.save(document);
        return t;
    }

    @Transactional
    public void delete(Term term) {

        Term t = termGraphRepository.findByCurie(term.getCurie());
        if (t != null) {
            termGraphRepository.delete(t.getId());
            Document document = DocumentBuilder.getDocumentFromTerm(term);
            if (document != null) {
                documentRepository.delete(document);
            }
        }


    }

    public void rebuildIndexes(String source) {

        documentRepository.deleteAll();

        int termCount = termGraphRepository.getIndexableTermCount();
        log.info("Total terms to index " + termCount);

        List<Document> chunk = new ArrayList<Document>();

        for (int x = 0; x <= termCount; x +=chunks) {
            log.info("Reading " +x + " terms from term repository...");
            for (IndexableTermInfo t : termGraphRepository.getAllIndexableTerms(x, chunks)) {
                chunk.add(DocumentBuilder.getDocumentFromTerm(t));
                // save solr in chuncks of 10000
                if (chunk.size() == 10000) {
                    log.info("Saving " +chunk.size() + " documents...");
                    documentRepository.save(chunk);
                    chunk.clear();
                }
            }
        }

        if (!chunk.isEmpty())  {
            log.info("Saving " +chunk.size() + " documents...");
            documentRepository.save(chunk);
        }
    }

    public void rebuildIndexes() {
        rebuildIndexes(null);
    }

    public Object getSummaryGraphJson(String curie, int distance) {
        return mappingQueryService.getMappingSummaryGraph(curie,distance);
    }

    public int getTermCountBySource(String prefix) {
        return termGraphRepository.getTermCountBySource(prefix);
    }
}
