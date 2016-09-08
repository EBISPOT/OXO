package uk.ac.ebi.spot.controller.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

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

    @Autowired SearchResultAssembler searchResultAssembler;

    @RequestMapping(path = "", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.POST)
    public HttpEntity<PagedResources<SearchResult>> search(
            @RequestBody MappingSearchRequest request,
            PagedResourcesAssembler resourceAssembler

    ) {

        String identfiers = request.getIdentifiers();
        if (identfiers == null) {
            // handle error
            throw new RuntimeException("Must supply an id to search");
        }

        Set<String> ids = new HashSet<>(Arrays.asList(identfiers.split("\n")));

        List<SearchResult> map = mappingService.getMappingsSearch(ids, request.getDistance(), request.getMappingSource(), request.getMappingTarget());

        Page<SearchResult> resultsPage = new PageImpl<SearchResult>(map);

        return new ResponseEntity<>(resourceAssembler.toResource(resultsPage, searchResultAssembler), HttpStatus.OK);


    }

    @ExceptionHandler({Exception.class})
    public void handleError(HttpServletResponse response, Exception exception) throws IOException {
        response.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.getMessage());
    }

}
