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
        int totalTerms = 0;

        if (!request.getIds().isEmpty()) {
            model.addAttribute("ids", ids);
            totalTerms = ids.size();

            int size =  pageable.getOffset() + pageable.getPageSize() > ids.size() ? ids.size() : pageable.getOffset() + pageable.getPageSize();
            ids = ids.subList(pageable.getOffset(), size);

        } else if (!request.getInputSource().isEmpty()) {
            for (String prefix : request.getInputSource()) {
                ids.addAll(termService.getTermsBySource(prefix, pageable).getContent().stream().map(Term::getCurie).collect(Collectors.toList()));
                totalTerms += termService.getTermCountBySource(prefix);
            }

        } else if (!request.getMappingSource().isEmpty()) {
            for (String prefix : request.getMappingSource()) {
                ids.addAll
                        (mappingService.getMappingBySource(prefix, pageable).getContent()
                        .parallelStream().map(Mapping::getFromTerm).collect(Collectors.toList())
                        .parallelStream().map(Term::getCurie).collect(Collectors.toList()));
                totalTerms += mappingService.getMappingsCountBySource(prefix);
            }
        }

        model.addAttribute("totalterms", totalTerms);
        model.addAttribute("pageable", pageable);
        model.addAttribute("pagedIds", ids);
        model.addAttribute("request", request);
        return "search";
    }

}
