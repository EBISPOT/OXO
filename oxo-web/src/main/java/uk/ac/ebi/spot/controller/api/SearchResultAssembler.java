package uk.ac.ebi.spot.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.model.Term;
import uk.ac.ebi.spot.service.SearchResult;

/**
 * @author Simon Jupp
 * @date 05/09/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Component
public class SearchResultAssembler implements ResourceAssembler<SearchResult, Resource<SearchResult>> {

    @Autowired
    EntityLinks entityLinks;

    @Override
    public Resource<SearchResult> toResource(SearchResult searchResult) {
        Resource<SearchResult> resource = new Resource<SearchResult>(searchResult);
        String id = searchResult.getCurie();
        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(TermController.class).getTerm(id));

        final ControllerLinkBuilder ml = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(MappingController.class).mappings(null, null, searchResult.getCurie(), null));


        resource.add(lb.withSelfRel());
        resource.add(new Link(
                ml.toUriComponentsBuilder().build().toUriString(),
                "mappings"
        ));


        return resource;
    }

}


