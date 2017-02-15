package uk.ac.ebi.spot.controller.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Mapping;
import uk.ac.ebi.spot.model.MappingSearchRequest;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.service.DatasourceService;
import uk.ac.ebi.spot.service.MappingResponse;
import uk.ac.ebi.spot.service.MappingService;
import uk.ac.ebi.spot.service.TermService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Simon Jupp
 * @date 30/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Controller
@RequestMapping("search")
public class SearchControllerUI {

    @Autowired
    MappingService mappingService;

    @Autowired
    TermService termService;

    @Autowired
    DatasourceService datasourceService;

    @ModelAttribute("all_datasources")
    public List<Datasource> getDatasources() {
        return datasourceService.getDatasurceWithMappings();
    }

    @ModelAttribute("mapping_datasources")
    public List<Datasource> getMappingSources() {
        return datasourceService.getMappingSources();
    }

    @GetMapping
    public String search() {
        return "index";
    }

    @PostMapping
    public String search(
            MappingSearchRequest request,
            Model model,
            Pageable pageable

    ) {

        List<String> ids = new ArrayList<>(request.getIds());

        if (!request.getIds().isEmpty()) {
            model.addAttribute("ids", ids);
        } else if (request.getInputSource() !=null && !request.getMappingTarget().isEmpty()) {

            String source = request.getInputSource();
            Set<String> target = request.getMappingTarget();
            model.addAttribute("inputSource", source);
            model.addAttribute("mappingTarget", target);
        }

        return "search";
    }

}
