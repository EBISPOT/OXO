package uk.ac.ebi.spot.controller.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Simon Jupp
 * @date 16/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Controller
@RequestMapping("")
public class IndexController {

    @RequestMapping(path = "")
    public String home() {
        return "index";
    }

}
