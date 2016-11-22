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
import java.io.OutputStream;
import java.util.*;
import java.util.function.Function;
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

//    @Autowired
//    SearchResultsCsvBuilder csvBuilder;

    @RequestMapping(path = "", produces = "text/csv", method = RequestMethod.POST)
    public void getSearchAsCSV(
            MappingSearchRequest request,
            HttpServletResponse response,
            String seperator,
            String contentType


    ) {
        if (seperator == null && contentType == null) {
            contentType = "csv";
            seperator = ",";
        }

        response.setContentType("text/"+contentType);
        response.setHeader( "Content-Disposition", "filename=mappings."+contentType );

        try {
            SearchResultsCsvBuilder csvBuilder = new SearchResultsCsvBuilder(seperator.charAt(0), response.getOutputStream());
            csvBuilder.writeHeaders();
            for (String id: request.getIds()) {

                // check if list of ids
                Set<String> ids  = Arrays.asList(id.split("\n")).stream().map(trim).collect(Collectors.toSet());
                for (String id2 : ids)  {

                    MappingSearchRequest mpq1 = new MappingSearchRequest(
                            Collections.singleton(id2),
                            request.getInputSource(), request.getMappingSource(), request.getMappingTarget());
                    mpq1.setDistance(request.getDistance());
                    List<SearchResult> map = getSearchResults(mpq1);
                    csvBuilder.writeResultsAsCsv(map);
                }
            }
            csvBuilder.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(path = "", produces = "text/tsv", method = RequestMethod.POST)
    public void getSearchAsTsv(
            MappingSearchRequest request,
            HttpServletResponse response

    ) {
        getSearchAsCSV(request, response, "\t", "tsv");
    }

    @RequestMapping(path = "",  produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = "application/x-www-form-urlencoded;charset=UTF-8", method = RequestMethod.POST)
    public HttpEntity<PagedResources<SearchResult>> postSearchFromFrom(
            MappingSearchRequest request,
            PagedResourcesAssembler resourceAssembler

    ) {
        return search(request, resourceAssembler);
    }

    @RequestMapping(path = "",  produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.POST)
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
            throw new RuntimeException("Must supply an id or input datasource to search");
        }

        Set<String> ids = new HashSet<>();
        if (!identfiers.isEmpty()) {
            for (String id : identfiers) {
                ids.addAll(new HashSet<>(Arrays.asList(id.split("\n"))));
            }
            ids = ids.stream().map(trim).collect(Collectors.toSet());
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

    private static Function<String,String> trim = new Function<String,String>() {
        @Override public String apply(String s) {
            return s.replaceAll("(\\r|\\n)*$", "").trim();

        }
    };


    @ExceptionHandler({Exception.class})
    public void handleError(HttpServletResponse response, Exception exception) throws IOException {
        response.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.getMessage());
    }

}
