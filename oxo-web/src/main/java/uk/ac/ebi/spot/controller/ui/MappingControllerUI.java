package uk.ac.ebi.spot.controller.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import uk.ac.ebi.spot.exception.InvalidCurieException;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.MappingRequest;
import uk.ac.ebi.spot.model.MappingSource;
import uk.ac.ebi.spot.model.SourceType;
import uk.ac.ebi.spot.repository.DatasourceRepository;
import uk.ac.ebi.spot.security.model.OrcidPrinciple;
import uk.ac.ebi.spot.security.model.OrcidUser;
import uk.ac.ebi.spot.security.repository.UserRepository;
import uk.ac.ebi.spot.security.service.OrcidUserService;
import uk.ac.ebi.spot.service.DatasourceService;
import uk.ac.ebi.spot.service.MappingService;

import java.util.Collections;
import java.util.HashSet;

/**
 * @author Simon Jupp
 * @date 23/09/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Controller
@RequestMapping("mapping")
public class MappingControllerUI {

    @Autowired
    UserRepository userRepository;

    @Autowired
    DatasourceService datasourceService;

    @Autowired
    MappingService mappingService;

    @Secured("ROLE_USER")
    @PostMapping
    public ModelAndView addNewMapping (MappingRequest mappingRequest, @AuthenticationPrincipal OrcidPrinciple principle) {

        try {
            OrcidUser user = userRepository.findByOrcid(principle.getOrcid());

            Datasource userSource = datasourceService.getDatasource(user.getOrcid());
            if (userSource == null) {
                userSource = new Datasource(
                        user.getOrcid(),
                        null,
                        new HashSet<String>(),
                        user.getGivenName(),
                        null,
                        SourceType.USER
                );
                userSource = datasourceService.getOrCreateDatasource(userSource);
            }

            mappingService.save(
                    mappingRequest.getFromId(),
                    mappingRequest.getToId(),
                    userSource.getPrefix(),
                    mappingRequest.getSourceType(),
                    mappingRequest.getScope()
            );
            // get or create datasource from user detail
//            redirectAttributes.addAttribute("message", "Successfully created mapping to " + mappingRequest.getToId());

        } catch (InvalidCurieException e) {
            e.printStackTrace();
//            redirectAttributes.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();

//            redirectAttributes.addAttribute("error", e.getMessage());

        }
        String url = "terms/"+mappingRequest.getFromId();
        RedirectView rv = new RedirectView(url);
        rv.setExposeModelAttributes(false);
        rv.setStatusCode(HttpStatus.FOUND);
        rv.setUrl(url);
        return new ModelAndView(rv);
    }
}
