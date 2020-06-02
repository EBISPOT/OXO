package uk.ac.ebi.spot.controller.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Mapping;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.security.model.OrcidPrinciple;
import uk.ac.ebi.spot.service.DatasourceService;
import uk.ac.ebi.spot.service.MappingService;
import uk.ac.ebi.spot.service.TermService;
import uk.ac.ebi.spot.util.MappingDistance;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Simon Jupp
 * @since 16/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Controller
@RequestMapping("")
public class IndexController {

    @Autowired
    DatasourceService datasourceService;

    @Autowired
    TermService termService;

    @Autowired
    MappingService mappingService;

    @Autowired
    CustomisationProperties customisationProperties;

    @RequestMapping(path = {"", "index"})
    public String home(Model model) {

        model.addAttribute("datasources",datasourceService.getDatasourceWithMappings());
        model.addAttribute("distance", MappingDistance.DEFAULT_MAPPING_DISTANCE);

        customisationProperties.setCustomisationModelAttributes(model);

        return "index";
    }

    /*
    @RequestMapping({"docs"})
    public String showDocsIndex(Model model) {
        return "redirect:docs/";
    }
    // ok, this is bad, need to find a way to deal with trailing slashes and constructing relative URLs in the thymeleaf template...
    @RequestMapping({"docs/"})
    public String showDocsIndex2(Model model) {*/
    /*
    @RequestMapping(path = "docs")
    public String docs(Model model) {
        return "docs";
    } 

    @RequestMapping({"docs/{page}"})
    public String showDocs(@PathVariable("page") String pageName, Model model) {
        model.addAttribute("page", pageName);
        return "docs-template";
    }*/

    @RequestMapping(path = "about")
    public String about(Model model) {
        return "about";
    }

    @RequestMapping(path = "contact")
    public String contact(Model model) {
        return "contact";
    }


    @Secured("ROLE_USER")
    @RequestMapping(path = "myaccount")
    public String myAccount(Model model, @AuthenticationPrincipal OrcidPrinciple principle,  Pageable pageable) {

        Datasource datasource  = datasourceService.getDatasource(principle.getOrcid());
        if (datasource == null) {
            model.addAttribute("error", "Datasource not found");

        }  else {
            List<Mapping> mappings = mappingService.getMappingBySource(datasource.getPrefix());
            model.addAttribute("mappings",mappings);

        }

        customisationProperties.setCustomisationModelAttributes(model);

        return "myaccount";
    }





    @RequestMapping({"docs"})
    public String showDocsIndex(Model model) {
        return "redirect:docs/index";
    }
    // ok, this is bad, need to find a way to deal with trailing slashes and constructing relative URLs in the thymeleaf template...
    @RequestMapping({"docs/"})
    public String showDocsIndex2(Model model) {
        return "redirect:index";
    }

    @RequestMapping({"docs/{page}"})
    public String showDocs(@PathVariable("page") String pageName, Model model) {
        model.addAttribute("page", pageName);
        customisationProperties.setCustomisationModelAttributes(model);
        return "docs-template";
    }







}
