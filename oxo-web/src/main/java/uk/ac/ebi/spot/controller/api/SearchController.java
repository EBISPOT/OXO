package uk.ac.ebi.spot.controller.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.spot.exception.InvalidCurieException;
import uk.ac.ebi.spot.exception.MappingException;
import uk.ac.ebi.spot.exception.UnknownDatasourceException;
import uk.ac.ebi.spot.model.MappingSearchRequest;
import uk.ac.ebi.spot.service.MappingService;

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

    @PostMapping
    public HttpEntity<String> search(
            MappingSearchRequest request

    ) {

        String identfiers = request.getIdentifiers();
        if (identfiers == null) {
            // handle error
            throw new RuntimeException("Must supply an id to search");
        }

        Set<String> ids = new HashSet<>(Arrays.asList(identfiers.split("\n")));

        LinkedHashMap map = mappingService.getMappingsSearch(ids, request.getDistance(), Collections.emptySet(), Collections.emptySet());
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();

        try {
            return new HttpEntity<String>(ow.writeValueAsString(map));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can't get summary view");
        }    }

    @ExceptionHandler({Exception.class})
    public void handleError(HttpServletResponse response, Exception exception) throws IOException {
        response.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.getMessage());
    }

}
