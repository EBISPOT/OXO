package uk.ac.ebi.spot.controller.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Mapping;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.security.model.OrcidPrinciple;
import uk.ac.ebi.spot.service.DatasourceService;
import uk.ac.ebi.spot.service.MappingService;
import uk.ac.ebi.spot.service.TermService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Simon Jupp
 * @date 16/08/2016
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

    @RequestMapping(path = {"", "index"})
    public String home(Model model) {

        model.addAttribute("datasources",datasourceService.getDatasourceWithMappings());

        return "index";
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

        return "myaccount";
    }
}
