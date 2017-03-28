package uk.ac.ebi.spot.controller.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import uk.ac.ebi.spot.exception.InvalidCurieException;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Mapping;
import uk.ac.ebi.spot.model.MappingRequest;
import uk.ac.ebi.spot.model.SourceType;
import uk.ac.ebi.spot.security.model.OrcidPrinciple;
import uk.ac.ebi.spot.security.model.OrcidUser;
import uk.ac.ebi.spot.security.repository.OrcidUserRepository;
import uk.ac.ebi.spot.service.DatasourceService;
import uk.ac.ebi.spot.service.MappingService;

import java.util.HashSet;
import java.util.List;

/**
 * @author Simon Jupp
 * @date 23/09/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Controller
@RequestMapping("mappings")
public class MappingControllerUI {

    @Autowired
    OrcidUserRepository userRepository;

    @Autowired
    DatasourceService datasourceService;

    @Autowired
    MappingService mappingService;

    @RequestMapping(path = "", produces = {MediaType.TEXT_HTML_VALUE}, method = RequestMethod.GET)
    public String getMappings (
            Model model,
            @RequestParam(value = "fromId", required=false) String fromId,
            @RequestParam(value = "toId", required=false) String toId,
            @RequestParam(value = "source", required=false) String source,
            @RequestParam(value = "scope", required=false) String scope
    ) {

        if (fromId != null && toId != null && source != null && scope != null) {
            Mapping mapping = mappingService.findOneByMappingBySourceAndId(fromId, toId, source, scope);
            if (mapping == null) {
                model.addAttribute("error", "mapping not found");
                return "index";

            }  else {
                return "forward:mappings/"+mapping.getMappingId();
            }
        }
        else if  (fromId != null && toId != null) {
            List<Mapping> mappings = mappingService.findMappingsById(fromId, toId);
            List<Mapping> derivedMappings = mappingService.findInferredMappingsById(fromId, toId);
            if (mappings.isEmpty() && derivedMappings.isEmpty()) {
                model.addAttribute("error", "mapping not found");
                return "index";

            }  else {
                model.addAttribute("from", fromId);
                model.addAttribute("to", toId);
                model.addAttribute("mappings", mappings);
                model.addAttribute("derivedMappings", derivedMappings);
                return "mappings";
            }
        }

        return "index";
    }

    @RequestMapping(path = "/{id}", produces = {MediaType.TEXT_HTML_VALUE}, method = RequestMethod.GET)
    public String getMapping (@PathVariable("id") String id, Model model) {

        Mapping mapping = mappingService.getMapping(id);

        if (mapping == null) {
            model.addAttribute("error", "mapping not found");

        }  else {
            model.addAttribute("mapping", mapping);
        }
        return "mapping";
    }

    @RequestMapping(value = "/{id}", produces = MediaType.TEXT_HTML_VALUE, method = RequestMethod.POST, params={"delete"})
    @Secured("ROLE_USER")
    public ModelAndView deleteMapping(
            @PathVariable("id") String id,
            @AuthenticationPrincipal OrcidPrinciple principle,
            Model model,
            final RedirectAttributes redirectAttributes) {

        Mapping mapping = mappingService.getMapping(id);

        if (mapping == null) {
            model.addAttribute("error", "mapping not found for id " + id);
            return redirect("../index");

        }
        if (!principle.getOrcid().equals(mapping.getSourcePrefix())) {
            redirectAttributes.addFlashAttribute("error", "You don't have permissions to remove this mapping");
            return redirect(mapping.getMappingId().toString());
        }

        mappingService.remove(mapping.getMappingId());
        redirectAttributes.addFlashAttribute("message", "Successfully removed mapping: " + mapping.getMappingId());
        return redirect("../myaccount");
    }

    @Secured("ROLE_USER")
    @PostMapping
    public ModelAndView addNewMapping (MappingRequest mappingRequest, @AuthenticationPrincipal OrcidPrinciple principle, final RedirectAttributes redirectAttributes) {

        try {
            OrcidUser user = userRepository.findByOrcid(principle.getOrcid());

            Datasource userSource = datasourceService.getDatasource(user.getOrcid());
            if (userSource == null) {
                userSource = new Datasource(
                        user.getOrcid(),
                        user.getGivenName(),
                        null,
                        new HashSet<String>(),
                        user.getGivenName(),
                        null,
                        SourceType.USER
                );
                userSource = datasourceService.getOrCreateDatasource(userSource);
            }

            mappingRequest.setDatasourcePrefix(userSource.getPrefix());
            mappingService.save(
                    mappingRequest
            );
            // get or create datasource from user detail
            redirectAttributes.addFlashAttribute("message", "Successfully created mapping to " + mappingRequest.getToId());

        } catch (InvalidCurieException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        String url = "terms/"+mappingRequest.getFromId();
        return   redirect(url);
    }

    private ModelAndView redirect(String url) {
        RedirectView rv = new RedirectView(url);
        rv.setExposeModelAttributes(false);
        rv.setStatusCode(HttpStatus.FOUND);
        rv.setUrl(url);
        return new ModelAndView(rv);
    }
}
