package uk.ac.ebi.spot.controller.ui;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.Mapping;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.service.DatasourceService;
import uk.ac.ebi.spot.service.MappingService;
import uk.ac.ebi.spot.service.TermService;
import uk.ac.ebi.spot.util.MappingDistance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Simon Jupp
 * @date 31/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Controller
@RequestMapping("datasources")
public class DatasourceControllerUI {

    @Autowired
    DatasourceService datasourceService;

    @Autowired
    MappingService mappingService;

    @Autowired
    TermService termService;

    @RequestMapping(path = "/{prefix}", produces = {MediaType.TEXT_HTML_VALUE}, method = RequestMethod.GET)
    public String getDatasource (@PathVariable("prefix") String prefix, Model model, Pageable pageable) {

        Datasource datasource  = datasourceService.getDatasource(prefix);
        if (datasource == null) {
            model.addAttribute("error", "Datasource not found");

        }  else {


//            termService.getTermsBySource(datasource.getPrefix(), new PageRequest(0, 100000));
//
//            List<String> ids =mappingService.getMappingBySource(datasource.getPrefix())
//                    .parallelStream().map(Mapping::getFromTerm).collect(Collectors.toSet())
//                    .parallelStream().map(Term::getCurie).collect(Collectors.toList());

//            model.addAttribute("ids",ids);

//            model.addAttribute("mappingCount",mappingService.getMappedTargetCounts(prefix, 3));
            model.addAttribute("datasource",datasource);
            model.addAttribute("distance", MappingDistance.DEFAULT_MAPPING_DISTANCE);
        }



        return "datasource";


    }

}
