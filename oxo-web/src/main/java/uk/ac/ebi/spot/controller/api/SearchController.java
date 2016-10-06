package uk.ac.ebi.spot.controller.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.exception.InvalidCurieException;
import uk.ac.ebi.spot.exception.MappingException;
import uk.ac.ebi.spot.exception.UnknownDatasourceException;
import uk.ac.ebi.spot.model.MappingSearchRequest;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.service.MappingService;
import uk.ac.ebi.spot.service.SearchResult;
import uk.ac.ebi.spot.service.SearchResultsCsvBuilder;
import uk.ac.ebi.spot.service.TermService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Simon Jupp
 * @date 26/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Controller
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    MappingService mappingService;

    @Autowired
    TermService termService;

    @Autowired SearchResultAssembler searchResultAssembler;

    @Autowired
    SearchResultsCsvBuilder csvBuilder;

    @RequestMapping(path = "", produces = "text/csv", method = RequestMethod.GET)
    public void getSearchAsCSV(
            MappingSearchRequest request,
            HttpServletResponse response

    ) {

        response.setContentType("text/csv");
        response.setHeader( "Content-Disposition", "filename=mappings.csv" );

        List<SearchResult> map = getSearchResults(request);

        try {
            csvBuilder.writeResultsAsCsv(map, ',', response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(path = "", produces = "text/tsv", method = RequestMethod.GET)
    public void getSearchAsTsv(
            MappingSearchRequest request,
            HttpServletResponse response

    ) {

        response.setContentType("text/tsv");
        response.setHeader( "Content-Disposition", "filename=mappings.tsv" );

        List<SearchResult> map = getSearchResults(request);

        try {
            csvBuilder.writeResultsAsCsv(map, '\t', response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(path = "",  produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.POST)
    public HttpEntity<PagedResources<SearchResult>> postSearch(
                @RequestBody MappingSearchRequest request,
                PagedResourcesAssembler resourceAssembler

        ) {
        return search(request, resourceAssembler);
    }

    @RequestMapping(path = "", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
    public HttpEntity<PagedResources<SearchResult>> search(
            MappingSearchRequest request,
            PagedResourcesAssembler resourceAssembler

    ) {

        List<SearchResult> map = getSearchResults(request);
        Page<SearchResult> resultsPage = new PageImpl<SearchResult>(map);

        return new ResponseEntity<>(resourceAssembler.toResource(resultsPage, searchResultAssembler), HttpStatus.OK);


    }

    private List<SearchResult> getSearchResults(MappingSearchRequest request) {
        Set<String> identfiers = request.getIds();
        if (identfiers == null && request.getInputSource().isEmpty()) {
            // handle error
            throw new RuntimeException("Must supply an id or inputDatasource to search");
        }

        Set<String> ids = new HashSet<>();
        if (!identfiers.isEmpty()) {
            for (String id : identfiers) {
                ids.addAll(new HashSet<>(Arrays.asList(id.split("\n"))));
            }
        } else if (!request.getInputSource().isEmpty()) {
            for (String inputSource : request.getInputSource()) {

                // we may need to support paging search results
                ids.addAll(
                        termService.getTermsBySource(inputSource, new PageRequest(0, 100000)).getContent().stream().map(Term::getCurie).collect(Collectors.toSet())
                );
            }
        } else  {
            throw new RuntimeException("Must supply an id or inputDatasource to search");
        }

        return mappingService.getMappingsSearch(ids, request.getDistance(), request.getMappingSource(), request.getMappingTarget());

    }



    @ExceptionHandler({Exception.class})
    public void handleError(HttpServletResponse response, Exception exception) throws IOException {
        response.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.getMessage());
    }

}
