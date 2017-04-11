package uk.ac.ebi.spot.service;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.spot.exception.*;
import uk.ac.ebi.spot.model.*;
import uk.ac.ebi.spot.repository.MappingRepository;
import uk.ac.ebi.spot.repository.TermGraphRepository;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Simon Jupp
 * @since 11/05/2016
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
     * @return
     * @throws UnknownTermException
     * @throws UnknownDatasourceException
     */
    @Transactional("transactionManager")
    public Mapping save(MappingRequest mappingRequest) throws MappingException, UnknownTermException, UnknownDatasourceException, InvalidCurieException {
        return mappingRepository.save(getMappingFromRequest(mappingRequest));
    }

    @Transactional("transactionManager")
    public Iterable<Mapping> saveAll(Collection<MappingRequest> mappingRequests)  {

        Collection<Mapping> mappings = new ArrayList<>();
        StopWatch timer = new StopWatch();
        System.out.println("preparing to save mappings");
        timer.start();

        for (MappingRequest mappingRequest: mappingRequests) {
            try {
                mappings.add(getMappingFromRequest(mappingRequest));
            } catch (Exception e) {
                log.warn(e.getMessage());
            }

        }
        timer.split();
        System.out.println(TimeUnit.NANOSECONDS.toSeconds(timer.getSplitNanoTime()));

        System.out.println("mappings ready to save");
        Iterable<Mapping> mappingssaved = mappingRepository.save(mappings);
        System.out.println("mappings saved");
        timer.split();
        System.out.println(TimeUnit.NANOSECONDS.toSeconds(timer.getSplitNanoTime()));
        timer.stop();
        return mappingssaved;
    }

    private Mapping getMappingFromRequest(MappingRequest mappingRequest) throws InvalidCurieException {
        Term fromT = termService.getOrCreateTerm(mappingRequest.getFromId(), null, null);
        Term toT = termService.getOrCreateTerm(mappingRequest.getToId(), null, null);


        Mapping mapping = mappingRepository.findOneByMappingBySourceAndId(fromT.getCurie(), toT.getCurie(), mappingRequest.getDatasourcePrefix(), mappingRequest.getScope().toString());

        if (mapping != null) {
            throw new MappingException("Mapping between these identifiers already exists from this source");
        }
        mapping = new Mapping();


        Datasource datasource = datasourceService.getDatasource(mappingRequest.getDatasourcePrefix());
        if (datasource == null) {
            throw new UnknownDatasourceException("You can only create mappings for an existing datasource");
        }
        mapping.setFromTerm(fromT);
        mapping.setToTerm(toT);
        mapping.setDatasource(datasource);
        mapping.setSourcePrefix(datasource.getPrefix());
        mapping.setSourceType(mappingRequest.getSourceType());
        mapping.setScope(mappingRequest.getScope());
        mapping.setDate(new Date());

        return mapping;
    }

    public List<Mapping> getMappingBySource(String sourcePrefix) {
        Datasource datasource = datasourceService.getDatasource(sourcePrefix);
        return mappingRepository.findAllByAnySource(datasource.getPrefix());
    }

    public Page<Mapping> getMappingBySource(String sourcePrefix, Pageable pageable) {
        Datasource datasource = datasourceService.getDatasource(sourcePrefix);
        return new PageImpl<Mapping>(mappingRepository.findAllByAnySource(datasource.getPrefix(), pageable.getOffset(), pageable.getPageSize()));
    }

    public Mapping findOneByMappingBySourceAndId(String fromCurie, String toCurie, String sourcePrefix, String scope) {
        return mappingRepository.findOneByMappingBySourceAndId(fromCurie, toCurie, sourcePrefix, scope);
    }

    public List<Mapping> findMappingsById(String fromCurie, String toCurie) {
        return mappingRepository.findMappingsById(fromCurie, toCurie);
    }

    public Page<Mapping> findMappingsById(String fromCurie, Pageable pageable) {
        return new PageImpl<Mapping>(mappingRepository.findMappingsById(fromCurie, pageable.getOffset(), pageable.getPageSize()));
    }

    public Page<Mapping> findMappingsById(String fromCurie, String toCurie, Pageable pageable) {
        return new PageImpl<Mapping>(mappingRepository.findMappingsById(fromCurie, toCurie, pageable.getOffset(), pageable.getPageSize()));
    }

    public List<Mapping> findMappingsById(String fromCurie) {
        return mappingRepository.findMappingsById(fromCurie);
    }

    public List<Mapping> findInferredMappingsById(String fromCurie, String toCurie) {
        return mappingRepository.findInferredMappingsById(fromCurie, toCurie);
    }

    public Page<SearchResult> getMappingsSearchByIds(List<String> identifiers, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix, Pageable pageable) {

        List<SearchResult> searchResults = new ArrayList<>();

        int size =  pageable.getOffset() + pageable.getPageSize() > identifiers.size() ? identifiers.size() : pageable.getOffset() + pageable.getPageSize();

        if (pageable.getOffset() <= size) {
            for (String id : identifiers.subList(pageable.getOffset(), size)) {
                Term fromTerm = termService.getTerm(id);

                // could check that source prefix and target prefixes are valid...
                if (fromTerm != null) {
                    SearchResult searchResult = mappingQueryService.getMappingResponseSearchById(fromTerm.getCurie(), distance, sourcePrefix, targetPrefix);
                    searchResult.setCurie(fromTerm.getCurie());
                    searchResult.setLabel(fromTerm.getLabel());
                    searchResult.setQueryId(id);
                    searchResults.add(searchResult);
                }
                else {
                    searchResults.add(new SearchResult(id, null,null, null, Collections.emptyList()));
                }
            }
        }
        return new PageImpl<SearchResult>(searchResults, pageable, identifiers.size());
    }


    public Page<SearchResult> getMappingsSearchByDatasource(String fromDatasource, int distance, Collection<String> sourcePrefix, Collection<String> targetPrefix, Pageable pageable) {
        // check the datasrouce is valid

        Datasource datasource = null;
        if (fromDatasource != null) {
            datasource = datasourceService.getDatasource(fromDatasource);
        }

        if (datasource != null) {
            return mappingQueryService.getMappingResponseSearchByDatasource(datasource.getPrefix(), distance, sourcePrefix, targetPrefix, pageable);
        }
        else  {
            return mappingQueryService.getMappingResponseSearchByDatasource(fromDatasource, distance, sourcePrefix, targetPrefix, pageable);
        }
//        return new PageImpl<SearchResult>(Collections.emptyList(), pageable, 0);
    }



    public void dropMappingsBySource(String sourcePrefix) {
        Iterable<Mapping> mappings = mappingRepository.findAllBySourcePrefix(sourcePrefix);
        mappingRepository.delete(mappings);
    }

    public void remove(Long id) {
        mappingRepository.delete(id);
    }


    public Page<Mapping> getMappings(Pageable pageable) {
        return mappingRepository.findAll(pageable);
    }

    public Mapping getMapping(String id) {
        return mappingRepository.findOne(Long.parseLong(id));
    }

    public Object getSummaryJson() {
        return mappingQueryService.getMappingSummary();
    }
    public Object getSummaryJson(String datasource) {
        return mappingQueryService.getMappingSummary(datasource);
    }

    /**
     * Get a list of curies for a given target datasource and distance. This is an optimised convenience query used by the UI
     * @param fromDatasource the from datasoource
     * @param targetDatasource the target datasource for mappings
     * @param distance defaults to 3
     * @return
     */
    public List<String> getMappedTermCuries (String fromDatasource, String targetDatasource, int distance) {
        return  mappingQueryService.getMappedTermCuries(fromDatasource, targetDatasource, distance);
    }

    /**
     * Get a list of target datasources and counts for a given input source and distance. This is an optimised convenience query used by the UI
     * @param fromDatasource the from datasoource
     * @param distance defaults to 3
     * @return
     */
    public Map<String, Integer> getMappedTargetCounts (String fromDatasource, int distance) {
        return  mappingQueryService.getMappedTargetCounts(fromDatasource, distance);
    }


}
