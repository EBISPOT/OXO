
package uk.ac.ebi.spot.controller.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.Model;

@ConfigurationProperties(prefix = "oxo.customisation")
@Configuration("customisationProperties")
public class CustomisationProperties {

    @Value("${oxo.customisation.debrand:false}")
    private boolean debrand;
    
    @Value("${oxo.customisation.logo:/img/OXO_logo_2017_colour_background.png")
    private String logo;

    @Value("${oxo.customisation.title:Ontology Xref Service}")
    private String title;

    @Value("${oxo.customisation.short-title:OxO}")
    private String shortTitle;

    @Value("${oxo.customisation.org:EMBL-EBI}")
    private String org;

    @Value("${oxo.customisation.olsUrl:https://www.ebi.ac.uk/ols}")
    private String olsUrl;

    public void setCustomisationModelAttributes(Model model) {
        model.addAttribute("debrand", debrand);
        model.addAttribute("logo", logo);
        model.addAttribute("title", title);
        model.addAttribute("shortTitle", shortTitle);
        model.addAttribute("org", org);
        model.addAttribute("olsUrl", olsUrl);
    }

    public boolean getDebrand() {
        return debrand;
    }
}
