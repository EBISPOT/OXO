package uk.ac.ebi.spot.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.model.Term;

/**
 * @author Simon Jupp
 * @date 08/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Component
public class TermAssembler implements ResourceAssembler<Term, Resource<Term>> {

    @Autowired
    EntityLinks entityLinks;

    @Override
    public Resource<Term> toResource(Term term) {
        Resource<Term> resource = new Resource<Term>(term);
        String id = term.getCurie();
        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(TermController.class).getTerm(id));

        final ControllerLinkBuilder ml = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(MappingController.class).mappings(null, null, term.getCurie(), null));

        final ControllerLinkBuilder dl = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(DatasourceController.class).getDatasource(term.getDatasource().getPrefix()));


        resource.add(lb.withSelfRel());
        resource.add(dl.withRel("datasource"));
        resource.add(new Link(
                ml.toUriComponentsBuilder().build().toUriString(),
                "mappings"
        ));

        return resource;
    }
}