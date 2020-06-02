package uk.ac.ebi.spot.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.spot.model.Datasource;
import uk.ac.ebi.spot.model.SourceType;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.service.TermService;

/**
 * @author Simon Jupp
 * @since 08/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Component
public class TermAssembler implements ResourceAssembler<Term, Resource<Term>> {

    @Autowired
    EntityLinks entityLinks;

    @Autowired
    private TermService termService;

    //private static String olsBase = "https://www.ebi.ac.uk/ols/api/terms?id=";
    private static String olsBase = "https://www.ebi.ac.uk/ols/api/terms?obo_id=";
    @Override
    public Resource<Term> toResource(Term term) {
        Resource<Term> resource = new Resource<Term>(term);
        String id = term.getCurie();
        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(TermController.class).getTerm(id));

        final ControllerLinkBuilder ml = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(MappingController.class).mappings(null, null, term.getCurie(), null));

        Datasource datasource = term.getDatasource();
        if (datasource == null) {
            datasource= termService.getTerm(term.getCurie()).getDatasource();
        }
        final ControllerLinkBuilder dl = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(DatasourceController.class).getDatasource(datasource.getPrefix()));


        resource.add(lb.withSelfRel());
        resource.add(dl.withRel("datasource"));
        resource.add(new Link(
                ml.toUriComponentsBuilder().build().toUriString(),
                "mappings"
        ));

        if (term.getDatasource().getSource().equals(SourceType.ONTOLOGY)) {
            resource.add(new Link(
                    olsBase+id,
                    "ols"
            ));
        }

        return resource;
    }
}