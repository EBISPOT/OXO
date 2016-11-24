package uk.ac.ebi.spot.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.model.Datasource;

/**
 * @author Simon Jupp
 * @date 09/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Component
public class DatasourceAssembler implements ResourceAssembler<Datasource, Resource<Datasource>> {

    @Autowired
        EntityLinks entityLinks;

    @Override
    public Resource<Datasource> toResource(Datasource datasource) {
        Resource<Datasource> resource = new Resource<Datasource>(datasource);
        String id = datasource.getPrefix();
        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(DatasourceController.class).getDatasource(id));

        final ControllerLinkBuilder ml = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(TermController.class).terms(id, null, null));

        resource.add(lb.withSelfRel());
        resource.add(new Link(
                ml.toUriComponentsBuilder().build().toUriString(),
                "terms"
        ));
        return resource;
    }
}
