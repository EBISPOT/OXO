package uk.ac.ebi.spot.controller.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.MappingSearchRequest;
import uk.ac.ebi.spot.service.DatasourceService;
import uk.ac.ebi.spot.service.MappingResponse;
import uk.ac.ebi.spot.service.MappingService;

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
    DatasourceService datasourceService;

    @ModelAttribute("all_datasources")
    public List<Datasource> getDatasources() {
        return datasourceService.getDatasurceWithMappings();
//        return Collections.emptyList();
    }

    @ModelAttribute("mapping_datasources")
    public List<Datasource> getMappingSources() {
        return datasourceService.getMappingSources();
//        return Collections.emptyList();
    }

    @GetMapping
    public String search(
            MappingSearchRequest request,
            Model model

    ) {

        Set<String> identfiers = request.getIds();
        if (identfiers.isEmpty()) {
            // handle error
            return "error";
        }


        Set<String> ids = new HashSet<>();
        for (String id : identfiers) {

            ids.addAll(new HashSet<>(Arrays.asList(id.split("\n"))));
        }

        ids = ids.stream().map(trim).collect(Collectors.toSet());
        // only get first 100 in UI
        int limit = ids.size();
        if (limit > 100) {
            limit = 99;
        }
        model.addAttribute("ids", new ArrayList<String>(ids).subList(0, limit));
//        model.addAttribute("results", results);
        model.addAttribute("request", request);


        return "search";
    }

    private static Function<String,String> trim = new Function<String,String>() {
        @Override public String apply(String s) {
            return s.replaceAll("(\\r|\\n)*$", "").trim();

        }
    };

}
