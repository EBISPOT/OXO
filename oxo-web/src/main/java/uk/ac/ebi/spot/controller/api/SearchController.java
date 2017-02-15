package uk.ac.ebi.spot.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.model.MappingSearchRequest;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.service.MappingService;
import uk.ac.ebi.spot.service.SearchResult;
import uk.ac.ebi.spot.service.SearchResultsCsvBuilder;
import uk.ac.ebi.spot.service.TermService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    @RequestMapping(path = "", produces = "text/csv", consumes = "application/x-www-form-urlencoded;charset=UTF-8", method = RequestMethod.POST)
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

            int page = 0;
            PageRequest pageRequest = new PageRequest(page, 100);
            List<SearchResult> map = getSearchResults(request, pageRequest).getContent();
            csvBuilder.writeResultsAsCsv(map);
            while (!map.isEmpty()) {
                map = getSearchResults(request, pageRequest).getContent();
                csvBuilder.writeResultsAsCsv(map);
                page++;
                pageRequest = new PageRequest(page, 100);
            }


            csvBuilder.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(path = "", produces = "text/tsv",consumes = "application/x-www-form-urlencoded;charset=UTF-8", method = RequestMethod.POST)
    public void getSearchAsTsv(
            MappingSearchRequest request,
            HttpServletResponse response

    ) {
        getSearchAsCSV(request, response, "\t", "tsv");
    }

    @RequestMapping(path = "",  produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = "application/x-www-form-urlencoded;charset=UTF-8", method = RequestMethod.POST)
    public HttpEntity<PagedResources<SearchResult>> postSearchFromForm(
            MappingSearchRequest request,
            Pageable pageable,
            PagedResourcesAssembler resourceAssembler

    ) {
        return search(request, pageable, resourceAssembler);
    }

    @RequestMapping(path = "",  produces = {MediaType.APPLICATION_JSON_VALUE}, consumes = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.POST)
    public HttpEntity<PagedResources<SearchResult>> postSearch(
            @RequestBody MappingSearchRequest request,
            Pageable pageable,
            PagedResourcesAssembler resourceAssembler

    ) {
        return search(request, pageable, resourceAssembler);
    }

    @RequestMapping(path = "", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
    public HttpEntity<PagedResources<SearchResult>> search(
            MappingSearchRequest request,
            Pageable pageable,
            PagedResourcesAssembler resourceAssembler

    ) {

        Page<SearchResult> resultsPage =getSearchResults(request, pageable);

        return new ResponseEntity<>(resourceAssembler.toResource(resultsPage, searchResultAssembler), HttpStatus.OK);


    }

    private Page<SearchResult> getSearchResults(MappingSearchRequest request, Pageable pageable) {
        if (request.getIds().isEmpty() && request.getInputSource() == null) {
            // handle error
            throw new RuntimeException("Must supply an id or input datasources to search");
        }
        // if Ids are provided then we know what to lookup
        List<String> ids =request.getIds();;
        if (!ids.isEmpty()) {
            return mappingService.getMappingsSearchByIds(ids, request.getDistance(), request.getMappingSource(), request.getMappingTarget(), pageable);
        }
        String inputSource = request.getInputSource();
        return mappingService.getMappingsSearchByDatasource(inputSource, request.getDistance(), request.getMappingSource(), request.getMappingTarget(), pageable);
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
