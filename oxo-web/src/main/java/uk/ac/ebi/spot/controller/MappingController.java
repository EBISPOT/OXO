package uk.ac.ebi.spot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ebi.spot.model.Mapping;
import uk.ac.ebi.spot.model.MappingQuery;
import uk.ac.ebi.spot.service.CypherMappingQueryBuilder;
import uk.ac.ebi.spot.service.MappingService;

import java.util.List;

/**
 * @author Simon Jupp
 * @date 14/06/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Controller
@RequestMapping("/api/mappings")
@ExposesResourceFor(Mapping.class)
public class MappingController {

    @Autowired
    private MappingService mappingService;

    @RequestMapping(path = "/search", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.POST)
    HttpEntity<List<CypherMappingQueryBuilder.MappingResponse>> getOntology(@RequestBody MappingQuery mappingQuery) throws ResourceNotFoundException {
        List<CypherMappingQueryBuilder.MappingResponse> mappingList =  mappingService.getMappingsSearch(mappingQuery.getId(), mappingQuery.getDistance(), mappingQuery.getSourcePrefix());

        return new ResponseEntity<List<CypherMappingQueryBuilder.MappingResponse>>(mappingList, HttpStatus.OK);

    }

}
