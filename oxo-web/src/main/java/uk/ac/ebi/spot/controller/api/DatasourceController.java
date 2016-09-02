package uk.ac.ebi.spot.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.RepositoryRestExceptionHandler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.exception.InvalidCurieException;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.service.DatasourceService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Simon Jupp
 * @date 09/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Controller
@RequestMapping("/api/datasources")
@ExposesResourceFor(Datasource.class)
@ControllerAdvice(basePackageClasses = RepositoryRestExceptionHandler.class)
public class DatasourceController implements
        ResourceProcessor<RepositoryLinksResource> {

    @Autowired
    private DatasourceAssembler datasourceAssembler;

    @Autowired
    private DatasourceService datasourceService;

    @RequestMapping(path = "", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
    HttpEntity<PagedResources<Datasource>> sources(
            Pageable pageable,
            PagedResourcesAssembler resourceAssembler) throws ResourceNotFoundException {

        Page<Datasource> page = datasourceService.getDatasources(pageable);

        return new ResponseEntity<>(resourceAssembler.toResource(page, datasourceAssembler), HttpStatus.OK);
    }

    @RequestMapping(path = "", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.POST)
    HttpEntity<Datasource> saveSource(@RequestBody Datasource datasource) throws ResourceNotFoundException {
        return new ResponseEntity<Datasource>(datasourceService.save(datasource), HttpStatus.OK);
    }


    @RequestMapping(path = "/{prefix}", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
    HttpEntity<Resource<Datasource>> getDatasource(
            @PathVariable("prefix") String prefix) throws ResourceNotFoundException {

        Datasource page = datasourceService.getDatasource(prefix);

        return new ResponseEntity<>(datasourceAssembler.toResource(page), HttpStatus.OK);
    }


    @RequestMapping(path = "/{prefix}", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.PUT)
    HttpEntity<Datasource> updateSource(@PathVariable("prefix") String prefix, @RequestBody Datasource datasource) throws ResourceNotFoundException, InvalidCurieException {
        datasource.setPrefix(prefix);
        return new ResponseEntity<Datasource>(datasourceService.update(datasource), HttpStatus.OK);
    }

    @RequestMapping(path = "/{prefix}", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    void deleteTerm(@PathVariable("curie") String curie, @RequestBody Datasource term) throws ResourceNotFoundException {
        throw new UnsupportedOperationException("Can't delete datasources");
    }

    //////////////// Exception handling ///////////////////
    // throw a 422
    @ExceptionHandler (DuplicateKeyException.class)
    public void handleError(HttpServletResponse response, Exception exception) throws IOException {
        response.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.getMessage());
    }

    @ExceptionHandler (InvalidCurieException.class)
    public void handleError2(HttpServletResponse response, Exception exception) throws IOException {
        response.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.getMessage());
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(ControllerLinkBuilder.linkTo(DatasourceController.class).withRel("datasource"));
        return resource;
    }
}

