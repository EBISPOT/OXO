package uk.ac.ebi.spot.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.model.Mapping;

/**
 * @author Simon Jupp
 * @date 09/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Component
public class MappingAssembler  implements ResourceAssembler<Mapping, Resource<Mapping>> {

    @Autowired
    EntityLinks entityLinks;

    @Override
    public Resource<Mapping> toResource(Mapping mapping) {
        Resource<Mapping> resource = new Resource<Mapping>(mapping);
        Long id = mapping.getMappingId();
        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(MappingController.class).getMapping(id.toString()));

        final ControllerLinkBuilder fromLink = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(TermController.class).getTerm(mapping.getFromTerm().getCurie()));
        final ControllerLinkBuilder linkTo = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(TermController.class).getTerm(mapping.getToTerm().getCurie()));

        resource.add(lb.withSelfRel());
        resource.add(fromLink.withRel("fromTerm"));
        resource.add(linkTo.withRel("toTerm"));

        return resource;
    }
}
