package uk.ac.ebi.spot.controller.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ebi.spot.model.MappingRequest;
import uk.ac.ebi.spot.model.Scope;
import uk.ac.ebi.spot.model.SourceType;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.service.MappingResponse;
import uk.ac.ebi.spot.service.MappingService;
import uk.ac.ebi.spot.service.TermService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Simon Jupp
 * @date 31/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Controller
@RequestMapping("terms")
public class TermControllerUI {

    @Autowired
    TermService termService;

    @Autowired
    MappingService mappingService;
    @RequestMapping(path = "/{curie}", produces = {MediaType.TEXT_HTML_VALUE}, method = RequestMethod.GET)
    public String getTerms (@PathVariable("curie") String curie, Model model) {

        Term t = termService.getTerm(curie);

        if (t == null) {
            model.addAttribute("error", "Term not found");

        }  else {
            model.addAttribute("id", t.getCurie());
            model.addAttribute("term", t);
        }

        MappingRequest mappingRequest = new MappingRequest();
        mappingRequest.setFromId(curie);
        mappingRequest.setSourceType(SourceType.USER);
        mappingRequest.setScope(Scope.EXACT);
        model.addAttribute("mappingRequest", mappingRequest);

        return "terms";
    }


}
