package uk.ac.ebi.spot.controller.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ebi.spot.model.MappingSearchRequest;
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

    @PostMapping
    public String search(
            MappingSearchRequest request,
            Model model

    ) {

        String identfiers = request.getIdentifiers();
        if (identfiers == null) {
            // handle error
            return "error";
        }

        Set<String> ids = new HashSet<>(Arrays.asList(identfiers.split("\n")));

        ids = ids.stream().map(trim).collect(Collectors.toSet());
//        LinkedHashMap<String, List<MappingResponse>> results = mappingService.getMappingsSearch(ids, request.getDistance(), Collections.emptySet(), Collections.emptySet());

        model.addAttribute("ids", ids);
//        model.addAttribute("results", results);
        model.addAttribute("request", request);


        return "search";
    }

    private static Function<String,String> trim = new Function<String,String>() {
        @Override public String apply(String s) {
            return s.replaceAll("(\\r|\\n)$", "").trim();

        }
    };

}
