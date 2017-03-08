package uk.ac.ebi.spot.controller.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
import org.springframework.http.*;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.exception.InvalidCurieException;
import uk.ac.ebi.spot.exception.UnknownDatasourceException;
import uk.ac.ebi.spot.exception.UnknownTermException;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.security.model.OrcidPrinciple;
import uk.ac.ebi.spot.security.repository.UserRepository;
import uk.ac.ebi.spot.service.TermService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Simon Jupp
 * @date 04/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */

@Controller
@RequestMapping("/api/terms")
@ExposesResourceFor(Term.class)
@ControllerAdvice(basePackageClasses = RepositoryRestExceptionHandler.class)
public class TermController  implements
        ResourceProcessor<RepositoryLinksResource> {

    @Autowired TermAssembler termAssembler;

    @Autowired
    private TermService termService;

    @Autowired
    UserRepository userRepository;

    @RequestMapping(produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
    HttpEntity<PagedResources<Term>> terms(
            @RequestParam(value = "datasource", required = false) String datasource,
            Pageable pageable,
            PagedResourcesAssembler resourceAssembler) throws ResourceNotFoundException {

        Page<Term> page;

        if (datasource != null ) {
            page = termService.getTermsBySource(datasource, pageable);
        } else {
            page = termService.getTerms(pageable);
        }

        return new ResponseEntity<>(resourceAssembler.toResource(page, termAssembler), HttpStatus.OK);
    }

    @RequestMapping(path = "", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.POST)
    HttpEntity<Term> saveTerm(@RequestBody Term term) throws ResourceNotFoundException, InvalidCurieException {

        return new ResponseEntity<Term>(termService.save(term), HttpStatus.OK);

    }



    @RequestMapping(path = "/{curie}", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
    HttpEntity<Resource<Term>> getTerm(
            @PathVariable("curie") String curie) throws ResourceNotFoundException {

        Term page = termService.getTerm(curie);

        return new ResponseEntity<>(termAssembler.toResource(page), HttpStatus.OK);
    }

    @RequestMapping(path = "/{curie}", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.PUT)
    HttpEntity<Term> updateTerm(@PathVariable("curie") String curie, @RequestBody Term term) throws ResourceNotFoundException {
        term.setCurie(curie);
        try {
            return new ResponseEntity<Term>(termService.update(term), HttpStatus.OK);
        } catch (UnknownDatasourceException | UnknownTermException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @CrossOrigin
    @RequestMapping(path = "/{curie}/graph", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    HttpEntity<String> getTermMappingGraph(@PathVariable("curie") String curie) throws ResourceNotFoundException {
        Term page = termService.getTerm(curie);

        if (page != null) {
            Object object=  termService.getSummaryGraphJson(page.getCurie());
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            try {
                return new HttpEntity<String>(ow.writeValueAsString(object));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Can't get summary view");
            }
        }
        throw new ResourceNotFoundException();
    }

    @RequestMapping(path = "/{curie}", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.PATCH)
    HttpEntity<Term> patchTerm(@RequestParam(value = "apikey",required=false) String apikey, @PathVariable("curie") String curie, @RequestParam(required = false, name = "label") String label, @RequestParam(required = false, name = "uri") String uri) throws ResourceNotFoundException {

        if (userRepository.findByApikey(apikey) == null) {
            throw new UnauthorizedUserException("User with this api key are not authorised to update terms");
        }

        Term t = new Term();
        t.setCurie(curie);

        if (label != null) {
            t.setLabel(label);
        }

        if (uri!= null) {
            t.setUri(uri);
        }
        try {
            return new ResponseEntity<Term>(termService.update(t), HttpStatus.OK);
        } catch (UnknownDatasourceException | UnknownTermException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @RequestMapping(path = "/{curie}", produces = {MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    void deleteTerm(@PathVariable("curie") String curie, @RequestBody Term term) throws ResourceNotFoundException {
        term.setCurie(curie);
        termService.delete(term);

    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(ControllerLinkBuilder.linkTo(TermController.class).withRel("terms"));
        return resource;
    }

    //////////////// Exception handling ///////////////////
    // throw a 422
    @ExceptionHandler ({DuplicateKeyException.class, UnknownDatasourceException.class, InvalidCurieException.class})
    public void handleError(HttpServletResponse response, Exception exception) throws IOException {
        response.sendError(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.getMessage());
    }

    // throw a 422
    @ExceptionHandler ({UnauthorizedUserException.class})
    public void handleUnauthorisedError(HttpServletResponse response, Exception exception) throws IOException {
        response.sendError(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
    }

}
