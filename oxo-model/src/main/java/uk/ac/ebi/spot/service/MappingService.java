package uk.ac.ebi.spot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.spot.exception.*;
import uk.ac.ebi.spot.model.*;
import uk.ac.ebi.spot.repository.MappingRepository;
import uk.ac.ebi.spot.repository.TermGraphRepository;

import java.util.*;

/**
 * @author Simon Jupp
 * @date 11/05/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Service
public class MappingService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private TermGraphRepository termGraphRepository;

    @Autowired
    private MappingRepository mappingRepository;

    @Autowired
    private DatasourceService datasourceService;

    @Autowired
    MappingQueryService mappingQueryService;

    @Autowired
    TermService termService;

    public MappingService() {
    }

    /**
     * Save a mapping between two terms. Terms must have a prefix that is from a known datasource
     * @param fromId
     * @param toId
     * @param datasourcePrefix
     * @param sourceType
     * @param scope
     * @return
     * @throws UnknownTermException
     * @throws UnknownDatasourceException
     */
    @Transactional("transactionManager")
    public Mapping save(String fromId, String toId, String datasourcePrefix, SourceType sourceType, Scope scope) throws MappingException, UnknownTermException, UnknownDatasourceException, InvalidCurieException {


        Term fromT = termService.getOrCreateTerm(fromId, null, null);
        Term toT = termService.getOrCreateTerm(toId, null, null);


        Mapping mapping = mappingRepository.findOneByMappingBySourceAndId(fromT.getCurie(), toT.getCurie(), datasourcePrefix, scope.toString());

        if (mapping != null) {
            throw new MappingException("Mapping between these identifiers already exists from this source");
        }
        mapping = new Mapping();


        Datasource datasource = datasourceService.getDatasource(datasourcePrefix);
        if (datasource == null) {
            throw new UnknownDatasourceException("You can only create mappings for an existing datasource");
        }
        mapping.setFromTerm(fromT);
        mapping.setToTerm(toT);
        mapping.setDatasource(datasource);
        mapping.setSourcePrefix(datasource.getPrefix());
        mapping.setSourceType(sourceType);
        mapping.setScope(scope);
        mapping.setDate(new Date());
        return mappingRepository.save(mapping);
    }

    public Page<Mapping> getMappingBySource(String sourcePrefix, Pageable pageable) {
        Datasource datasource = datasourceService.getDatasource(sourcePrefix);
        return mappingRepository.findAllByAnySource(datasource.getPrefix(), pageable);
    }

    public List<SearchResult> getMappingsSearch(Collection<String> identifiers, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix) {

        List<SearchResult> searchResults = new ArrayList<>();
        LinkedHashMap<String, List<MappingResponse>> mappingResponses = new LinkedHashMap<>();

        for (String id : identifiers) {
            Term fromTerm = termService.getTerm(id);

            // could check that source prefix and target prefixes are valid...

            List<MappingResponse> mappingResponse = new ArrayList<>();

            String fromCurie = id;
            String fromLabel = null;
            if (fromTerm != null) {
                mappingResponse = mappingQueryService.getMappingResponseSearch(fromTerm.getCurie(), distance, sourcePrefix, targetPrefix);
                fromCurie = fromTerm.getCurie();
                fromLabel = fromTerm.getLabel();
            }

            searchResults.add(new SearchResult(fromCurie, fromLabel, mappingResponse));
        }

        return searchResults;
    }




    public void dropMappingsBySource(String sourcePrefix) {
        Iterable<Mapping> mappings = mappingRepository.findAllBySourcePrefix(sourcePrefix);
        mappingRepository.delete(mappings);
    }


    public Page<Mapping> getMappings(Pageable pageable) {
        return mappingRepository.findAll(pageable);
    }

    public Mapping getMapping(String id) {
        return mappingRepository.findOne(Long.getLong(id));
    }

    public Object getSummaryJson() {
        return mappingQueryService.getMappingSummary();
    }
}
