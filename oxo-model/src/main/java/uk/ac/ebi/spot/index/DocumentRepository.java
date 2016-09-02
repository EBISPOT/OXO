package uk.ac.ebi.spot.index;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.solr.repository.SolrCrudRepository;

/**
 * @author Simon Jupp
 * @date 04/08/2016
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@RepositoryRestResource(exported = false)
public interface DocumentRepository extends SolrCrudRepository<Document, String> {

    Document findOneByIdentifier(String id);

}
